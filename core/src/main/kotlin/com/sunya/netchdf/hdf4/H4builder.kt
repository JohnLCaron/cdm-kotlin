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

private const val attLengthMax = 4000

class H4builder(val raf : OpenFile, val valueCharset : Charset) {
    var rootBuilder: Group.Builder = Group.Builder("")
    val metadata = mutableListOf<Attribute>()
    val structMetadata = mutableListOf<String>()

    private val alltags = mutableListOf<Tag>() // in order as they appear in the file
    private val completedObjects = mutableSetOf<Int>()

    internal val tagidMap = mutableMapOf<Int, Tag>()
    private var imageCount = 0

    fun type() : String {
        return if ( structMetadata.isEmpty()) "hdf4     " else "hdf-eos2 "
    }

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
    private val unparentedGroups = mutableMapOf<Int, Group4>() // vg refno, vg group4
    private val parentedGroups = mutableMapOf<Int, TagVGroup>() // vg refno, vg
    private val sdGroups = mutableMapOf<Int, TagVGroup>() // sd refno, sd parent group
    private val vhGroups = mutableMapOf<Int, TagVGroup>() // vh refno, vh parent group

    val sdAliasMap = mutableMapOf<Int, TagVGroup>() // sd refno, sd parent group
    val vhAliasMap = mutableMapOf<Int, TagVGroup>()    // vh refno, vh group

