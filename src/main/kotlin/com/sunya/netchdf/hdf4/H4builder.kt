package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.util.Indent
import mu.KotlinLogging
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class H4builder(val raf : OpenFile, val valueCharset : Charset, val strict : Boolean) {
    var rootBuilder: Group.Builder = Group.Builder("")
    val metadata = mutableListOf<Attribute>()
    var structMetadata : String? = null

    private val alltags : List<Tag>
    private val dims = mutableListOf<Variable.Builder>()
    private val completedObjects = mutableSetOf<Int>()

    internal val tagidMap = mutableMapOf<Int, Tag>()
    private val refnoMap = mutableMapOf<Int, Vinfo>()
    private var imageCount = 0

    init {
        // header information is in big endian byte order
        val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)

        // this positions the file after the header
        if (!isValidFile(raf, state)) {
            throw RuntimeException("Not an HDF4 file ")
        }

        // read the DDH and DD records, and populate the tag list
        val mtags = mutableListOf<Tag>()
        var link = state.pos
        while (link > 0) {
            state.pos = link
            val ndd: Int = raf.readShort(state).toUShort().toInt() // number of DD blocks
            link = raf.readInt(state).toUInt().toLong() // point to the next DDH; link == 0 means no more
            var pos = state.pos
            for (i in 0 until ndd) {
                val tag: Tag = readTag(raf, state)
                pos += 12
                state.pos = pos // tag usually changed the file pointer
                if (tag.code > 1) mtags.add(tag) // ignore NONE, NULL
            }
        }
        alltags = mtags.sortedBy { it.refno }

        // now read the individual tags' data
        for (tag: Tag in alltags) {
            with(this) { tag.readTag(this) }
            val tagid = tagid(tag.refno, tag.code)
            tagidMap[tagid] = tag // track all tags in a map, key is the "tag id" = code, refno.
            if (debugTag1) println(if (debugTagDetail) tag.detail() else tag)
        }

        if (debugTag2) {
            val summ = mutableMapOf<Int, MutableList<Int>>()
            alltags.forEach {
                val tags = summ.getOrPut(it.code) { mutableListOf() }
                tags.add(it.refno)
            }
            println("alltags = ${alltags.size} unique = ${summ.size} ")
            summ.toSortedMap().forEach { (key, list) ->
                val tagEnum = TagEnum.byCode(key)
                println(" $tagEnum (${list.size} tags)")
                // if (list.size < 100) println("  $list")
            }
        }
    }

    fun make() {
        //val roots = constructGroupTree()
        //constructCdm2(roots)
        constructCdm()

        if (structMetadata != null) {
            ODLparser(rootBuilder, false).applyStructMetadata(structMetadata!!)
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    val groupMap = mutableMapOf<Int, TGroup>()  // refno, Tgroup
    // we need to replace these tags with a Vgroup that contains them, so we have the name (!)
    val replaceMap = mutableMapOf<Int, TagVGroup>() // tagid, TagVGroup
    // the name of the SD from the Vgroup, when available
    val sdNameMap = mutableMapOf<Int, String>() // refno, sd name

    inner class TGroup(val name: String, val className : String) {
        val nested = mutableListOf<TGroup>()
        val vgroups = mutableListOf<TagVGroup>()
        val ndgs = mutableListOf<TagDataGroup>()
        val vhs = mutableListOf<TagVH>()
        var placed = false
        var parent : TGroup? = null // needed?

        fun addFromVGroup(vgroup : TagVGroup) {
            repeat(vgroup.nelems) { objIdx ->
                val tagRef = vgroup.elem_ref[objIdx]
                val tagCode = vgroup.elem_tag[objIdx]
                val tagid = tagid(tagRef, tagCode)
                val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")
                // println("  addFromVGroup  ${tag}")
                when (tag.tagEnum()) {
                    TagEnum.VG -> vgroups.add(tag as TagVGroup)
                    TagEnum.NDG -> ndgs.add(tag as TagDataGroup)
                    TagEnum.VH -> vhs.add(tag as TagVH)
                    else -> {}
                }
            }
        }

        fun addNested(ngroup : TGroup) {
            nested.add(ngroup)
            ngroup.parent = this
            ngroup.placed = true
        }

        fun show(indent : Indent) : String {
            return buildString {
                append("$indent${name} placed=$placed\n")
                nested.forEach{ append(it.show(indent.incr()))}
            }
        }

        override fun toString(): String {
            return buildString {
                append(if (placed) "  " else " *")
                append("'${className}' '${name}'")
                append(" nestedGroups=${nested.map { it.name } }")
                append(" vgroups=${vgroups.map { it.refno } }")
                append(" ndgs=${ndgs.map { it.refno } }")
                append(" vhs=${vhs.map { it.refno } }")
            }
        }
    }

    private fun constructGroupTree() : TGroup {
        val root = TGroup("root", "root")
        groupMap[-1] = root

        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.VG) {
                constructTGroup(t as TagVGroup, root)
            }
        }

        println("replace map =")
        replaceMap.forEach {
            println("  '${tagidName(it.key)}' = Vgroup refno=${it.value.refno}")
        }

        println("sdName map =")
        sdNameMap.forEach {
            println("  refno='$it.key' = sdName=$it.value")
        }

        groupMap.forEach { (_, group) ->
            if (!group.placed && group.nested.isNotEmpty()) root.addNested(group)
        }

        println("group map =")
        groupMap.forEach {
            println("  ${"%3d".format(it.key)}=${it.value}")
        }

        return root
    }

    // looks like could just sort by ref
    private fun constructTGroup(vgroup: TagVGroup, parent : TGroup) {
        println(" vgroup '${vgroup.name}' ${vgroup.className} ref=${vgroup.refno}")
        // if (parent.name == "root") parent.vgroups.add(vgroup)

        // this is to replace these tags with a Vgroup that contains them, so we have the name (!)
        repeat(vgroup.nelems) { objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")
            println("    ${tag}")
            when (tag.tagEnum()) {
                TagEnum.NDG -> {
                    replaceMap[tagid] = vgroup
                    sdNameMap[tagRef] = vgroup.name
                }
                TagEnum.VH -> replaceMap[tagid] = vgroup
                else -> {}
            }
        }

        // this is for the nested vgroups that form nested CDM groups
        if (vgroup.isNestedGroup()) {
            val tnested = TGroup(vgroup.name, vgroup.className)
            tnested.addFromVGroup(vgroup)
            groupMap[vgroup.refno] = tnested

            repeat(vgroup.nelems) { objIdx ->
                val tagRef = vgroup.elem_ref[objIdx]
                val tagCode = vgroup.elem_tag[objIdx]
                val tagid = tagid(tagRef, tagCode)
                val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagidName(tagid)}")

                if (tag.tagEnum() == TagEnum.VG) {
                    val nngroup = tag as TagVGroup
                    val ngroup = groupMap[nngroup.refno]
                    if (ngroup != null) {
                        tnested.addNested(ngroup)
                    }
                }
            }
        }
    }

    private fun TagVGroup.isNestedGroup() : Boolean {
        // seriously fucked up
        return className.startsWith("SWATH") or className.startsWith("GRID") or className.startsWith("POINT")
    }

    //////////////////////////////////////////////////////////////////////////////////

    private fun constructCdm2(root : TGroup) {
        VgroupProcess(root, rootBuilder)
        SDiterate(rootBuilder)
        GRiterate(rootBuilder)
        VStructureIterate(rootBuilder)

        metadata.forEach { makeVariableFromStringAttribute(it, rootBuilder) }

        if (structMetadata != null) {
            ODLparser(rootBuilder, false).applyStructMetadata(structMetadata!!)
        }
    }

    private fun VgroupProcess(tgroup : TGroup, parent : Group.Builder) {
        if (debugConstruct) println("--VgroupProcess '${tgroup.name}'")

        tgroup.ndgs.forEach {
            val sdName = sdNameMap[it.refno]
            println(" ** name for sd ${it.refno} = $sdName")
            SDread(it, sdName, parent)
        }

        tgroup.vhs.forEach {
            val tagid = tagid(it.refno, TagEnum.VH.code)
            val t = tagidMap[tagid] ?: throw RuntimeException()
            val cgroup = replaceMap[tagid]
            VStructureRead(t as TagVH, cgroup?.name, rootBuilder)
        }

        tgroup.vgroups.forEach {
            if (it.className == "Var0.0") {
                val t = it.tags().find { it.tagEnum() == TagEnum.NDG }
                if (t != null) {
                    SDread(t as TagDataGroup, it.name, parent)
                }
            }
            if (it.className == "CDF0.0") {
                VgroupCDF(it, parent)
            }
        }

        tgroup.nested.forEach {
            val nestedCdmGroup = Group.Builder(it.name)
            parent.addGroup(nestedCdmGroup)
            if (debugConstruct) println("  nested vgroup '${it.name}'")
            VgroupProcess(it, nestedCdmGroup)
        }
    }


    fun TagVGroup.tags() : List<Tag> {
        val result = mutableListOf<Tag>()
        repeat(nelems) {
            val tag = tagidMap[tagid(elem_ref[it], elem_tag[it])] ?: throw RuntimeException()
            result.add(tag)
        }
        return result
    }

    private fun VgroupVar(vgroup: TagVGroup, parent: Group.Builder) {
        if (debugConstruct) println("  VgroupVar '${vgroup.name}'")

        val atts = mutableListOf<TagVH>()
        var vb: Variable.Builder? = null

        repeat(vgroup.nelems) { objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

            if (tage == TagEnum.NDG) {
                vb = SDread(tag as TagDataGroup, vgroup.name, parent)
            }
            if (tage == TagEnum.VH) {
                val tagVH = tag as TagVH
                if (tagVH.className.startsWith("Att")) {
                    atts.add(tagVH)
                }
            }
        }

        if (vb != null) {
            atts.forEach {
                val att = VStructureReadAttribute(it)
                if (att != null) vb!!.attributes.add(att)
            }
        }
    }

    // These are coordinate variables, I think. Always has an associated VS for the data.
    private fun VgroupDim(vgroup : TagVGroup, parent : Group.Builder) {
        repeat(vgroup.nelems) {objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

            if (tage == TagEnum.VH) {
                val vb = VStructureRead(tag as TagVH, vgroup.name, parent)
                if (vb != null) {
                    if (debugConstruct) println("  adding dimension ${dims.size} name='${vb.name}'")
                    dims.add(vb)
                    // parent.addVariable(vb)
                }
            }
        }
    }

    private fun VgroupCDF(vgroup: TagVGroup, parent: Group.Builder) {
        repeat(vgroup.nelems) { objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagidName(tagid)})")

            if (tage == TagEnum.VH) {
                val tagVH = tag as TagVH
                if (tagVH.className == "Attr0.0") {
                    val attr = VStructureReadAttribute(tagVH)
                    if (attr != null) {
                        if (debugVGroupDetails) println("     read attribute ${attr.name}")
                        val moveup = attr.isString && attr.values.size == 1 && (attr.values[0] as String).length > 4000
                        if (EOS.isMetadata(attr.name) || moveup) {
                            metadata.add(attr)
                            if (attr.name == "StructMetadata.0") {
                                this.structMetadata = attr.values[0] as String
                            }
                        } else {
                            if (debugVGroupDetails) println("     add attribute ${attr}")
                            parent.addAttribute(attr)
                        }
                    }
                }
            }
        }
    }

    private fun SDiterate(rootBuilder : Group.Builder) {
        if (debugConstruct) println("--SDiterate")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.NDG) {
                SDread(t as TagDataGroup, null, rootBuilder)
            }
        }
    }

    private fun SDread(group: TagDataGroup, groupName : String?, parent : Group.Builder): Variable.Builder? {
        val tagid = tagid(group.refno, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugConstruct) println(" SDread skip ${group.refno}")
            return null
        }
        completedObjects.add(tagid)
        if (debugNG) println(" SDread ${group.refno} name=$groupName")

        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true

        // dimensions
        var dim: TagSDD? = null
        var data: TagData? = null
        val dimList = mutableListOf<String>()
        for (i in 0 until group.nelems) {
            if (debugNG)  print("     NDF has tag ${group.elem_ref[i]}/${TagEnum.byCode(group.elem_tag[i])} ")
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                println("NOT FOUND group=${group.refno}")
                continue
            } else {
                println()
            }

            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then its used, in order to avoid redundant variables
            if (tag.tagEnum() == TagEnum.SDD) dim = tag as TagSDD
            if (tag.tagEnum() == TagEnum.SD) data = tag as TagData
            if (tag.tagEnum() == TagEnum.SDL) {
                val text = tag as TagTextN
            }
        }
        if (data == null) {
            println("   **** NO data found")
            return null
        }
        if (dim == null) {
            println("   **** NO dimensions found")
            return null
        } else {
            // println("      TagSDD = $dim")
        }

        val nt: TagNT = tagidMap.get(tagid(dim.data_nt_ref, TagEnum.NT.code)) as TagNT? ?: throw IllegalStateException()
        val vb = Variable.Builder()
        vb.name = groupName ?: "Data-Set-" + group.refno
        vb.setDimensionsAnonymous(dim.shape)
        val dataType = H4type.getDataType(nt.numberType)
        vb.datatype = dataType
        vinfo.setVariable(vb)
        vinfo.setData(data, dataType.size)

        // fill value?
        val tagFV = tagidMap.get(tagid(dim.data_nt_ref, TagEnum.FV.code))
        if ((tagFV != null) and (tagFV is TagFV)) {
            vinfo.fillValue = (tagFV as TagFV).readFillValue(this, dataType)
        }

        for (i in 0 until group.nelems) {
            val tagid = tagid(group.elem_ref[i], group.elem_tag[i])
            val tag = tagidMap[tagid] // ?: throw RuntimeException("Dont have tag (${group.elem_ref[i]}, ${TagEnum.byCode(group.elem_tag[i])})")
            if (tag == null) {
                continue
            }
            if (tag.tagEnum() == TagEnum.SDL) {
                val labels: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.LONG_NAME, Datatype.STRING, labels.texts))
            }
            if (tag.tagEnum() == TagEnum.SDU) {
                val units: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.UNITS, Datatype.STRING, units.texts))
            }
            if (tag.tagEnum() == TagEnum.SDF) {
                val formats: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute("format", Datatype.STRING, formats.texts))
            }
            if (tag.tagEnum() == TagEnum.SDM) {
                val minmax: TagSDminmax = tag as TagSDminmax
                tag.isUsed = true
                vb.attributes.add(Attribute("valid_min", dataType, listOf(minmax.getMin(dataType))))
                vb.attributes.add(Attribute("valid_max", dataType, listOf(minmax.getMax(dataType))))
            }
        }

        // look for VH style attributes - dunno if they are actually used
        addVariableAttributes(group, vinfo)
        // if (debugConstruct) println(" makeVariable '${vb.name}' from Group $group SDdim= ${dim}")

        parent.addVariable(vb)
        return vb
    }

    private fun VStructureIterate(rootBuilder : Group.Builder) {
        if (debugConstruct) println("--VStructureIterate")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.VH) {
                VStructureRead(t as TagVH, null, rootBuilder)
            }
        }
    }

    private fun VStructureRead(tagVH: TagVH, groupName : String?, parent : Group.Builder) : Variable.Builder? {
        val tagid = tagid(tagVH.refno, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugConstruct) println(" VStructureRead skip ${tagVH.refno}")
            return null
        }
        completedObjects.add(tagid)

        if (tagVH.className.startsWith("Attr0.")) {
            val attr = VStructureReadAttribute(tagVH)
            if (attr != null && !EOS.isMetadata(attr.name)) {
                parent.addAttribute(attr)
            }
            return null
        } else  if (tagVH.className.startsWith("DimVal") || tagVH.className.startsWith("_HDF_CHK_TBL")) {
            return null
        }

        // Hcheader has
        //         if (g4.gb.variables.find { it.name == vsname } != null) return // LOOK why needed?

        val vinfo = Vinfo(tagVH.refno)
        vinfo.tags.add(tagVH)
        tagVH.vinfo = vinfo
        tagVH.isUsed = true
        // LOOK so we just guess that theres a VS with the same refno ? what else can we guess??
        val data: TagData? = tagidMap[tagid(tagVH.refno, TagEnum.VS.code)] as TagData?
        if (data == null) {
            log.error(("Cant find tag " + tagVH.refno + "/" + TagEnum.VS.code) + " for TagVH=" + tagVH.detail())
            return null
        }
        val dtagid = tagid(tagVH.refno, TagEnum.VS.code)
        completedObjects.add(dtagid)

        vinfo.tags.add(data)
        data.isUsed = true
        data.vinfo = vinfo
        if (tagVH.nfields < 1) throw IllegalStateException()

        val vb = Variable.Builder()
        vinfo.setVariable(vb)
        vb.name = tagVH.name
        if (tagVH.nfields.toInt() == 1) { // one field - dont make it into a structure
            vb.datatype = H4type.getDataType(tagVH.fld_type[0].toInt())
            val fnelems = tagVH.fld_nelems[0]
            if (tagVH.nelems > 1) {
                if (fnelems > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems, fnelems.toInt()))
                } else if (tagVH.fld_nelems[0] < 0) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems, tagVH.fld_isize[0]))
                } else {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems))
                }
            } else {
                if (fnelems > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(fnelems.toInt()))
                } else if (fnelems < 0) { // LOOK what is this case ??
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.fld_isize.get(0)))
                } else {
                    vb.dimensions.clear()
                }
            }
            vinfo.setData(data, vb.datatype!!.size)
        } else {
            if (tagVH.nelems > 1) {
                vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems))
            } else {
                vb.dimensions.clear()
            }
            val members = tagVH.readStructureMembers()
            val typedef = CompoundTypedef(tagVH.name, members)
            vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
            vinfo.setData(data, tagVH.ivsize)
            rootBuilder.typedefs.add(typedef)
        }
        if (debugConstruct) {
            println("added variable ${vb.name} from VH $tagVH")
        }

        parent.addVariable(vb)
        return vb
    }

    private fun VStructureReadAttribute(vh: TagVH): Attribute? {
        val data: Tag = tagidMap.get(tagid(vh.refno, TagEnum.VS.code)) ?: throw IllegalStateException()
        if (vh.name == "end_latlon") {
            println()
        }

        // assume always only one field.
        if (vh.nfields.toInt() != 1) throw IllegalStateException()
        val name: String = vh.name
        val type = vh.fld_type[0].toInt()
        val size: Int = vh.fld_isize[0]
        val nelems: Int = vh.nelems * vh.fld_nelems[0]
        vh.isUsed = true
        data.isUsed = true
        val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
        val att = when (type) {
            3, 4 -> {
                val vals = mutableListOf<String>()
                repeat (nelems) { vals.add(raf.readString(state, size, valueCharset)) }
                Attribute(name, Datatype.STRING, vals)
            }
            5 -> {
                val vals = mutableListOf<Float>()
                repeat (nelems) { vals.add(raf.readFloat(state)) }
                Attribute(name, Datatype.FLOAT, vals)
            }
            6 ->  {
                val vals = mutableListOf<Double>()
                repeat (nelems) { vals.add(raf.readDouble(state)) }
                Attribute(name, Datatype.DOUBLE, vals)
            }
            20 -> {
                val vals = mutableListOf<Byte>()
                repeat (nelems) { vals.add(raf.readByte(state)) }
                Attribute(name, Datatype.BYTE, vals)
            }
            21 -> {
                val vals = mutableListOf<UByte>()
                repeat (nelems) { vals.add(raf.readByte(state).toUByte()) }
                Attribute(name, Datatype.UBYTE, vals)
            }
            22 -> {
                val vals = mutableListOf<Short>()
                repeat (nelems) { vals.add(raf.readShort(state)) }
                Attribute(name, Datatype.SHORT, vals)
            }
            23 -> {
                val vals = mutableListOf<UShort>()
                repeat (nelems) { vals.add(raf.readShort(state).toUShort()) }
                Attribute(name, Datatype.USHORT, vals)
            }
            24 -> {
                val vals = mutableListOf<Int>()
                repeat (nelems) { vals.add(raf.readInt(state)) }
                Attribute(name, Datatype.INT, vals)
            }
            25 -> {
                val vals = mutableListOf<UInt>()
                repeat (nelems) { vals.add(raf.readInt(state).toUInt()) }
                Attribute(name, Datatype.UINT, vals)
            }
            26 -> {
                val vals = mutableListOf<Long>()
                repeat (nelems) { vals.add(raf.readLong(state)) }
                Attribute(name, Datatype.LONG, vals)
            }
            27 -> {
                val vals = mutableListOf<ULong>()
                repeat (nelems) { vals.add(raf.readLong(state).toULong()) }
                Attribute(name, Datatype.ULONG, vals)
            }
            else -> null
        }
        if (debugAtt) println("      att = '${att?.name}' (class=${vh.className})")
        return att
    }

    private fun GRiterate(rootBuilder : Group.Builder) {
        if (debugConstruct) println("--GRiterate")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.RIG) {
                GRVariable(t as TagDataGroup, rootBuilder)
            }
        }
    }

    private fun GRVariable(group: TagDataGroup, parent : Group.Builder): Variable.Builder? {
        var dimTag: TagRIDimension? = null
        val ntag: TagNT
        var data: Tag? = null
        val vinfo = Vinfo(group.refno)
        group.isUsed = true

        // use the list of elements in the group to find the other tags
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.warn("Image Group ${group.tagRefAndCode()} has missing tag=${group.elem_ref[i]}/${group.elem_tag[i]}")
                return null
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then used, to avoid redundant variables
            if (tag.code == 300) dimTag = tag as TagRIDimension
            if (tag.code == 302) data = tag
        }
        if (dimTag == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing dimension tag")
            return null
        }
        if (data == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing data tag")
            return null
        }

        // get the NT tag, referred to from the dimension tag
        val tag: Tag? = tagidMap[tagid(dimTag.nt_ref, TagEnum.NT.code)]
        if (tag == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing NT tag")
            return null
        }
        ntag = tag as TagNT
        if (debugConstruct) println("construct image " + group.refno)
        vinfo.start = data.offset
        vinfo.tags.add(group)
        vinfo.tags.add(dimTag)
        vinfo.tags.add(data)
        vinfo.tags.add(ntag)

        val vb = Variable.Builder()
        vb.name = "Raster_Image_#" + imageCount
        vb.datatype = H4type.getDataType(ntag.numberType)

        // assume dimensions are not shared
        vb.dimensions.add(makeDimensionUnshared("ydim", dimTag.ydim))
        vb.dimensions.add(makeDimensionUnshared("xdim", dimTag.xdim))

        vinfo.setVariable(vb)
        imageCount++

        parent.addVariable(vb)
        if (debugConstruct) println("GRVariable ${vb.name}")

        return vb
    }

    private fun makeVariableFromStringAttribute(att : Attribute, parent : Group.Builder) {
        require(att.isString)
        val svalue = att.values[0] as String
        val vb = Variable.Builder()
        vb.name = att.name
        vb.datatype = Datatype.STRING
        vb.spObject = Vinfo(-1).setSValue(svalue)
        parent.addVariable(vb)
    }

