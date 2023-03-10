package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import mu.KotlinLogging
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class H4builder(val raf : OpenFile, val valueCharset : Charset, val strict : Boolean) {
    val rootBuilder: Group.Builder = Group.Builder("")
    var isEos = false
    var fileVersion = "N/A"
    val alltags = mutableListOf<Tag>()
    val tagidMap = mutableMapOf<Int, Tag>()
    private val refnoMap = mutableMapOf<Int, Vinfo>()

    init {
        // header information is in big endian byte order
        val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)

        // this positions the file after the header
        if (!isValidFile(raf, state)) {
            throw RuntimeException("Not an HDF4 file ")
        }

        // read the DDH and DD records, and populate the tag list
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
                if (tag.code > 1) alltags.add(tag) // ignore NONE, NULL
            }
        }

        // now read the individual tags' data
        for (tag: Tag in alltags) {
            with (this) { tag.readTag(this) }
            val tagid = tagid(tag.refno, tag.code)
            tagidMap[tagid] = tag // track all tags in a map, key is the "tag id".
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

        // construct the netcdf objects
        construct(alltags)
        /* if (useHdfEos) {
            isEos = HdfEos.amendFromODL(raf.getLocation(), this, root)
            if (isEos) {
                adjustDimensions()
                val history: String = root.getAttributeContainer().findAttributeString("_History", "")
                root.addAttribute(Attribute("_History", "$history; HDF-EOS StructMetadata information was read"))
            }
        } */
    }

    /* fun getEosInfo(f: Formatter?) {
        HdfEos.getEosInfo(raf.getLocation(), this, root, f)
    }
    
     */

    ////////////////////////////////////////////////////////////////////////////
    
    private fun construct(alltags: List<Tag>) {
        val vars = mutableListOf<Variable.Builder>()

        // pass 1 : Vgroups with special classes
        for (t: Tag in alltags) {
            if (t.code == 306) { // raster image
                val v: Variable.Builder? = makeImage(t as TagGroup)
                if (v != null) vars.add(v)
            } else if (t.code == 1965) {
                val vgroup: TagVGroup = t as TagVGroup
                if (vgroup.className.startsWith("Dim") || vgroup.className.startsWith("UDim")) {
                    makeDimension(vgroup)

                } else if (vgroup.className.startsWith("Var")) {
                    val v = makeVariable(vgroup)
                    if (v != null) vars.add(v)

                } else if (vgroup.className.startsWith("CDF0.0")) {
                    addGlobalAttributes(vgroup)
                }
            }
        }

        // pass 2 - VHeaders, NDG
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.code == 1962) { // VHeader
                val tagVH: TagVH = t as TagVH
                if (tagVH.className.startsWith("Data")) {
                    val v = makeVariable(tagVH)
                    if (v != null) vars.add(v)
                }
            } else if (t.code == 720) { // numeric data group
                val v = makeVariable(t as TagGroup)
                vars.add(v)
            }
        }

        // pass 3 - misc not claimed yet
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.code == 1962) { // VHeader
                val vh: TagVH = t as TagVH
                if (!vh.className.startsWith("Att") && !vh.className.startsWith("_HDF_CHK_TBL")) {
                    val v = makeVariable(vh)
                    if (v != null) vars.add(v)
                }
            }
        }

        // pass 4 - Groups
        for (t: Tag in alltags) {
            if (t.isUsed) continue
            if (t.code == 1965) { // VGroup
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
                        if (t.code == 105) "description" else CDM.LONG_NAME,
                        t.text
                    )
                )
                t.isUsed = true
            }
        }

        // misc global attributes
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
    }

    /*
    private fun adjustDimensions() {
        val dimUsedMap: Multimap<Dimension, Variable.Builder> =
            ArrayListMultimap.create<Dimension, Variable.Builder>()
        makeDimensionMap(root, dimUsedMap)
        val dimUsed: Set<Dimension> = dimUsedMap.keySet()

        // remove unused dimensions from root group
        for (dim: Dimension in root.getDimensions()) {
            if (!dimUsed.contains(dim)) {
                root.removeDimension(dim.getShortName())
            }
        }

        // push used dimensions to the lowest group that contains all variables
        for (dim: Dimension in dimUsed) {
            var lowest: Group.Builder? = null
            val vlist: Collection<Variable.Builder> = dimUsedMap[dim]
            for (v: Variable.Builder in vlist) {
                if (v.spObject == null) {
                    log.warn(java.lang.String.format("adjustDimensions %s missing vinfo (new)%n", v.name))
                    continue
                }
                val vinfo: Vinfo = v.spObject as Vinfo
                val gb: Group.Builder = vinfo.group
                if (lowest == null) {
                    lowest = gb
                } else {
                    lowest = lowest.commonParent(gb)
                }
            }
            if (lowest != null) {
                root.removeDimensionFromAllGroups(root, dim)
                lowest.addDimensionIfNotExists(dim)
            }
        }
    }


    /** Make a multimap of Dimensions and all the variables that reference them, in this group and its nested groups.  */
    private fun makeDimensionMap(parent: Group.Builder, dimUsedMap: Multimap<Dimension, Variable.Builder>) {
        for (v: Variable.Builder in parent.variables) {
            for (d: Dimension in getDimensionsFor(parent, v)) {
                if (d.isShared) {
                    dimUsedMap.put(d, v)
                }
            }
        }
        for (g: Group.Builder in parent.groups) {
            makeDimensionMap(g, dimUsedMap)
        }
    }
     */

    private fun getDimensionsFor(gb: Group.Builder, vb: Variable.Builder): List<Dimension> {
        val dims = mutableListOf<Dimension>()
        for (dim: Dimension in vb.dimensions) {
            if (dim.isShared) {
                val sharedDim = gb.dimensions.find{ it.name == dim.name }
                if (sharedDim == null) {
                    throw IllegalStateException("Shared Dimension $dim does not exist in a parent proup")
                } else {
                    dims.add(sharedDim)
                }
            } else {
                dims.add(dim)
            }
        }
        return dims
    }

    private fun makeDimension(vtags: TagVGroup) {
        if (vtags.name == "time") {
            println()
        }
        val dims = mutableListOf<TagVH>()
        var data: Tag? = null
        for (i in 0 until vtags.nelems) {
            val tag: Tag = tagidMap[tagid(vtags.elem_ref.get(i), vtags.elem_tag.get(i))] ?: throw IllegalStateException()
            if (tag.code == 1962) dims.add(tag as TagVH)
            if (tag.code == 1963) data = tag
        }
        if (dims.isEmpty()) throw IllegalStateException()
        
        var length = 0
        if (data != null) {
            val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
            length = raf.readInt(state)
            data.isUsed = true
        } else {
            for (vh: TagVH in dims) {
                vh.isUsed = true
                data = tagidMap[tagid(vh.refno, TagEnum.VS.code)]
                if (null != data) {
                    data.isUsed = true
                    val state = OpenFileState(data.offset, ByteOrder.BIG_ENDIAN)
                    val length2: Int = raf.readInt(state)
                    if (debugConstruct) println("dimension length=" + length2 + " for TagVGroup= " + vtags + " using data " + data.refno)
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
        val isUnlimited = length == 0
        val dim = Dimension(vtags.name, length, isUnlimited, true)
        rootBuilder.addDimension(dim)
    }

    private fun addGlobalAttributes(group: TagVGroup) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))] ?: throw IllegalStateException()
            if (tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val lowername: String = vh.name.lowercase(Locale.getDefault())
                    if (vh.nfields == 1.toShort() &&
                        H4type.getDataType(vh.fld_type[0].toInt()) === Datatype.CHAR
                        && (vh.fld_isize[0] > 4000 || lowername.startsWith("archivemetadata")
                                || lowername.startsWith("coremetadata") || lowername.startsWith("productmetadata")
                                || lowername.startsWith("structmetadata"))
                    ) {
                        val v = makeVariable(vh)
                        if (v != null) {
                            rootBuilder.addVariable(v) // // large EOS metadata - make into variable in root group
                        }
                    } else {
                        val att: Attribute? = makeAttribute(vh)
                        if (null != att) {
                            rootBuilder.addAttribute(att)
                        } // make into attribute in root group
                    }
                }
            }
        }
        group.isUsed = true
    }

    private fun makeAttribute(vh: TagVH): Attribute? {
        val data: Tag = tagidMap.get(tagid(vh.refno, TagEnum.VS.code)) ?: throw IllegalStateException()

        // for now assume only 1
        if (vh.nfields.toInt() != 1) throw IllegalStateException()
        val name: String = vh.name
        val type = vh.fld_type[0].toInt()
        val size: Int = vh.fld_isize[0]
        val nelems: Int = vh.nvert
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
            20, 21 -> {
                val vals = mutableListOf<Byte>()
                repeat (nelems) { vals.add(raf.readByte(state)) }
                Attribute(name, Datatype.BYTE, vals)
            }
            22, 23 -> {
                val vals = mutableListOf<Short>()
                repeat (nelems) { vals.add(raf.readShort(state)) }
                Attribute(name, Datatype.SHORT, vals)
            }
            24, 25 -> {
                val vals = mutableListOf<Int>()
                    repeat (nelems) { vals.add(raf.readInt(state)) }
                            Attribute(name, Datatype.INT, vals)
                }
            26, 27 -> {
                val vals = mutableListOf<Long>()
                repeat (nelems) { vals.add(raf.readLong(state)) }
                Attribute(name, Datatype.LONG, vals)
            }
            else -> null
        }
        if (debugAtt) println("added attribute $att")
        return att
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
                    val att: Attribute? = makeAttribute(vh)
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
        // root.removeGroup(g.name) ??

        val groupExisting = parent.groups.find { it.name == gb.name }
        if (groupExisting != null) groupExisting.name += tag.refno // disambiguate
        parent.addGroup(gb)
    }

    private fun makeImage(group: TagGroup): Variable.Builder? {
        var dimTag: TagRIDimension? = null
        val ntag: TagNumberType
        var data: Tag? = null
        val vinfo = Vinfo(group.refno)
        group.isUsed = true

        // use the list of elements in the group to find the other tags
        for (i in 0 until group.nelems) {
            val tag: Tag? =
                tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.warn("Image Group ${group.tag()} has missing tag=${group.elem_ref[i]}/${group.elem_tag[i]}")
                return null
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then used, to avoid redundant variables
            if (tag.code == 300) dimTag = tag as TagRIDimension
            if (tag.code == 302) data = tag
        }
        if (dimTag == null) {
            log.warn("Image Group " + group.tag() + " missing dimension tag")
            return null
        }
        if (data == null) {
            log.warn("Image Group " + group.tag() + " missing data tag")
            return null
        }

        // get the NT tag, referred to from the dimension tag
        val tag: Tag? = tagidMap[tagid(dimTag.nt_ref, TagEnum.NT.code)]
        if (tag == null) {
            log.warn("Image Group " + group.tag() + " missing NT tag")
            return null
        }
        ntag = tag as TagNumberType
        if (debugConstruct) println("construct image " + group.refno)
        vinfo.start = data.offset
        vinfo.tags.add(group)
        vinfo.tags.add(dimTag)
        vinfo.tags.add(data)
        vinfo.tags.add(ntag)

        // assume dimensions are not shared for now
        if (dimTag.dims.isEmpty()) {
            dimTag.dims.add(makeDimensionUnshared("ydim", dimTag.ydim))
            dimTag.dims.add(makeDimensionUnshared("xdim", dimTag.xdim))
        }
        val vb = Variable.Builder()
        vb.name = "Image-" + group.refno
        vb.datatype = H4type.getDataType(ntag.numberType)
        vb.dimensions.addAll(dimTag.dims)
        vinfo.setVariable(vb)
        return vb
    }

    private fun makeDimensionUnshared(dimName: String, len: Int): Dimension {
        return Dimension(dimName, len, false ,false)
    }

    private fun makeVariable(tagVH: TagVH): Variable.Builder? {
        val vinfo = Vinfo(tagVH.refno)
        vinfo.tags.add(tagVH)
        tagVH.vinfo = vinfo
        tagVH.isUsed = true
        val data: TagData? =
            tagidMap[tagid(tagVH.refno, TagEnum.VS.code)] as TagData?
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
        if (tagVH.nfields.toInt() == 1) {
            vb.datatype = H4type.getDataType(tagVH.fld_type[0].toInt())
            if (tagVH.nvert > 1) {
                if (tagVH.fld_order[0] > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nvert, tagVH.fld_order.get(0).toInt()))
                } else if (tagVH.fld_order[0] < 0) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nvert, tagVH.fld_isize.get(0)))
                } else {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.nvert))
                }
            } else {
                if (tagVH.fld_order.get(0) > 1) {
                    vb.setDimensionsAnonymous(intArrayOf(tagVH.fld_order.get(0).toInt()))
                } else if (tagVH.fld_order.get(0) < 0) vb.setDimensionsAnonymous(
                    intArrayOf(tagVH.fld_isize.get(0))
                ) else {
                    vb.dimensions.clear()
                }
            }
            vinfo.setData(data, vb.datatype!!.size)
        } else {
            if (tagVH.nvert > 1) {
                vb.setDimensionsAnonymous(intArrayOf(tagVH.nvert))
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

    private fun makeVariable(group: TagVGroup): Variable.Builder? {
        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true
        var dim: TagSDDimension? = null
        var ntag: TagNumberType? = null
        var data: TagData? = null

        val dims = mutableListOf<Dimension>()
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.error("Reference tag missing= " + group.elem_ref.get(i) + "/" + group.elem_tag.get(i))
                continue
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Vgroup, then not needed, to avoid redundant variables
            if (tag.code == 106) ntag = tag as TagNumberType
            if (tag.code == 701) dim = tag as TagSDDimension
            if (tag.code == 702) data = tag as TagData
            if (tag.code == 1965) {
                val vg: TagVGroup = tag as TagVGroup
                if (vg.className.startsWith("Dim") || vg.className.startsWith("UDim")) {
                    val dimName: String = vg.name
                    val d: Dimension = rootBuilder.dimensions.find{it.name == dimName } ?: throw IllegalStateException()
                    dims.add(d)
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
        vb.dimensions.addAll(dims)
        vb.datatype = H4type.getDataType(ntag.numberType.toInt())
        vinfo.setVariable(vb)
        vinfo.setData(data, vb.datatype!!.size)

        // apparently the 701 SDDimension tag overrides the VGroup dimensions
        require(dim.shape.size == vb.dimensions.size)
        var ok = true
        val vdimensions: List<Dimension> = vb.dimensions
        for (i in dim.shape.indices) {
            val vdim: Dimension = vdimensions[i]
            if (dim.shape.get(i) != vdim.length) {
                if (warnings) log.info("${dim.shape.get(i)} != ${vdim.length} for ${vb.name}")
                ok = false
            }
        }
        if (!ok) {
            vb.setDimensionsAnonymous(dim.shape)
        }

        // look for attributes
        addVariableAttributes(group, vinfo)
        if (debugConstruct) {
            println(("added variable " + vb.name).toString() + " from VG " + group.refno)
            println("  SDdim= " + dim.detail())
            print("  VGdim= ")
            for (vdim: Dimension in dims) print(vdim.toString() + " ")
            println()
        }
        return vb
    }

    private fun makeVariable(group: TagGroup): Variable.Builder {
        val vinfo = Vinfo(group.refno)
        vinfo.tags.add(group)
        group.isUsed = true

        // dimensions
        var dim: TagSDDimension? = null
        var data: TagData? = null
        for (i in 0 until group.nelems) {
            val tag: Tag? = tagidMap[tagid(group.elem_ref.get(i), group.elem_tag.get(i))]
            if (tag == null) {
                log.error("Cant find tag " + group.elem_ref.get(i) + "/" + group.elem_tag.get(i) + " for group=" + group.refno)
                continue
            }
            vinfo.tags.add(tag)
            tag.vinfo = vinfo // track which variable this tag belongs to
            tag.isUsed = true // assume if contained in Group, then its used, in order to avoid redundant variables
            if (tag.code == 701) dim = tag as TagSDDimension
            if (tag.code == 702) data = tag as TagData
        }
        if ((dim == null) || (data == null)) throw IllegalStateException()

        val nt: TagNumberType = tagidMap.get(tagid(dim.nt_ref, TagEnum.NT.code)) as TagNumberType? ?: throw IllegalStateException()
        val vb = Variable.Builder()
        vb.name = "SDS-" + group.refno
        vb.setDimensionsAnonymous(dim.shape)
        val dataType = H4type.getDataType(nt.numberType)
        vb.datatype = dataType
        vinfo.setVariable(vb)
        vinfo.setData(data, dataType.size)

        // fill value?
        val tagFV = tagidMap.get(tagid(dim.nt_ref, TagEnum.FV.code))
        if ((tagFV != null) and (tagFV is TagFV)) {
            vinfo.fillValue = (tagFV as TagFV).readFillValue(this, dataType)
        }

        // now that we know n, read attribute tags
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap.get(tagid(group.elem_ref.get(i), group.elem_tag.get(i))) ?: throw IllegalStateException()
            if (tag.code == 704) {
                val labels: TagTextN = tag as TagTextN
                labels.readTag(this, dim.rank.toInt())
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.LONG_NAME, Datatype.STRING, labels.text))
            }
            if (tag.code == 705) {
                val units: TagTextN = tag as TagTextN
                units.readTag(this, dim.rank.toInt())
                tag.isUsed = true
                vb.attributes.add(Attribute(CDM.UNITS, Datatype.STRING, units.text))
            }
            if (tag.code == 706) {
                val formats: TagTextN =
                    tag as TagTextN
                formats.readTag(this, dim.rank.toInt())
                tag.isUsed = true
                vb.attributes.add(Attribute("formats", Datatype.STRING, formats.text))
            }
            if (tag.code == 707) {
                val minmax: TagSDminmax = tag as TagSDminmax
                tag.isUsed = true
                vb.attributes.add(Attribute("min", dataType, listOf(minmax.getMin(dataType))))
                vb.attributes.add(Attribute("max", dataType, listOf(minmax.getMax(dataType))))
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

    private fun addVariableAttributes(group: TagGroup, vinfo: Vinfo) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap.get(tagid(group.elem_ref.get(i), group.elem_tag.get(i))) ?: throw IllegalStateException()
            if (tag.code == 1962) {
                val vh: TagVH = tag as TagVH
                if (vh.className.startsWith("Att")) {
                    val att: Attribute? = makeAttribute(vh)
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
                    val att: Attribute? = makeAttribute(vh)
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
        private var debugConstruct = false // show CDM objects as they are constructed
        private var debugAtt = false // show CDM attributes as they are constructed
        private var debugLinked = false // linked data
        private var debugChunkTable = false // chunked data
        private var debugChunkDetail = false // chunked data
        private var debugTracker = false // memory tracker
        private val warnings = false // log messages
        private var useHdfEos = true // allow to turn hdf eos processing off

        fun useHdfEos(`val`: Boolean) {
            useHdfEos = `val`
        }

        // this is a unique id for a message in a file
        fun tagid(refno: Int, code: Int): Int {
            val result = (code and 0x3FFF) // why not FFFF ?
            val result2 = (refno and 0xffff) shl 16
            return result + result2
        }
    }
}