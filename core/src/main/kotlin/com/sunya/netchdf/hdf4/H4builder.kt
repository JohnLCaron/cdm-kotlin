package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.util.Indent
import com.sunya.netchdf.netcdf4.NUG
import mu.KotlinLogging
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class H4builder(val raf : OpenFile, val valueCharset : Charset) {
    var rootBuilder: Group.Builder = Group.Builder("")
    val metadata = mutableListOf<Attribute>()
    var structMetadata : String? = null

    private val alltags : List<Tag>
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
            if (debugTag) println(if (debugTagDetail) tag.detail() else tag)
        }

        if (debugTagSummary) {
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

    //////////////////////////////////////////////////////////////////////////////////
    val sdAliasMap = mutableMapOf<Int, TagVGroup>() // refno, sd name
    val vhAliasMap = mutableMapOf<Int, String>() // refno, sd name

    fun make() {
        VgroupIterate() { t : Tag -> VgroupFindAliases(t as TagVGroup, Indent(2)) }
        VgroupIterate() { t : Tag -> VgroupRead(t as TagVGroup, rootBuilder) }
        SDiterate(rootBuilder)
        GRiterate(rootBuilder)
        VStructureIterate(rootBuilder)

        metadata.forEach { makeVariableFromStringAttribute(it, rootBuilder) }

        if (structMetadata != null) {
            ODLparser(rootBuilder, true).applyStructMetadata(structMetadata!!)
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

    private fun VgroupIterate(lamda : (t : Tag) -> Unit) {
        if (debugConstruct) println("--VgroupIterate")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.VG) {
                lamda.invoke(t)
            }
        }
    }

    private fun VgroupFindAliases(vgroup: TagVGroup, indent:Indent) {
        if (debugVGroupDetails) println("$indent  Vgroup ${vgroup.refno} ${vgroup.className} ${vgroup.name} ")
        repeat(vgroup.nelems) {
            val tagRef = vgroup.elem_ref[it]
            val tagCode = vgroup.elem_tag[it]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            if (debugVGroupDetails) println("$indent    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)}")
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")
            if (tage == TagEnum.VG) {
                VgroupFindAliases(tag as TagVGroup, indent.incr())
            } else if (tage == TagEnum.NDG) {
                sdAliasMap[tagRef] = vgroup
            } else if (tage == TagEnum.VH) {
                vhAliasMap[tagRef] = vgroup.name
            }
        }
    }

    // read a VGroup (1965)
    private fun VgroupRead(tagVG : TagVGroup, parent : Group.Builder) {
        val tagid = tagid(tagVG.refno, tagVG.code)
        if (completedObjects.contains(tagid)) {
            if (debugConstruct) println("VgroupRead skip  ${tagVG.refno} '${tagVG.name}'")
            return
        }
        completedObjects.add(tagid)

        if (tagVG.nelems > 0) {
            if (isNestedGroup(tagVG.className)) {
                VgroupGroup(tagVG, parent)
            } else if (isDimClass(tagVG.className)) {
                VgroupDim(tagVG, parent)
            } else if (tagVG.className == "Var0.0") { // ??
                VgroupVar(tagVG, parent)
            } else if (tagVG.className == "CDF0.0") {
                // LOOK ignoring the contents of the VGroup, and looking at the attributes on the group
                VgroupCDF(tagVG, parent)
            }
        }

        /* attributes ??
        val nattrs = mfhdf_h.Vnattrs(vgroup_id)
        val aname_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(mfhdf_h.C_INT, 0)
        val nvalues_p = session.allocate(mfhdf_h.C_INT, 0)
        val size_p = session.allocate(mfhdf_h.C_INT, 0)
        repeat(nattrs) { idx ->
            checkErr("Vattrinfo", mfhdf_h.Vattrinfo(vgroup_id, idx, aname_p, datatype_p, nvalues_p, size_p))
            val aname = aname_p.getUtf8String(0)
            val datatype = datatype_p[mfhdf_h.C_INT, 0]
            val nvalues = nvalues_p[mfhdf_h.C_INT, 0]
            val size = size_p[mfhdf_h.C_INT, 0]
            if (com.sunya.netchdf.hdf4Clib.debugVGroupDetails) println("    readVGroupAttr '$aname' datatype='$datatype' nvalues=$nvalues size =$size")
        }

         */
    }

    private fun VgroupGroup(vgroup: TagVGroup, parent: Group.Builder) {
        if (debugConstruct) println("  VgroupGroup '${vgroup.name}' ref=${vgroup.refno}")
        val nested = Group.Builder(vgroup.name)
        parent.addGroup(nested)

        repeat(vgroup.nelems) { objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

            if (tage == TagEnum.VG) {
                val vtag = tag as TagVGroup
                if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)} '${vtag.className}' '${vtag.name}'")
                VgroupRead(vtag, nested)
            } else if (tage == TagEnum.NDG) {
                if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)}")
                val alias = sdAliasMap[tagRef]
                if (alias != null)
                    VgroupVar(alias, nested)
                else {
                    SDread(tag as TagDataGroup, null, nested)
                }
            } else if (tage == TagEnum.VH) {
                val vtag = tag as TagVH
                if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)} '${vtag.className}' '${vtag.name}'")
                VStructureRead(vtag, vhAliasMap[tagRef], nested, true)
            }
        }
    }

    private fun VgroupVar(vgroup: TagVGroup, parent: Group.Builder) {
        if (debugConstruct) println("  VgroupVar '${vgroup.name}' ref=${vgroup.refno}")

        val dims = mutableListOf<String>()
        val atts = mutableListOf<TagVH>()
        var vb: Variable.Builder? = null

        repeat(vgroup.nelems) { objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

            // assumes all the dims come before the NDG
            if (tage == TagEnum.VG) {
                val tagV = tag as TagVGroup
                if (isDimClass(tagV.className)) {
                    val dim = VgroupDim(tagV, parent)
                    if (dim != null && !dim.name.startsWith("fakeDim")) dims.add(dim.name) // length is wrong - haha
                }
            }

            if (tage == TagEnum.NDG) {
                vb = SDread(tag as TagDataGroup, vgroup.name, parent, dims)
            }

            if (tage == TagEnum.VH) {
                val tagVH = tag as TagVH
                if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)} ${tagVH.className} ${tagVH.name}")
                if (tagVH.className.startsWith("Att")) {
                    atts.add(tagVH)
                }
            }
        }

        if (vb != null) {
            atts.forEach {
                val att = VStructureReadAttribute(it)
                if (att != null) vb!!.addAttribute(att)
            }
        }
    }

    // These are coordinate variables, I think. Always has an associated VS for the data.
    // unfortunately, they are not always correct
    private fun VgroupDim(vgroup : TagVGroup, parent : Group.Builder) : Dimension? {
        if (debugConstruct) println("  VgroupDim '${vgroup.name}' ref=${vgroup.refno}")
        repeat(vgroup.nelems) {objIdx ->
            val tagRef = vgroup.elem_ref[objIdx]
            val tagCode = vgroup.elem_tag[objIdx]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)}")
            val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

            if (tage == TagEnum.VH) {
                val tagVH = tag as TagVH
                val length = tagVH.nelems * tagVH.fld_nelems[0]
                if (debugDims) println("     read dimension ${vgroup.name} length='${length}'")
                return Dimension(vgroup.name, length)
            }
        }
        return null
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

    private fun SDread(group: TagDataGroup, groupName : String?, parent : Group.Builder, dimNames : List<String>? = null): Variable.Builder? {
        val tagid = tagid(group.refno, group.code)
        if (completedObjects.contains(tagid)) {
            if (debugConstruct) println("SDread skip  ${group.refno} $groupName")
            return null
        }
        completedObjects.add(tagid)
        if (debugNG) println(" SDread ${group.refno} name=$groupName")

        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true

        // dimensions - serious amount of dysfunction here
        var dimSDD: TagSDD? = null
        var data: TagData? = null
        for (i in 0 until group.nelems) {
            val ntagid = tagid(group.elem_ref[i], group.elem_tag[i])
            if (debugNG)  print("     NDF has tag ${tagidName(ntagid)} ")
            val tag: Tag? = tagidMap[ntagid]
            if (tag == null) {
                if (group.elem_tag[i] != 721) println(" SDread NOT FOUND ${tagidName(ntagid)} in group=${group.refno}")
                continue
            }

            vinfo.tags.add(tag) // needed?
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then its used, in order to avoid redundant variables
            if (tag.tagEnum() == TagEnum.SDD) dimSDD = tag as TagSDD
            if (tag.tagEnum() == TagEnum.SD) data = tag as TagData
            if (tag.tagEnum() == TagEnum.SDL) { // just wondering wtf
                val text = tag as TagTextN
            }
        }
        if (dimSDD == null) {
            println("   **** NO dimensions found")
            return null
        }

        val vb = Variable.Builder()
        vb.name = groupName ?: "Data-Set-${group.refno}"

        // have to use the variables to figure out the dimensions. barf
        if (dimNames != null && !dimNames.isEmpty()) {
            require(dimSDD.shape.size == dimNames.size)
            repeat(dimSDD.shape.size) {
                val dim = Dimension(dimNames[it], dimSDD.shape[it])
                vb.addDimension(dim)
                parent.addDimensionIfNotExists(dim)
            }
        } else {
            vb.setDimensionsAnonymous(dimSDD.shape)
        }

        val nt: TagNT = tagidMap.get(tagid(dimSDD.data_nt_ref, TagEnum.NT.code)) as TagNT? ?: throw IllegalStateException()
        val dataType = H4type.getDataType(nt.numberType)
        vb.datatype = dataType
        vinfo.setVariable(vb)
        vinfo.setData(data, dataType.size)

        // fill value?
        val tagFV = tagidMap.get(tagid(dimSDD.data_nt_ref, TagEnum.FV.code))
        if ((tagFV != null) and (tagFV is TagFV)) {
            vinfo.fillValue = (tagFV as TagFV).readFillValue(this, dataType)
        }
        // data
        if (data == null) {
            vinfo.hasNoData = true
        }

        // look for interesting attributes
        for (i in 0 until group.nelems) {
            val tagid = tagid(group.elem_ref[i], group.elem_tag[i])
            val tag = tagidMap[tagid] // ?: throw RuntimeException("Dont have tag (${group.elem_ref[i]}, ${TagEnum.byCode(group.elem_tag[i])})")
            if (tag == null) {
                continue
            }
            if (tag.tagEnum() == TagEnum.SDL) {
                val labels: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.addAttribute(Attribute(NUG.LONG_NAME, Datatype.STRING, labels.texts))
            }
            if (tag.tagEnum() == TagEnum.SDU) {
                val units: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.addAttribute(Attribute(NUG.UNITS, Datatype.STRING, units.texts))
            }
            if (tag.tagEnum() == TagEnum.SDF) {
                val formats: TagTextN = tag as TagTextN
                tag.isUsed = true
                vb.addAttribute(Attribute("format", Datatype.STRING, formats.texts))
            }
            if (tag.tagEnum() == TagEnum.SDM) {
                val minmax: TagSDminmax = tag as TagSDminmax
                tag.isUsed = true
                vb.addAttribute(Attribute("valid_min", dataType, listOf(minmax.getMin(dataType))))
                vb.addAttribute(Attribute("valid_max", dataType, listOf(minmax.getMax(dataType))))
            }
        }

        // look for VH style attributes - dunno if they are actually used
        addVariableAttributes(group, vb, vinfo)
        // if (debugConstruct) println(" makeVariable '${vb.name}' from Group $group SDdim= ${dim}")

        parent.addVariable(vb)
        return vb
    }

    private fun VStructureIterate(rootBuilder : Group.Builder) {
        if (debugConstruct) println("--VStructureIterate")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.VH) {
                VStructureRead(t as TagVH, null, rootBuilder, false)
            }
        }
    }

    private fun VStructureRead(tagVH: TagVH, groupName : String?, parent : Group.Builder, addAttsToGroup : Boolean) : Variable.Builder? {
        val tagid = tagid(tagVH.refno, tagVH.code)
        if (completedObjects.contains(tagid)) {
            if (debugConstruct) println("VStructureRead skip  ${tagVH.refno} $groupName")
            return null
        }
        completedObjects.add(tagid)

        if (tagVH.className.startsWith("Attr0.")) {
            if (addAttsToGroup) {
                val attr = VStructureReadAttribute(tagVH)
                if (attr != null && !EOS.isMetadata(attr.name)) {
                    parent.addAttribute(attr)
                }
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
            rootBuilder.addTypedef(typedef)
        }
        if (debugConstruct) {
            println("added variable ${vb.name} from VH $tagVH")
        }

        parent.addVariable(vb)
        return vb
    }

    private fun VStructureReadAttribute(vh: TagVH): Attribute? {
        val data: Tag = tagidMap.get(tagid(vh.refno, TagEnum.VS.code)) ?: throw IllegalStateException()

        // assume always only one field.
        if (vh.nfields.toInt() != 1) throw IllegalStateException()
        val name: String = vh.name
        val type = vh.fld_type[0].toInt()
        val datatype = H4type.getDataType(type)
        val size: Int = vh.fld_isize[0]
        val fld_nelems = vh.fld_nelems[0]
        val nelems = if (datatype == Datatype.CHAR) vh.nelems else  vh.nelems * fld_nelems

        vh.isUsed = true
        data.isUsed = true

        val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
        val att = when (type) {
            3, 4 -> {
                val totalSize = nelems * size
                val vals = mutableListOf<String>()
                repeat (nelems) { vals.add(raf.readString(state, totalSize, valueCharset)) }
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
        var tagRasterImage: TagRasterImage? = null
        val vinfo = Vinfo(group.refno)
        group.isUsed = true

        // look through the other tags
        for (i in 0 until group.nelems) {
            val tagid = tagid(group.elem_ref[i], group.elem_tag[i])
            val tag: Tag? = tagidMap[tagid]
            if (tag == null) {
                log.warn("Image Group ${group.tagRefAndCode()} has missing tag=${tagidName(tagid)}")
                return null
            }
            if (debugGR) println("   GRVariable ${group.refno} has tag ${tagidName(tagid)} ")

            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then used, to avoid redundant variables
            if (tag.tagEnum() == TagEnum.ID) dimTag = tag as TagRIDimension
            if (tag.tagEnum() == TagEnum.RI) tagRasterImage = tag as TagRasterImage
            if (tag.tagEnum() == TagEnum.RIG) {
                val rig = tag as TagDataGroup
                for (i in 0 until rig.nelems) {
                    val ntagid = tagid(rig.elem_ref[i], rig.elem_tag[i])
                    if (debugGR) println("     GRVariable has nested tag ${tagidName(ntagid)} ")
                }

            }
        }
        if (dimTag == null) {
            log.warn("Image Group ${group.tagRefAndCode()} missing TagRIDimension")
            return null
        }
        if (tagRasterImage == null) {
            log.warn("Image Group ${group.tagRefAndCode()} missing TagRasterImage")
            return null
        }
        vinfo.tagDataRI = tagRasterImage

        // get the NT tag, referred to from the dimension tag
        val tag: Tag? = tagidMap[tagid(dimTag.nt_ref, TagEnum.NT.code)]
        if (tag == null) {
            log.warn("Image Group " + group.tagRefAndCode() + " missing NT tag")
            return null
        }
        ntag = tag as TagNT
        if (debugConstruct) println("construct image " + group.refno)

        val vb = Variable.Builder()
        vb.name = "Raster_Image_#" + imageCount
        val datatype = H4type.getDataType(ntag.numberType)
        vinfo.start = tagRasterImage.offset
        vb.datatype = datatype
        vinfo.elemSize = datatype.size

        // assume dimensions are not shared
        vb.dimensions.add(Dimension("ydim", dimTag.ydim, false ,false))
        vb.dimensions.add(Dimension("xdim", dimTag.xdim, false ,false))

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

    private fun addVariableAttributes(group: TagDataGroup, vb : Variable.Builder, vinfo: Vinfo) {
        // look for attributes
        repeat(group.nelems) {
            val tag = tagidMap.get(tagid(group.elem_ref[it], group.elem_tag[it]))
            if (tag != null && tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val att: Attribute? = VStructureReadAttribute(vh)
                    if (null != att) {
                        vb.addAttribute(att)
                        if (att.name.equals(NUG.FILL_VALUE)) vinfo.setFillValue(att)
                    }
                }
            }
        }
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

        private var debugTagSummary = false // show tags after everything is done.
        private var debugTag = false // show tags when reading in first time
        private var debugTagDetail = false // when showing tags, show detail or not

        private var debugVGroup = false
        private var debugVGroupDetails = false
        private var debugConstruct = false // show CDM objects as they are constructed
        private var debugAtt = false // show CDM attributes as they are constructed
        private var debugDims = false
        private var debugNG = false
        private var debugGR = false

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

        fun tagidNameR(tagid : Int): String {
            val refno = (tagid shr 16) and 0xFFFF
            val code = (tagid and 0x3fff)
            return "ref=$refno code=${code}"
        }
    }
}

fun isNestedGroup(className : String) : Boolean {
    // seriously fucked up
    return className.startsWith("SWATH") or className.startsWith("GRID") or className.startsWith("POINT")
}

fun isDimClass(className : String) : Boolean {
    return (className == "Dim0.0") or (className == "UDim0.0")
}