//////////////////////////////////////////////////////////////////////////////////////////////////
        private fun constructCdm() {
        val vars = mutableListOf<Variable.Builder>()

        // pass 1 : Vgroups with special classes
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.RIG) { // raster image group
                val v: Variable.Builder? = makeRasterImage(t as TagDataGroup)
                if (v != null) vars.add(v)
            } else if (t.tagEnum() == TagEnum.VG) {
                val vgroup: TagVGroup = t as TagVGroup
                if (vgroup.className.startsWith("Dim") || vgroup.className.startsWith("UDim")) {
                    makeDimension(rootBuilder, vgroup) // maybe safe to put them in the root, EOS will move

                } else if (vgroup.className.startsWith("Var")) {
                    val v = makeVariableFromVGroup(vgroup)
                    if (v != null) vars.add(v)

                } else if (vgroup.className.startsWith("CDF0.0")) {
                    addGlobalAttributes(vgroup)
                }
            }
        }

        // pass 2 - VHeaders, NDG (aka SD)
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.tagEnum() == TagEnum.VH) { // VHeader
                val tagVH: TagVH = t as TagVH
                if (tagVH.className.startsWith("Data")) {
                    val v = makeVariableVS(tagVH)
                    if (v != null) vars.add(v)
                }
            } else if (t.tagEnum() == TagEnum.NDG) { // numeric data group
                val v = makeVariableFromNumericGroup(t as TagDataGroup)
                vars.add(v)
            }
        }

        // pass 3 - misc not claimed yet
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.tagEnum() == TagEnum.VH) { // VHeader
                val vh: TagVH = t as TagVH
                if (!vh.className.startsWith("Att") && !vh.className.startsWith("DimVal") && !vh.className.startsWith("_HDF_CHK_TBL")) {
                    val v = makeVariableVS(vh)
                    if (v != null) vars.add(v)
                }
            }
        }

        // pass 4 - Groups
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.tagEnum() == TagEnum.VG) { // VGroup
                val vgroup: TagVGroup = t as TagVGroup
                makeGroup(vgroup, rootBuilder)
            }
        }

        // not already assigned to a group : put in root group.
        for (v: Variable.Builder in vars) {
            val vinfo: Vinfo = v.spObject as Vinfo
            if (vinfo.group == null && rootBuilder.variables.find { it.name == v.name } == null) {
                rootBuilder.addVariable(v)
                vinfo.group = rootBuilder
            }
        }

        // annotations become attributes
        for (t: Tag in alltags) {
            if (t is TagAnnotate) {
                val vinfo: Vinfo? = refnoMap[t.obj_refno]
                vinfo?.vb?.attributes?.add(
                    Attribute(
                        if (t.tagEnum() == TagEnum.DIA) "description" else CDM.LONG_NAME,
                        t.text
                    )
                )
                t.isUsed = true
            }
        }

        /* misc global attributes
        // root.addAttribute(Attribute("_History", "Direct read of HDF4 file through Netchdf library"))
        for (t: Tag in alltags) {
            if (t.code == 30) {
                fileVersion = (t as TagVersion).value()
                if (!strict) rootBuilder.addAttribute(Attribute("HDF4_Version", fileVersion))
                t.isUsed = true
            } else if (t.code == 100) {
                rootBuilder.addAttribute(
                    Attribute(
                        "Title-" + t.refno,
                        (t as TagText).text
                    )
                )
                t.isUsed = true
            } else if (t.code == 101) {
                rootBuilder.addAttribute(
                    Attribute(
                        "Description-" + t.refno,
                        (t as TagText).text
                    )
                )
                t.isUsed = true
            }
        }
         */
    }

    private fun adjustDimensions() {
        val dimUsedMap = mutableMapOf<Dimension, MutableList<Variable.Builder>>()
        makeDimensionMap(rootBuilder, dimUsedMap)

        // remove unused dimensions from root group
        for (dim: Dimension in rootBuilder.dimensions) {
            if (!dimUsedMap.contains(dim)) {
                rootBuilder.dimensions.remove(dim)
            }
        }

        // push used dimensions to the lowest group that contains all variables
        for ((dim, vlist) in dimUsedMap) {
            var lowest: Group.Builder? = null
            for (v in vlist) {
                if (v.spObject == null) {
                    log.warn("adjustDimensions ${v.name} missing vinfo (new)")
                    continue
                }
                val vinfo = v.spObject as Vinfo
                val gb: Group.Builder = vinfo.group!!
                lowest = if (lowest == null) gb else lowest.commonParent(gb)
            }
            if (lowest != null) {
                rootBuilder.removeDimensionFromAllGroups(dim)
                lowest.addDimensionIfNotExists(dim)
            }
        }
    }

    fun Group.Builder.removeDimensionFromAllGroups(removeDim: Dimension) {
        this.dimensions.remove(removeDim)
        this.groups.forEach { it.removeDimensionFromAllGroups(removeDim) }
    }

    /** Make a multimap of Dimensions and all the variables that reference them, in this group and its nested groups.  */
    private fun makeDimensionMap(parent: Group.Builder, dimUsedMap: MutableMap<Dimension, MutableList<Variable.Builder>>) {
        for (v: Variable.Builder in parent.variables) {
            for (d: Dimension in getDimensionsFor(parent, v)) {
                if (d.isShared) {
                    val list = dimUsedMap.getOrPut(d) { mutableListOf() }
                    list.add(v)
                }
            }
        }
        parent.groups.forEach { makeDimensionMap(it, dimUsedMap) }
    }

    private fun getDimensionsFor(gb: Group.Builder, vb: Variable.Builder): List<Dimension> {
        val dims = mutableListOf<Dimension>()
        for (dim: Dimension in vb.dimensions) {
            if (dim.isShared) {
                val sharedDim = gb.findDimension(dim.name) ?:
                                throw IllegalStateException("Shared Dimension $dim does not exist in a parent group")
                dims.add(sharedDim)
            } else {
                dims.add(dim)
            }
        }
        return dims
    }

    private fun makeDimension(gb : Group.Builder, vtags: TagVGroup) {
        if (vtags.name.startsWith("fakeDim")) {
            return
        }
        val dims = mutableListOf<TagVH>()
        var data: Tag? = null
        for (i in 0 until vtags.nelems) {
            val tag: Tag = tagidMap[tagid(vtags.elem_ref.get(i), vtags.elem_tag.get(i))] ?: throw IllegalStateException()
            if (tag.code == 1962) dims.add(tag as TagVH)
            if (tag.code == 1963) data = tag
        }

        // why not add all the dimensions?
        var length = 0
        if (data != null) {
            val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
            length = raf.readInt(state)
            data.isUsed = true
        } else {
            require (!dims.isEmpty())
            for (vh: TagVH in dims) {
                vh.isUsed = true
                data = tagidMap[tagid(vh.refno, TagEnum.VS.code)]
                if (null != data) {
                    data.isUsed = true
                    val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
                    val length2: Int = raf.readInt(state)
                    if (debugConstruct) println("   makeDimension length=" + length2 + " for TagVGroup= " + vtags + " using data " + data.refno)
                    if (length2 > 0) {
                        length = length2
                        break
                    }
                }
            }
        }
        if (data == null) {
            log.error("**no data for dimension TagVGroup= $vtags")
            return
        }
        if (length <= 0) {
            log.warn("**dimension length=" + length + " for TagVGroup= " + vtags + " using data " + data.refno)
        }

        // remove the eos long name bologna
        val dim = Dimension(vtags.name, length)
        gb.addDimension(dim)
    }

    private fun addGlobalAttributes(group: TagVGroup) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))] ?: throw IllegalStateException()
            if (tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    if (vh.nfields == 1.toShort() && H4type.getDataType(vh.fld_type[0].toInt()) === Datatype.CHAR &&
                            (vh.fld_isize[0] > 4000 || EOS.isMetadata(vh.name))
                    ) {
                        val vb = makeVariableVS(vh)
                        if (vb != null) {
                            vb.datatype = Datatype.STRING
                            vb.dimensions.clear()
                            rootBuilder.addVariable(vb) // // large EOS metadata - make into variable in root group
                        }
                    } else {
                        val att: Attribute? = VStructureReadAttribute(vh)
                        if (null != att) {
                            rootBuilder.addAttribute(att)
                        } // make into attribute in root group
                    }
                    if (vh.name == "StructMetadata.0") {
                        val att: Attribute? = VStructureReadAttribute(vh)
                        if (att != null) {
                            this.structMetadata = att.values[0] as String
                        }
                    }
                }
            }
        }
        group.isUsed = true
    }

    private fun makeGroup(tagGroup: TagVGroup, parent: Group.Builder): Group.Builder? {
        if (tagGroup.nelems < 1) return null
        val group: Group.Builder = Group.Builder(tagGroup.name)
        parent.addGroup(group)
        tagGroup.isUsed = true
        tagGroup.group = group
        
        for (i in 0 until tagGroup.nelems) {
            val tag: Tag? = tagidMap[tagid(tagGroup.elem_ref.get(i), tagGroup.elem_tag.get(i))]
            if (tag == null) {
                log.error(
                    "Reference tag missing= " + tagGroup.elem_ref.get(i) + "/" + tagGroup.elem_tag.get(i) + " for group "
                            + tagGroup.refno
                )
                continue
            }
            
            if (tag.code == 720) { // NG - prob var
                if (tag.vinfo != null) {
                    val v  = tag.vinfo!!.vb
                    if (v != null) addVariableToGroup(group, v, tag) else log.error("Missing variable " + tag.refno)
                }
            }
            if (tag.code == 1962) { // Vheader - may be an att or a var
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val att: Attribute? = VStructureReadAttribute(vh)
                    if (null != att) group.addAttribute(att)
                } else if (tag.vinfo != null) {
                    val v = tag.vinfo!!.vb!!
                    addVariableToGroup(group, v, tag)
                }
            }

            if (tag.code == 1965) { // VGroup - prob a Group
                val vg: TagVGroup = tag as TagVGroup
                if ((vg.group != null)) { // && (vg.group.parent === root)) {
                    addGroupToGroup(group, vg.group!!, vg)
                } else {
                    // makeGroup adds the nested group.
                    makeGroup(vg, group)
                }
            }
        }
        if (debugConstruct) {
            println("added group ${group.name} from VG ${tagGroup.refno}")
        }
        return group
    }

    private fun addVariableToGroup(
        parent: Group.Builder,
        vb: Variable.Builder,
        tag: Tag
    ) {
        val varExisting = parent.variables.find { it.name == vb.name }
        if (varExisting != null) varExisting.name += tag.refno // disambiguate
        parent.addVariable(vb)
        tag.vinfo!!.group = parent
    }

    private fun addGroupToGroup(parent: Group.Builder, gb: Group.Builder, tag: Tag) {
        // may have to reparent the group
        rootBuilder.removeGroupIfExists(gb.name)

        val groupExisting = parent.groups.find { it.name == gb.name }
        if (groupExisting != null) groupExisting.name += tag.refno // disambiguate
        parent.addGroup(gb)
    }

    private fun makeRasterImage(group: TagDataGroup): Variable.Builder? {
        var dimTag: TagRIDimension? = null
        val ntag: TagNT
        var data: Tag? = null
        val vinfo = Vinfo(group.refno)
        group.isUsed = true

        // use the list of elements in the group to find the other tags
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.warn("Image Group ${group.tagRefAndCode()} has missing tag=${group.elem_ref[i]}/${group.elem_tag[i]}")
                return null
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then used, to avoid redundant variables
            if (tag.code == 300) dimTag = tag as TagRIDimension
            if (tag.code == 302) data = tag
        }
        if (dimTag == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing dimension tag")
            return null
        }
        if (data == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing data tag")
            return null
        }

        // get the NT tag, referred to from the dimension tag
        val tag: Tag? = tagidMap[tagid(dimTag.nt_ref, TagEnum.NT.code)]
        if (tag == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing NT tag")
            return null
        }
        ntag = tag as TagNT
        if (debugConstruct) println("construct image " + group.refno)
        vinfo.start = data.offset
        vinfo.tags.add(group)
        vinfo.tags.add(dimTag)
        vinfo.tags.add(data)
        vinfo.tags.add(ntag)

        val vb = Variable.Builder()
        vb.name = "Raster_Image_#" + imageCount
        vb.datatype = H4type.getDataType(ntag.numberType)

        // assume dimensions are not shared
        vb.dimensions.add(makeDimensionUnshared("ydim", dimTag.ydim))
        vb.dimensions.add(makeDimensionUnshared("xdim", dimTag.xdim))

        vinfo.setVariable(vb)
        imageCount++
        return vb
    }

    private fun makeDimensionUnshared(dimName: String, len: Int): Dimension {
        return Dimension(dimName, len, false ,false)
    }

    // A Struct with nelems = Struct(nelems) {
    //                          fld1, fld2, ..., fldn
    // fln = datatype fln
    private fun makeVariableVS(tagVH: TagVH): Variable.Builder? {
        if (tagVH.name.startsWith("fakeDim")) {
            println()
        }
        val vinfo = Vinfo(tagVH.refno)
        vinfo.tags.add(tagVH)
        tagVH.vinfo = vinfo
        tagVH.isUsed = true
        val data: TagData? = tagidMap[tagid(tagVH.refno, TagEnum.VS.code)] as TagData?
        if (data == null) {
            log.error(("Cant find tag " + tagVH.refno + "/" + TagEnum.VS.code) + " for TagVH=" + tagVH.detail())
            return null
        }
        vinfo.tags.add(data)
        data.isUsed = true
        data.vinfo = vinfo
        if (tagVH.nfields < 1) throw IllegalStateException()

        val vb = Variable.Builder()
        vinfo.setVariable(vb)
        vb.name = tagVH.name
        if (tagVH.nfields.toInt() == 1) { // one field - dont make it into a structure
            vb.datatype = H4type.getDataType(tagVH.fld_type[0].toInt())
            val fnelems = tagVH.fld_nelems[0]
            if (tagVH.nelems > 1) {
                if (fnelems > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems, fnelems.toInt()))
                } else if (tagVH.fld_nelems[0] < 0) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems, tagVH.fld_isize[0]))
                } else {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems))
                }
            } else {
                if (fnelems > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(fnelems.toInt()))
                } else if (fnelems < 0) { // LOOK what is this case ??
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.fld_isize.get(0)))
                } else {
                    vb.dimensions.clear()
                }
            }
            vinfo.setData(data, vb.datatype!!.size)
        } else {
            if (tagVH.nelems > 1) {
                vb.setDimensionsAnonymous(intArrayOf(tagVH.nelems))
            } else {
                vb.dimensions.clear()
            }
            val members = tagVH.readStructureMembers()
            val typedef = CompoundTypedef(tagVH.name, members)
            vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
            vinfo.setData(data, tagVH.ivsize)
            rootBuilder.typedefs.add(typedef)
        }
        if (debugConstruct) println("makeVariableVS ${vb.name} from VH $tagVH")
        return vb
    }

    fun makeVinfoForDimensionMapVariable(parent: Group.Builder, v: Variable.Builder) {
        val vinfo: Vinfo = makeVinfo(-1)
        vinfo.group = parent
        vinfo.setVariable(v)
    }

    fun readStructMetadata(structMetadataVar: Variable.Builder): String {
        val vinfo = structMetadataVar.spObject as Vinfo
        return vinfo.read(this)
    }

    // member info
    internal class Minfo(val offset: Int)

    private fun makeVariableFromVGroup(group: TagVGroup): Variable.Builder? {
        if (group.name.startsWith("fakeDim")) {
            println()
        }
        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true
        var dim: TagSDD? = null
        var ntag: TagNT? = null
        var data: TagData? = null

        val dimList = mutableListOf<String>()
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.error("Reference tag missing= " + group.elem_ref.get(i) + "/" + group.elem_tag.get(i))
                continue
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Vgroup, then not needed, to avoid redundant variables
            if (tag.code == 106) ntag = tag as TagNT
            if (tag.code == 701) dim = tag as TagSDD
            if (tag.code == 702) data = tag as TagData
            if (tag.code == 1965) {
                val dimGroup: TagVGroup = tag as TagVGroup
                if (dimGroup.className.startsWith("Dim") || dimGroup.className.startsWith("UDim")) {
                    if (!dimGroup.name.startsWith("fakeDim")) {
                        dimList.add(dimGroup.name)
                    }
                }
            }
        }
        if (ntag == null) {
            log.error("ntype tag missing vgroup= " + group.refno)
            return null
        }
        if (dim == null) {
            log.error("dim tag missing vgroup= " + group.refno)
            return null
        }
        if (data == null) {
            log.warn("data tag missing vgroup= " + group.refno + " " + group.name)
            // return null;
        }

        val vb = Variable.Builder()
        vb.name = group.name
        vb.dimList = dimList
        vb.datatype = H4type.getDataType(ntag.numberType)
        vinfo.setVariable(vb)
        vinfo.setData(data, vb.datatype!!.size)

        // the 701 SDD tag overrides the VGroup dimensions
        if (dimList.isEmpty())
            vb.setDimensionsAnonymous(dim.shape)

        // look for attributes
        addVariableAttributes(group, vinfo)
        if (debugConstruct) {
            println("   added variable ${vb.name} from VG ${group.refno}")
            println("    SDdim= ${dim.detail()}")
            println("    VGdims= $dimList")
        }
        return vb
    }

    // aka SD
    private fun makeVariableFromNumericGroup(group: TagDataGroup): Variable.Builder {
        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true

        // dimensions
        var dim: TagSDD? = null
        var data: TagData? = null
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (debugNG) println("  NDF has tag $tag")
            if (tag == null) {
                log.error("Cant find tag " + group.elem_ref.get(i) + "/" + group.elem_tag.get(i) + " for group=" + group.refno)
                continue
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then its used, in order to avoid redundant variables
            if (tag.tagEnum() == TagEnum.SDD) dim = tag as TagSDD
            if (tag.tagEnum() == TagEnum.SD) data = tag as TagData
            if (tag.tagEnum() == TagEnum.SDL) {
                val text = tag as TagTextN
            }
        }
        if ((dim == null) || (data == null)) throw IllegalStateException()

        val nt: TagNT = tagidMap.get(tagid(dim.data_nt_ref, TagEnum.NT.code)) as TagNT? ?: throw IllegalStateException()
        val vb = Variable.Builder()
        vb.name = "Data-Set-" + group.refno
        vb.setDimensionsAnonymous(dim.shape)
        val dataType = H4type.getDataType(nt.numberType)
        vb.datatype = dataType
        vinfo.setVariable(vb)
        vinfo.setData(data, dataType.size)

        // fill value?
        val tagFV = tagidMap.get(tagid(dim.data_nt_ref, TagEnum.FV.code))
        if ((tagFV != null) and (tagFV is TagFV)) {
            vinfo.fillValue = (tagFV as TagFV).readFillValue(this, dataType)
        }

        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap.get(tagid(group.elem_ref.get(i), group.elem_tag.get(i))) ?: throw IllegalStateException()
            if (tag.tagEnum() == TagEnum.SDL) {
                val labels: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.LONG_NAME, Datatype.STRING, labels.texts))
            }
            if (tag.tagEnum() == TagEnum.SDU) {
                val units: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.UNITS, Datatype.STRING, units.texts))
            }
            if (tag.tagEnum() == TagEnum.SDF) {
                val formats: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.attributes.add(Attribute("format", Datatype.STRING, formats.texts))
            }
            if (tag.tagEnum() == TagEnum.SDM) {
                val minmax: TagSDminmax = tag as TagSDminmax
                tag.isUsed = true
                vb.attributes.add(Attribute("valid_min", dataType, listOf(minmax.getMin(dataType))))
                vb.attributes.add(Attribute("valid_max", dataType, listOf(minmax.getMax(dataType))))
            }
        }

        // look for VH style attributes - dunno if they are actually used
        addVariableAttributes(group, vinfo)
        if (debugConstruct) {
            println(("added variable " + vb.name) + " from Group " + group)
            println("  SDdim= " + dim.detail())
        }
        return vb
    }

    private fun addVariableAttributes(group: TagDataGroup, vinfo: Vinfo) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag = tagidMap.get(tagid(group.elem_ref.get(i), group.elem_tag.get(i)))
            if (tag != null && tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val att: Attribute? = VStructureReadAttribute(vh)
                    if (null != att) {
                        vinfo.vb!!.attributes.add(att)
                        if (att.name.equals(CDM.FILL_VALUE)) vinfo.setFillValue(att)
                    }
                }
            }
        }
    }

    private fun addVariableAttributes(group: TagVGroup, vinfo: Vinfo) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap.get(tagid(group.elem_ref.get(i), group.elem_tag.get(i))) ?: throw IllegalStateException()
            if (tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val att: Attribute? = VStructureReadAttribute(vh)
                    if (null != att) {
                        vinfo.vb!!.attributes.add(att)
                        if (att.name.equals(CDM.FILL_VALUE)) vinfo.setFillValue(att)
                    }
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    internal fun makeVinfo(refno: Int): Vinfo {
        val vinfo = Vinfo(refno)
        this.refnoMap[refno] = vinfo
        return vinfo
    }

    //////////////////////////////////////////////////////////////////////

    companion object {
        val log = KotlinLogging.logger("H4builder")
        private val H4HEAD = byteArrayOf(0x0e.toByte(), 0x03.toByte(), 0x13.toByte(), 0x01.toByte())
        private val H4HEAD_STRING = String(H4HEAD, StandardCharsets.UTF_8)
        private val maxHeaderPos: Long = 500000 // header's gotta be within this

        fun isValidFile(raf: OpenFile, state : OpenFileState): Boolean {
            val size: Long = raf.size

            // search forward for the header
            var startPos = 0L
            while ((startPos < (size - H4HEAD.size)) && (startPos < maxHeaderPos)) {
                state.pos = startPos
                val magic: String = raf.readString(state, H4HEAD.size)
                if ((magic == H4HEAD_STRING)) return true
                startPos = if ((startPos == 0L)) 512 else 2 * startPos
            }
            return false
        }

        private var debugTag1 = false // show tags after read(), before read2().
        private var debugTag2 = true // show tags after everything is done.
        private var debugTagDetail = false // when showing tags, show detail or not
        private var debugConstruct = true // show CDM objects as they are constructed
        private var debugAtt = true // show CDM attributes as they are constructed
        private var debugVGroupDetails = false
        private var debugVSdata = false
        private var debugChunkDetail = false // chunked data
        private var debugNG = true
        private var debugTracker = false // memory tracker
        private val warnings = false // log messages
        private var useHdfEos = true // allow to turn hdf eos processing off

        // this is a unique id for a message in a file
        fun tagid(refno: Int, code: Int): Int {
            val result = (code and 0x3FFF) shl 16
            val result2 = (refno and 0xffff)
            return result + result2
        }

        fun tagidName(tagid : Int): String {
            val code = (tagid shr 16) and 0x3FFF
            val refno = (tagid and 0xffff)
            return "ref=$refno code=${code}"
        }
    }
}