    fun make() {
        VgroupIterate("VgroupNesting") { t : Tag -> VgroupNesting(t as TagVGroup, null, Indent(2)) }
        VgroupIterate("VgroupFindParents") { t : Tag -> VgroupFindParents(t as TagVGroup, Indent(2)) }

        unparentedGroups.values.forEach {
            val vgroup = it.vgroup
            val group = Group.Builder(vgroup.name)
            rootBuilder.addGroup(group)
            Vgroup4Read(it, group)
        }
        // now add all the orphans to the rootGroup
        VgroupIterate("VgroupRead") { t : Tag -> VgroupRead(t as TagVGroup, rootBuilder) }

        SDiterate(rootBuilder)
        GRiterate(rootBuilder)
        VStructureIterate(rootBuilder)

        metadata.forEach { makeVariableFromStringAttribute(it, rootBuilder) }

        if (structMetadata.isNotEmpty()) {
            val sm = structMetadata.joinToString("")
            ODLparser(rootBuilder, false).applyStructMetadata(sm)
        }

        // it appears that these are in reverse order in the C library
        var dataLabel = 0
        var dataDesc = 0
        var fileLabel = 0
        var fileDesc = 0
        alltags.reversed().forEach {
            when (it.code) {
                TagEnum.VERSION.code -> rootBuilder.addAttribute(Attribute("HDF4FileVersion", (it as TagVersion).value()))
                TagEnum.DIL.code -> rootBuilder.addAttribute(Attribute("DataLabel.${dataLabel++}", (it as TagAnnotate).text))
                TagEnum.DIA.code -> rootBuilder.addAttribute(Attribute("DataDesc.${dataDesc++}", (it as TagAnnotate).text))
                TagEnum.FID.code -> rootBuilder.addAttribute(Attribute("FileLabel.${fileLabel++}", (it as TagText).text))
                TagEnum.FD.code -> rootBuilder.addAttribute(Attribute("FileDesc.${fileDesc++}", (it as TagText).text))
            }
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

    private fun VgroupIterate(name : String, lamda : (t : Tag) -> Unit) {
        if (debugConstruct) println("--VgroupIterate $name")
        for (t: Tag in alltags) {
            if (t.tagEnum() == TagEnum.VG) {
                lamda.invoke(t)
            }
        }
    }

    // create a group heirarchy, allowing groups to be in any order in the file
    private fun VgroupNesting(vgroup: TagVGroup, parent : Group4?, indent:Indent) {
        // TODO too lame. perhaps its a group if it has nested groups ??
        if (!isNestedGroup(vgroup.className)) {
            return
        }
        if (debugVGroup) println("$indent VgroupNesting ${vgroup.refno} '${vgroup.name}' ${vgroup.className} ")
        if (parentedGroups[vgroup.tagid()] != null) {
            if (debugVGroup) println("$indent   already parented ${vgroup.name}")
            return
        }
        val already = unparentedGroups[vgroup.tagid()]
        if (already != null && parent != null) {
            parent.subgroups.add(already)
            unparentedGroups.remove(vgroup.tagid())
            parentedGroups[vgroup.tagid()] = vgroup
            if (debugVGroup) println("$indent   add ${vgroup.name} to ${parent.vgroup.name}")
            return
        }
        val group4 = Group4(vgroup)
        if (parent == null) {
            unparentedGroups[vgroup.tagid()] = group4
            if (debugVGroup) println("$indent   unparented ${vgroup.name}")
        } else {
            parentedGroups[vgroup.tagid()] = vgroup
            parent.subgroups.add(group4)
            if (debugVGroup) println("$indent   add ${vgroup.name} to ${parent.vgroup.name}")
        }

        repeat(vgroup.nelems) {
            val tagRef = vgroup.elem_ref[it]
            val tagCode = vgroup.elem_tag[it]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            val tag = tagidMap[tagid]
            if (tag == null) {
                println("*** Dont have tag (refno=${tagRef}, code=${TagEnum.byCode(tagCode)}) referenced in vgroup '${vgroup.name}'")
            } else if (tage == TagEnum.VG) {
                VgroupNesting(tag as TagVGroup, group4, indent.incr())
            } else if (tage == TagEnum.NDG) {
                sdGroups[tagRef] = vgroup // why needed we already know the group
                if (debugVGroup) println("$indent   sdGroups add ${tag}")
            } else if (tage == TagEnum.VH) {
                vhGroups[tagRef] = vgroup // why needed we already know the group
                if (debugVGroup) println("$indent   vhGroups add ${tag}")
            }
        }
    }

    // find sd, vh group parents
    private fun VgroupFindParents(vgroup: TagVGroup, indent:Indent) {
        if (debugVGroupDetails) println("$indent  VgroupFindParents ${vgroup.refno} ${vgroup.className} ${vgroup.name} ")
        repeat(vgroup.nelems) {
            val tagRef = vgroup.elem_ref[it]
            val tagCode = vgroup.elem_tag[it]
            val tage = TagEnum.byCode(tagCode)
            val tagid = tagid(tagRef, tagCode)
            if (debugVGroupDetails) println("$indent    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)}")
            val tag = tagidMap[tagid]
            if (tag == null) {
                println("*** Dont have tag (refno=${tagRef}, code=${TagEnum.byCode(tagCode)}) referenced in vgroup '${vgroup.name}'")
            } else if (tage == TagEnum.VG) {
                VgroupFindParents(tag as TagVGroup, indent.incr())
            } else if (tage == TagEnum.NDG) {
                sdAliasMap[tagRef] = vgroup
                if (debugVGroup) println("$indent   sdAliasMap add ${tag}")
            } else if (tage == TagEnum.VH) {
                vhAliasMap[tagRef] = vgroup
                if (debugVGroup) println("$indent   vhAliasMap add ${tag}")
            }
        }
    }

    // read a VGroup (1965). we may not know the parent yet
    private fun VgroupRead(vgroup : TagVGroup, parent : Group.Builder) {
        if (completedObjects.contains(vgroup.tagid())) {
            if (debugConstruct) println("VgroupRead skip ${vgroup.refno} '${vgroup.name}'")
            return
        }
        completedObjects.add(vgroup.tagid())
        if (vgroup.nelems == 0) {
            if (debugConstruct) println("  VgroupRead empty group '${vgroup.name}' ref=${vgroup.refno}")
            return
        }

        if (vgroup.nelems > 0) {
            //if (isNestedGroup(vgroup.className)) {
            //    VgroupGroup(vgroup, parent)
            //} else
            if (isDimClass(vgroup.className)) {
                VgroupDim(vgroup, parent)
            } else if (vgroup.className == "Var0.0") { // ??
                VgroupVar(vgroup, parent)
            } else if (vgroup.className == "CDF0.0") {
                // LOOK ignoring the contents of the VGroup, and looking at the attributes on the group
                VgroupCDF(vgroup, parent)
            }
        }
    }

    // read a VGroup. we know the parent group and nested groups
    private fun Vgroup4Read(group4 : Group4, group : Group.Builder) {
        val vgroup = group4.vgroup
        if (completedObjects.contains(vgroup.tagid())) {
            if (debugConstruct) println("VgroupRead skip ${vgroup.refno} '${vgroup.name}'")
            return
        }
        completedObjects.add(vgroup.tagid())
        if (debugConstruct) println("--Vgroup4Read '${vgroup.name}' ref=${vgroup.refno}")

        if (vgroup.nelems > 0) {
            if (isDimClass(vgroup.className)) {
                VgroupDim(vgroup, group)
            } else if (vgroup.className == "Var0.0") { // ??
                VgroupVar(vgroup, group)
            } else if (vgroup.className == "CDF0.0") {
                // LOOK ignoring the contents of the VGroup, and looking at the attributes on the group
                VgroupCDF(vgroup, group)
            } else {
                repeat(vgroup.nelems) { objIdx ->
                    val tagRef = vgroup.elem_ref[objIdx]
                    val tagCode = vgroup.elem_tag[objIdx]
                    val tage = TagEnum.byCode(tagCode)
                    val tagid = tagid(tagRef, tagCode)
                    val tag = tagidMap[tagid] ?: throw RuntimeException("Dont have tag (${tagRef}, ${TagEnum.byCode(tagCode)})")

                    if (tage == TagEnum.NDG) {
                        if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)}")
                        val alias = sdAliasMap[tagRef]
                        if (alias != null)
                            VgroupVar(alias, group)
                        else {
                            SDread(tag as TagDataGroup, null, group, emptyList())
                        }
                    } else if (tage == TagEnum.VH) {
                        val vtag = tag as TagVH
                        if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)} '${vtag.className}' '${vtag.name}'")
                        VStructureRead(vtag, vhAliasMap[tagRef]?.name, group, true)
                    }
                }
            }
        }

        group4.subgroups.filter{ it.vgroup.nelems > 0 }.forEach {
            val nested = Group.Builder(it.vgroup.name)
            group.addGroup(nested)
            Vgroup4Read(it, nested)
        }
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
                    SDread(tag as TagDataGroup, null, nested, emptyList())
                }
            } else if (tage == TagEnum.VH) {
                val vtag = tag as TagVH
                if (debugVGroup) println("    ${tagidName(tagid)} ${TagEnum.byCode(tagCode)} '${vtag.className}' '${vtag.name}'")
                VStructureRead(vtag, vhAliasMap[tagRef]?.name, nested, true)
            }
        }
    }

    private fun VgroupVar(vgroup: TagVGroup, parent: Group.Builder) {
        if (debugConstruct) println("  VgroupVar '${vgroup.name}' ref=${vgroup.refno}")

        val dims = mutableListOf<String>()
        val tagVHs = mutableListOf<TagVH>()
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
                    tagVHs.add(tagVH)
                }
            }
        }

        // tagVH's on the group tag (TagVGroup) might be attributes
        if (vb != null) {
            tagVHs.forEach {
                val att = VStructureReadAttribute(it)
                if (att != null) {
                    vb!!.addAttribute(att)
                    val vinfo = vb!!.spObject as Vinfo
                    if (att.name.equals(NUG.FILL_VALUE)) vinfo.setFillValue(att)
                }
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
                        val promoted = attr.isString && attr.values.size == 1 && (attr.values[0] as String).length > attLengthMax
                        if (EOS.isMetadata(attr.name) || promoted) {
                            metadata.add(attr)
                            if (attr.name.startsWith("StructMetadata")) {
                                this.structMetadata.add(attr.values[0] as String)
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
                SDread(t as TagDataGroup, null, rootBuilder, emptyList())
            }
        }
    }

    private fun SDread(group: TagDataGroup, groupName : String?, parent : Group.Builder, dimNames : List<String>): Variable.Builder? {
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

        val vb = Variable.Builder(groupName ?: "Data-Set-${group.refno}")

        // have to use the variables to figure out the dimensions. barf
        val rank = dimSDD.shape.size
        val ndimNames = dimNames.size
        repeat(ndimNames) {
            val dim = Dimension(dimNames[it], dimSDD.shape[it])
            vb.addDimension(dim)
            parent.addDimensionIfNotExists(dim)
        }
        // sometimes dont have any or enough names, use anon dimensions for remaining
        for (dimidx in ndimNames until rank) {
            val dim = Dimension(dimSDD.shape[dimidx]) // anon
            vb.addDimension(dim)
        }

        val nt: TagNT = tagidMap.get(tagid(dimSDD.data_nt_ref, TagEnum.NT.code)) as TagNT? ?: throw IllegalStateException()
        val dataType = H4type.getDataType(nt.numberType)
        vb.datatype = dataType
        vinfo.setVariable(vb)
        vinfo.setData(data, dataType.size)

        // Apparently SD uses defaults (but not VS). They are sort-of NC, except for unsigned.
        vinfo.fillValue = getSDefaultFillValue(dataType)
        // then look for this tag. Elsewhere we look for _FillValue attribute.
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
        } else if (tagVH.className.startsWith("DimVal") || tagVH.className.startsWith("_HDF_CHK_TBL")) {
            return null
        }

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
        vinfo.setData(data, tagVH.ivsize)
        data.isUsed = true
        data.vinfo = vinfo
        if (tagVH.nfields < 1) throw IllegalStateException()

        val vsname = if (tagVH.name.equals("Ancillary_Data")) tagVH.className else tagVH.name // Lame
        val vb = Variable.Builder(vsname)
        vinfo.setVariable(vb)

        val members = tagVH.readStructureMembers()
        val nrecords = tagVH.nelems
        if (members.size == 1) { // one field - dont make it into a structure
            val member = members[0]
            vb.datatype = member.datatype
            vinfo.elemSize = member.datatype.size // look correct the size, not tagVH.ivsize
            val totalNelems = nrecords * member.nelems
            if (totalNelems > 1) {
                if (nrecords != 1 && member.nelems != 1)
                    vb.setDimensionsAnonymous(intArrayOf(nrecords,  member.nelems))
                else
                    vb.setDimensionsAnonymous(intArrayOf(totalNelems))
            }
        } else {
            val typedef = CompoundTypedef(tagVH.name, members)
            vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
            if (nrecords > 1) {
                vb.setDimensionsAnonymous(intArrayOf(nrecords))
            }
            // LOOK!
            rootBuilder.addTypedef(typedef)
        }

        if (debugConstruct) {
            println("added variable ${vb.name} from VH $tagVH group $groupName")
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
        // LOOK the C lib probably just uses  nelems = fld_nelems
        val nelems = if (datatype == Datatype.CHAR) vh.nelems else vh.nelems * fld_nelems

        if (name == "start_latlon")
            println()

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
                repeat (rig.nelems) {
                    val ntagid = tagid(rig.elem_ref[it], rig.elem_tag[it])
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

        val vb = Variable.Builder("Raster_Image_#" + imageCount)
        val datatype = H4type.getDataType(ntag.numberType)
        vinfo.start = tagRasterImage.offset
        vb.datatype = datatype
        vinfo.elemSize = datatype.size

        // assume dimensions are not shared
        vb.dimensions.add(Dimension("ydim", dimTag.ydim.toLong(), false))
        vb.dimensions.add(Dimension("xdim", dimTag.xdim.toLong(), false))

        vinfo.setVariable(vb)
        imageCount++

        parent.addVariable(vb)
        if (debugConstruct) println("GRVariable ${vb.name}")

        return vb
    }

    private fun makeVariableFromStringAttribute(att : Attribute, parent : Group.Builder) {
        require(att.isString)
        val svalue = att.values[0] as String
        val vb = Variable.Builder(att.name)
        vb.datatype = Datatype.STRING
        vb.spObject = Vinfo(-1).setSValue(svalue)
        parent.addVariable(vb)
    }

    // looking for tagVHs on the TagDataGroup (NDG) might be attributes
    private fun addVariableAttributes(group: TagDataGroup, vb : Variable.Builder, vinfo: Vinfo) {
        // look for attributes
        repeat(group.nelems) {
            val tag = tagidMap.get(tagid(group.elem_ref[it], group.elem_tag[it]))
            if (tag != null && tag.code == 1962) { // VH
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

private class Group4(val vgroup : TagVGroup) {
    val subgroups = mutableListOf<Group4>()
}

// seriously fucked up to rely on these conventions

fun isNestedGroup(className : String) : Boolean {
    return className.startsWith("SWATH") or className.startsWith("GRID") or className.startsWith("POINT")
}

fun isDimClass(className : String) : Boolean {
    return (className == "Dim0.0") or (className == "UDim0.0")
}