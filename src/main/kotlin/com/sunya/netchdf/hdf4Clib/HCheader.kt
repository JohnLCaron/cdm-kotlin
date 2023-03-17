package com.sunya.netchdf.hdf4Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.makeStringZ
import com.sunya.netchdf.hdf4.*
import com.sunya.netchdf.hdf4.H4builder.Companion.tagid
import com.sunya.netchdf.hdf4.H4builder.Companion.tagidName
import com.sunya.netchdf.hdf4.H4builder.Companion.tagidNameR
import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.io.IOException
import java.lang.foreign.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private val debugVGroup = false
private val debugVGroupDetails = false
private val debugVSdata = false
private val debugVSdetails = false
private val debugSD = false
private val debugSDdetails = false
private val debugGR = false
private val debugAttributes = false
private val debugAttributeBug = false
private val debugDims = false

private val MAX_NAME = 255L
private val MAX_FIELDS_NAME = 1000L

class HCheader(val filename: String) {
    internal val rootGroup4 = Group4("", null)
    val rootGroup: Group
    val read_access_mode: MemorySegment

    internal var sdsStartId = 0
    internal var fileOpenId = 0
    internal var grStartId = 0

    private val completedObjects = mutableSetOf<Int>()
    private val metadata = mutableListOf<Attribute>()
    private var structMetadata: String? = null

    init {
        MemorySession.openConfined().use { session ->
            this.read_access_mode = session.allocateUtf8String("r")
            build(session)
            this.rootGroup = rootGroup4.gb.build(null)
            println(this.rootGroup)
        }
    }

    @Throws(IOException::class)
    private fun build(session: MemorySession) {
        val filenameSeg: MemorySegment = session.allocateUtf8String(filename)

        /* Initialize the SD interface. */
        this.sdsStartId = SDstart(filenameSeg, DFACC_READ())

        /* Initialize the rest of the API. */
        this.fileOpenId = Hopen(filenameSeg, DFACC_READ(), 0)
        checkErr("Vinitialize", Vinitialize(this.fileOpenId))
        this.grStartId = GRstart(this.fileOpenId)

        VgroupIterate(session, rootGroup4)
        SDiterate(session, rootGroup4)
        GRiterate(session, rootGroup4)
        VStructureIterate(session, rootGroup4)

        metadata.forEach { makeVariableFromStringAttribute(rootGroup4, it) }

        if (structMetadata != null) {
            ODLparser(rootGroup4.gb, false).applyStructMetadata(structMetadata!!)
        }
    }

    fun close() {
        val sdret = SDend(this.sdsStartId)
        System.out.printf("SDend ret=%d %n", sdret)
        val gret = GRend(this.grStartId)
        System.out.printf("GRend ret=%d %n", gret)
        val vsret = Vfinish(this.fileOpenId)
        System.out.printf("Vfinish ret=%d %n", vsret)
        val ret = Hclose(this.fileOpenId)
        System.out.printf("Hclose ret=%d %n", ret)
    }

    ///////////////////////////////////////////////////////////////////////////////
    // The Vgroup API: group related objects. DFTAG_VG.
    // "vgroups can contain any combination of HDF data objects". so that narrows it down.

    // Iterates over Vgroups (1965)
    private fun VgroupIterate(session: MemorySession, g4: Group4) {
        if (debugVGroup) println("iterateVgroups '${g4.gb.name}'")

        var last_ref = -1
        while (true) {
            val vgroup_ref = Vgetid(fileOpenId, last_ref)
            if (vgroup_ref == -1) {
                break
            }
            VgroupRead(session, g4, vgroup_ref)
            last_ref = vgroup_ref
        }

        // LOOK is there anything in here we need?
        /* retrieves the reference numbers of lone vdatas in the file
        if (debugVGroup) println("iterateVlone")
        val max_refs = 128
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_refs.toLong())
        val num_of_lone_vdatas = Vlone(fileid, ref_array_p, max_refs);
        val ref_array = IntArray(num_of_lone_vdatas) { ref_array_p.getAtIndex(C_INT, it.toLong()) }

        repeat(num_of_lone_vdatas) {
            readVGroup(session, g4, ref_array[it])
            val vdata_id = VSattach(fileid, ref_array[idx], vdata_access_mode)
            if (vdata_id == -1) break
            makeVvariable(session, g4, vdata_id)
            checkErr("Vdetach", Vdetach(vdata_id))
        }

         */

    }

    // read a VGroup (1965)
    private fun VgroupRead(session: MemorySession, g4: Group4, vgroup_ref: Int) {
        val tagid = tagid(vgroup_ref, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugVGroup) println(" VgroupRead skip $vgroup_ref")
            return
        }
        completedObjects.add(tagid)

        val vgroup_id = Vattach(fileOpenId, vgroup_ref, read_access_mode)
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)
        checkErr("Vgetclass", Vgetclass(vgroup_id, vclass_p))
        val vclass = vclass_p.getUtf8String(0)

        val nobjects_p = session.allocate(C_INT, 0)
        val name_p: MemorySegment = session.allocate(MAX_NAME)

        val ret = Vinquire(vgroup_id, nobjects_p, name_p)
        if (ret != 0) {
            println("Vinquire failed $ret")
            return
        }
        val max_nobjects = nobjects_p[C_INT, 0]
        val groupName = name_p.getUtf8String(0)

        val tag_array_p = session.allocateArray(C_INT as MemoryLayout, max_nobjects.toLong())
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_nobjects.toLong())
        val nobjects = Vgettagrefs(vgroup_id, tag_array_p, ref_array_p, max_nobjects)
        val ref_array = IntArray(nobjects) { ref_array_p.getAtIndex(C_INT, it.toLong()) }
        val tag_array = IntArray(nobjects) { tag_array_p.getAtIndex(C_INT, it.toLong()) }

        if (debugVGroup) {
            println("  readVGroup $vgroup_ref vclass '$vclass' name='$groupName' nobjects=$nobjects")
            if (debugVGroupDetails) {
                repeat(nobjects) {
                    println("    ref=${ref_array[it]} ${TagEnum.byCode(tag_array[it])} ")
                }
            }
        }

        if (nobjects > 0) {
            if (isNestedGroup(vclass)) {
                VgroupGroup(session, g4, groupName, ref_array, tag_array)
            } else if (isDimClass(vclass)) { // ??
                VgroupDim(session, g4, ref_array, tag_array)
            } else if (vclass == "Var0.0") { // ??
                VgroupVar(session, g4, ref_array, tag_array)
            } else if (vclass == "CDF0.0") {
                // LOOK ignoring the contents of the VGroup, and looking at the attributes on the group
                VgroupCDF(session, g4, vgroup_id)
            }
        }

        val nattrs = Vnattrs(vgroup_id)
        val aname_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val nvalues_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)
        repeat(nattrs) { idx ->
            checkErr("Vattrinfo", Vattrinfo(vgroup_id, idx, aname_p, datatype_p, nvalues_p, size_p))
            val aname = aname_p.getUtf8String(0)
            val datatype = datatype_p[C_INT, 0]
            val nvalues = nvalues_p[C_INT, 0]
            val size = size_p[C_INT, 0]
            if (debugVGroupDetails) println("    readVGroupAttr '$aname' datatype='$datatype' nvalues=$nvalues size =$size")
        }

        checkErr("Vdetach", Vdetach(vgroup_id))
    }

    private fun VgroupGroup(
        session: MemorySession,
        g4: Group4,
        groupName: String,
        ref_array: IntArray,
        tag_array: IntArray
    ) {
        val nested = Group4(groupName, g4)

        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            if (tage == TagEnum.VG) {
                VgroupRead(session, nested, ref)
            } else if (tage == TagEnum.NDG) {
                val sds_index = SDreftoindex(sdsStartId, ref)
                val sd = SDread(session, nested, sds_index)
            } else if (tage == TagEnum.VH) {
                val vs = VStructureRead(session, nested, ref, true)
            }
        }
    }

    private fun VgroupDim(session: MemorySession, g4: Group4, ref_array: IntArray, tag_array: IntArray) {
        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            if (tage == TagEnum.VH) {
                // val vh = readVH()
                // break
            }
        }
    }

    private fun VgroupVar(session: MemorySession, g4: Group4, ref_array: IntArray, tag_array: IntArray) {
        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            if (tage == TagEnum.NDG) {
                val sds_index = SDreftoindex(sdsStartId, ref)
                val sd = SDread(session, g4, sds_index)
                // break
            }
        }
    }

    // this goes through the attr interface instead of reading the VH?
    private fun VgroupCDF(session: MemorySession, g4: Group4, vgroup_id: Int) {
        val nattrs2 = Vnattrs2(vgroup_id)
        repeat(nattrs2) { idx ->
            // val attr = VgroupReadAttribute(session, vgroup_id, idx)
            val attr2 = VgroupReadAttribute2(session, vgroup_id, idx)
            if (debugVGroupDetails) println("     read attribute ${attr2.name}")
            val moveup = attr2.isString && attr2.values.size == 1 && (attr2.values[0] as String).length > 4000
            if (EOS.isMetadata(attr2.name) || moveup) {
                metadata.add(attr2)
                if (attr2.name == "StructMetadata.0") {
                    this.structMetadata = attr2.values[0] as String
                }
            } else {
                if (debugAttributes) println("     add attribute ${attr2}")
                g4.gb.addAttribute(attr2)
            }
        }
    }

    fun VgroupReadAttribute(session: MemorySession, vgroup_id: Int, idx: Int): Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)
        val flds_p = session.allocate(C_INT, 0)
        val refnum_p = session.allocate(C_SHORT, 0)

        val ret = Vattrinfo(vgroup_id, idx, name_p, datatype_p, count_p, size_p)
        if (ret != 0) {
            println("VgroupReadAttribute ret = $ret bailing out")
            return Attribute("what", "the")
        }
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)
        val nelems = count_p[C_INT, 0]
        val size = size_p[C_INT, 0] // LOOK bug where size returns nelems instead
        println("VgroupReadAttribute $aname size = $size nelems = $nelems datatype.size = ${datatype.size}")
        if (aname == "end_latlon") {
            println()
        }

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        // checkErr("Vgetattr2", Vgetattr2(vgroup_id, idx, data_p)) // LOOK malloc(): corrupted top size
        checkErr("Vgetattr", Vgetattr(vgroup_id, idx, data_p)) // Vgetattr return -1
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())

        return processAttribute(aname, nelems, datatype, bb)
    }

    // we can guess that theres only one field in an attribute. so nvalues should be vh.nelems * fld[0].nelems
    // looks like a bug where they are just using nvalues = fld[0].nelems
    fun VgroupReadAttribute2(session: MemorySession, vgroup_id: Int, attr_idx: Int): Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)
        val flds_p = session.allocate(C_INT, 0)
        val refnum_p = session.allocate(C_SHORT, 0)

        checkErr("Vattrinfo2", Vattrinfo2(vgroup_id, attr_idx, name_p, datatype_p, count_p, size_p, flds_p, refnum_p))
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)
        val nvalues = count_p[C_INT, 0]    // LOOK a bug where nvalues returns fld[0].nelems instead of vh.nelems * fld[0].nelems
        val sizeInBytes = size_p[C_INT, 0] // LOOK sometimes a bug where size returns nelems instead
        val nflds = flds_p[C_INT, 0]
        val refnum = refnum_p[C_SHORT, 0]
        if (debugAttributeBug) println("VgroupReadAttribute2 $aname size = $sizeInBytes nvalues = $nvalues datatype.size = ${datatype.size}")
        if (aname == "end_latlon") {
            println()
        }

        // DFTAG_VS on page 143: bytes needed = vh.nelems * SUM( fld_size * fld_order)
        val data_p: MemorySegment = session.allocate(nvalues * datatype.size.toLong())
        checkErr("Vgetattr2", Vgetattr2(vgroup_id, attr_idx, data_p)) // LOOK malloc(): corrupted top size
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())

        return processAttribute(aname, nvalues, datatype, bb)
    }

    //////////////////////////////////////////////////////////////////////////////

    private fun SDiterate(session: MemorySession, g4: Group4): Int {
        if (debugSD) println("iterateSDs '${g4.gb.name}'")

        val nvars_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)
        checkErr("SDfileinfo", SDfileinfo(sdsStartId, nvars_p, nattrs_p))
        val nvars = nvars_p[C_INT, 0]
        val nattrs = nattrs_p[C_INT, 0]

        repeat(nvars) { SDread(session, g4, it) }

        /*
        repeat(nattrs) {
            val attr = readSDattribute(session, sd_id, it)
            if (!EOSmetadata.contains(attr.name)) {
                g4.gb.addAttribute(attr)
            } else {
                this.metadata.add(attr)
                if (attr.name == "StructMetadata.0") {
                    this.structMetadata = attr.values[0] as String
                }
            }
        } */

        return nvars
    }

    @Throws(IOException::class)
    private fun SDread(session: MemorySession, g4: Group4, sdidx: Int) {
        val sd_id = SDselect(sdsStartId, sdidx)
        val sd_ref = SDidtoref(sd_id)

        val tagid = tagid(sd_ref, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugSD) println(" SDread skip $sd_ref")
            return
        }
        completedObjects.add(tagid)

        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val rank_p = session.allocate(C_INT, 0)
        val dims_p = session.allocateArray(C_INT as MemoryLayout, 100)
        val datatype_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)

        // SDgetinfo(sds_id, sds_name, &rank, dim_sizes, &data_type, &n_attrs)
        // SDgetinfo ( int sdsid,  Addressable name,  Addressable rank,  Addressable dimsizes,  Addressable nt,  Addressable nattr) {
        checkErr("SDgetinfo", SDgetinfo(sd_id, name_p, rank_p, dims_p, datatype_p, nattrs_p))

        val sdName = name_p.getUtf8String(0)
        val rank = rank_p[C_INT, 0]
        val dims = IntArray(rank) { dims_p.getAtIndex(ValueLayout.JAVA_INT, it.toLong()) }
        val datatype4 = datatype_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)
        val nattrs = nattrs_p[C_INT, 0]

        if (sdName.startsWith("fakeDim")) {
            SDendaccess(sd_id)
            return
        }

        // SDgetdatastrs: The predefined attributes are label, unit, format, and coordinate system

        // find available dimensions by using the variables
        val dimList = mutableListOf<String>()
        val dimLength_p = session.allocate(C_INT, 0)
        for (dimidx in 0 until rank) {
            val dimid = SDgetdimid(sd_id, dimidx)
            checkErr("SDdiminfo", SDdiminfo(dimid, name_p, dimLength_p, datatype_p, nattrs_p))
            if (debugDims) println("   dimid=$dimid == ${tagidName(dimid)} reverse=${tagidNameR(dimid)}")
            val dimName: String = name_p.getUtf8String(0)
            var dimLength = dimLength_p[C_INT, 0]
            val datatype = datatype_p[C_INT, 0] // wtf?
            val nattrs = nattrs_p[C_INT, 0] // wtf?
            val isUnlimited = (dimLength == 0)
            if (dimLength == 0) {
                dimLength = dims[dimidx]
            }
            if (dimName.startsWith("fakeDim")) { // LOOK
                dimList.add(dimLength.toString())
            } else {
                val useDim = Dimension(dimName, dimLength, false, true)
                g4.gb.addDimensionIfNotExists(useDim)
                dimList.add(useDim.name) // name has been cleaned up
            }
        }
        if (debugSD) println("  readSD '$sdName' ref=$sd_ref datatype=${datatype} dims=${dims.contentToString()} dimList=$dimList")

        // create the Variable
        val vb = Variable.Builder()
        vb.name = sdName
        vb.datatype = datatype
        vb.spObject = Vinfo4().setSDSindex(sdidx)
        vb.dimList = dimList

        // read Variable attributes
        repeat(nattrs) {
            val att = SDreadAttribute(session, sd_id, it)
            if (debugSDdetails) println("    att = ${att.name}")
            vb.addAttribute(att)
        }
        g4.gb.addVariable(vb)

        SDendaccess(sd_id)
    }

    private fun SDreadAttribute(session: MemorySession, sd_id: Int, idx: Int): Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)

        //     public static int SDattrinfo ( int id,  int idx,  Addressable name,  Addressable nt,  Addressable count) {
        checkErr("SDattrinfo", SDattrinfo(sd_id, idx, name_p, datatype_p, count_p))
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val nelems = count_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        checkErr("SDreadattr", SDreadattr(sd_id, idx, data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())

        return processAttribute(aname, nelems, datatype, bb)
    }

    //////////////////////////////////////////////////////////////////////

    // the GR API
    private fun GRiterate(session: MemorySession, g4: Group4) {
        val n_datasets_p = session.allocate(C_INT, 0)
        val n_gattrs_p = session.allocate(C_INT, 0)

        // GRfileinfo ( int grid,  Addressable n_datasets,  Addressable n_attrs)
        val ret = GRfileinfo(grStartId, n_datasets_p, n_gattrs_p)
        if (ret != 0) {
            return
        }
        val n_datasets = n_datasets_p[C_INT, 0]
        val n_gattrs = n_gattrs_p[C_INT, 0]

        if (debugGR) println("GRiterator $n_datasets attrs = $n_gattrs")
        repeat(n_datasets) { idx -> GRVariable(session, g4, idx) }
    }

    private fun GRVariable(session: MemorySession, g4: Group4, gridx: Int) {
        val grId = GRselect(grStartId, gridx)
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val n_comps_p = session.allocate(C_INT, 0) // Number of pixel components in the pixel
        val data_type_p = session.allocate(C_INT, 0) // Pixel data type
        val interlace_p = session.allocate(C_INT, 0) // Interlace mode of the data in the raster image
        val dim_sizes_p: MemorySegment = session.allocateArray(C_INT as MemoryLayout, 2)
        val n_attrs_p = session.allocate(C_INT, 0)

        checkErr("GRgetiminfo", GRgetiminfo(grId, name_p, n_comps_p, data_type_p, interlace_p, dim_sizes_p, n_attrs_p))

        val name = name_p.getUtf8String(0)
        val n_comps = n_comps_p[C_INT, 0]
        val datatype = data_type_p[C_INT, 0]
        val interlace = interlace_p[C_INT, 0]
        val dims = IntArray(2) { dim_sizes_p.getAtIndex(C_INT, it.toLong()) }
        val nattrs = n_attrs_p[C_INT, 0]

        // create the Variable
        val vb = Variable.Builder()
        vb.name = name
        vb.datatype = H4type.getDataType(datatype)
        vb.spObject = Vinfo4().setGRindex(gridx)

        // For GRreadimage, those parameters are expressed in (x,y) or [column,row] order. p 321
        val rdims = IntArray(2) { if (it == 0) dims[1] else dims[0] }
        vb.setDimensionsAnonymous(rdims)

        // read Variable attributes // look probably need GRattrinfo
        // For GRreadimage, those parameters are expressed in (x,y) or [column,row] order. For
        repeat(nattrs) { vb.addAttribute(GRreadAttribute(session, grId, it)) }
        g4.gb.addVariable(vb)

        // LOOK probably need GRgetlutinfo(), GRgetnluts

        if (debugGR) println("  GRgetiminfo '$name', n_comps=$n_comps, type=${vb.datatype}, interlace=$interlace, dims${dims.contentToString()}, attrs=$nattrs")
        GRendaccess(grId)
    }

    private fun GRreadAttribute(session: MemorySession, gr_id: Int, idx: Int): Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)

        //     public static int SDattrinfo ( int id,  int idx,  Addressable name,  Addressable nt,  Addressable count) {
        checkErr("GRattrinfo", GRattrinfo(gr_id, idx, name_p, datatype_p, count_p))
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val nelems = count_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        checkErr("GRgetattr", GRgetattr(gr_id, idx, data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())

        return processAttribute(aname, nelems, datatype, bb)
    }

    //////////////////////////////////////////////////////////////////////////////

    private fun makeGroup(
        session: MemorySession,
        parent: Group4,
        groupName: String,
        vgroup_id: Int,
        ref_array: IntArray,
        tag_array: IntArray
    ) {
        require(ref_array.size == tag_array.size)
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)

        val nested4 = Group4(groupName, parent)

        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            if (debugVGroup) println("   object $objIdx tag = '$tage' ref=$ref")
            if (tage == TagEnum.VG) {
                VgroupRead(session, nested4, ref)
            } //else if (tage == TagEnum.VH) {
            //   makeVStructure(session, nested4, ref)
            // } //else if (tage == TagEnum.NDG) {
            ///  readNDG(session, nested4, ref)
            //}
        }

        val nattrs = Vnattrs(vgroup_id)

        val aname_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val nvalues_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)
        repeat(nattrs) { idx ->
            checkErr("Vattrinfo", Vattrinfo(vgroup_id, idx, aname_p, datatype_p, nvalues_p, size_p))
            val aname = aname_p.getUtf8String(0)
            val datatype = datatype_p[C_INT, 0]
            val nvalues = nvalues_p[C_INT, 0]
            val size = size_p[C_INT, 0]
            if (debugVGroup) println("    readVGroupAttr '$aname' datatype='$datatype' nvalues=$nvalues size =$size")
        }
    }

    /*
    private fun addGlobalAttributes(session: MemorySession, g4: Group4, vdata_id: Int) {
        // look for attributes
        for (i in 0 until group.nelems) {
            val tag: Tag = tagidMap[H4builder.tagid(group.elem_ref.get(i), group.elem_tag.get(i))] ?: throw IllegalStateException()
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
*/

    //////////////////////////////////////////////////////////////////////////////


    // Vdata interface (also called the VS interface or the VS API). Lets call it VStructure
    // A vdata is a collection of records whose values are stored in fixed-length fields.

    // sequentially iterates through an HDF file to obtain the vdata
    private fun VStructureIterate(session: MemorySession, g4: Group4) {
        if (debugVSdata) println("iterateVSdata '${g4.gb.name}'")
        var last_ref = -1
        while (true) {
            val vdata_ref = VSgetid(fileOpenId, last_ref)
            if (vdata_ref == -1) {
                break
            }
            VStructureRead(session, g4, vdata_ref, false)
            // makeVStructure(session, g4, vdata_ref)

            /*
            val vdata_id = VSattach(fileid, vdata_ref, read_access_mode);
            val vclass_p: MemorySegment = session.allocate(MAX_NAME)
            checkErr("VSgetclass", VSgetclass(vdata_id, vclass_p))
            val vclass = vclass_p.getUtf8String(0)

            if (VSisattr(vdata_id) == 0) { // not an attribute
                if (debugVdata) println("  Vdata vclass = $vclass vdata_ref = $vdata_ref vdata_id = $vdata_id")
                makeVStructure(session, g4, vdata_id)
            } else {
                if (debugVdata) println("  VSisattr vclass = $vclass vdata_ref = $vdata_ref vdata_id = $vdata_id")
            }
            checkErr("VSdetach", VSdetach(vdata_id))
            */
            last_ref = vdata_ref
        }

        /* retrieves the reference numbers of lone vdatas in the file
        if (debugVSdata) println("iterateVSlone '${g4.gb.name}'")
        val max_refs = 128
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_refs.toLong())
        val num_of_lone_vdatas = VSlone(fileOpenId, ref_array_p, max_refs);
        val ref_array = IntArray(num_of_lone_vdatas) { ref_array_p.getAtIndex(C_INT, it.toLong()) }

        for (idx in 0 until num_of_lone_vdatas) {
            VStructureRead(session, g4, ref_array[idx], false)
        } */
    }

    private fun VStructureRead(session: MemorySession, g4: Group4, vs_ref: Int, addAttributesToGroup: Boolean) {
        val tagid = tagid(vs_ref, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugVSdata) println(" VStructureRead skip $vs_ref")
            return
        }
        completedObjects.add(tagid)

        val vdata_id = VSattach(fileOpenId, vs_ref, read_access_mode);

        val vclass_p: MemorySegment = session.allocate(MAX_NAME)
        checkErr("VSgetclass", VSgetclass(vdata_id, vclass_p))
        val vclass = vclass_p.getUtf8String(0)

        val n_records_p = session.allocate(C_INT, 0)
        val interlace_p = session.allocateArray(C_INT as MemoryLayout, 100)
        val fieldnames_p: MemorySegment = session.allocate(MAX_FIELDS_NAME)
        val recsize_p = session.allocate(C_INT, 0) // size, in bytes, of the vdata record
        val vsname_p: MemorySegment = session.allocate(MAX_NAME)

        // VSinquire(vdata_id, &n_records, &interlace_mode, fieldname_list, &vdata_size, vdata_name)
        checkErr("VSinquire", VSinquire(vdata_id, n_records_p, interlace_p, fieldnames_p, recsize_p, vsname_p))
        val nrecords = n_records_p[C_INT, 0]
        val interlace = interlace_p[C_INT, 0]
        val fieldnames = fieldnames_p.getUtf8String(0)
        val recsize = recsize_p[C_INT, 0]
        val vsname = vsname_p.getUtf8String(0)
        if (g4.gb.variables.find { it.name == vsname } != null) return

        if (debugVSdata) {
            println("  readVStructure '$vsname' ref=$vs_ref class = '$vclass' nrecords=$nrecords fieldnames='$fieldnames' recsize=$recsize")
        }

        val vb = Variable.Builder()
        vb.name = vsname
        if (g4.gb.variables.find { it.name == vsname } != null) return // LOOK why needed?

        val index_p = session.allocate(C_INT, 0)
        val names = fieldnames.split(",").map { it.trim() }
        val members = mutableListOf<StructureMember>()
        var offset = 0
        for (name in names) {
            checkErr("VSfindex", VSfindex(vdata_id, session.allocateUtf8String(name), index_p))
            val idx = index_p[C_INT, 0]
            val type = VFfieldtype(vdata_id, idx)
            val fdatatype = H4type.getDataType(type)
            val esize = VFfieldesize(vdata_id, idx)
            val isize = VFfieldisize(vdata_id, idx) // native machine size of the field.
            val nelems = VFfieldorder(vdata_id, idx) // field "order" ??
            if (debugVSdetails) println("    VSfield name='$name' fdatatype=$fdatatype offset='$offset' nelems=$nelems esize =$esize isize = $isize")
            val m = StructureMember(name, fdatatype, offset, intArrayOf(nelems))
            members.add(m)
            offset += isize
        }

        if (members.size == 1) {
            val member = members[0]
            vb.datatype = member.datatype
            val totalNelems  = nrecords * member.nelems
            if (totalNelems > 1) {
                vb.setDimensionsAnonymous(intArrayOf(totalNelems))
            }
        } else {
            val typedef = CompoundTypedef(vsname, members)
            vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
            if (nrecords > 1) {
                vb.setDimensionsAnonymous(intArrayOf(nrecords))
            }
        }

        val vinfo = Vinfo4()
        vinfo.vsInfo = VSInfo(vs_ref, nrecords, recsize, fieldnames)
        vb.spObject = vinfo

        // LOOK no attributes
        val nattrs = VSnattrs(vdata_id)
        repeat(nattrs) {
            val attr = VStructureReadAttribute(session, vdata_id, -1, it)
            // println("VStructureReadAttribute ${attr.name}")
            // vb.addAttribute( attr)
        }

        if (vclass.startsWith("Attr")) {
            if (EOS.isMetadata(vsname)) {
                //val attr = convertToAttribute(vb)
                //metadata.add(attr)
            }
            if (addAttributesToGroup) {
                val attr = VStructureMakeAttribute(vb.name!!, vb.datatype!!, vdata_id, vinfo.vsInfo!!)
                g4.gb.addAttribute(attr)
            }
        } else if (vclass.startsWith("DimVal") || vclass.startsWith("_HDF_CHK_TBL")) {
            // noop
        } else {
            if (vb.datatype!!.cdlName == "compound" ) {
                rootGroup4.gb.addTypedef(vb.datatype!!.typedef!!)
            }
            g4.gb.addVariable(vb)
        }

        checkErr("VSdetach", VSdetach(vdata_id))
    }

    fun VStructureMakeAttribute(aname : String, datatype: Datatype, vdata_id : Int, vsInfo : VSInfo): Attribute {
        val nbytes = vsInfo.nrecords * vsInfo.recsize
        MemorySession.openConfined().use { session ->
            val data_p = session.allocate(nbytes.toLong())
            val fldNames: MemorySegment = session.allocateUtf8String(vsInfo.fldNames)

            checkErrNeg("VSsetfields", VSsetfields(vdata_id, fldNames))
            checkErrNeg("VSread", VSread(vdata_id, data_p, vsInfo.nrecords, FULL_INTERLACE()))

            val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
            val bb = ByteBuffer.wrap(raw)
            bb.order(ByteOrder.LITTLE_ENDIAN) // ??
            bb.position(0)
            bb.limit(bb.capacity())

            return processAttribute(aname, vsInfo.nrecords, datatype, bb)
        }
    }

    // LOOK structure members can have attributes (!) with fld_idx
    fun VStructureReadAttribute(session: MemorySession, vdata_id: Int, fld_idx: Int, idx: Int): Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)

        checkErr("VSattrinfo", VSattrinfo(vdata_id, fld_idx, idx, name_p, datatype_p, count_p, size_p))
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)
        val nelems = count_p[C_INT, 0]
        val size = size_p[C_INT, 0]

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        checkErr("VSgetattr", VSgetattr(vdata_id, fld_idx, idx, data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())

        return processAttribute(aname, nelems, datatype, bb)
    }

}

private fun processAttribute(name : String, nelems : Int, datatype : Datatype, bb : ByteBuffer) : Attribute {
    val shape = intArrayOf(nelems)

    if (datatype == Datatype.CHAR) {
        val svalue = makeStringZ(bb.array(), Hdf4ClibFile.valueCharset)
        return Attribute(name, svalue)
    }

    val values = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    return Attribute(name, datatype, values.toList())
}

private fun makeVariableFromStringAttribute(g4 : Group4, att : Attribute) {
    require(att.isString)
    val svalue = att.values[0] as String
    // create the Variable
    val vb = Variable.Builder()
    vb.name = att.name
    vb.datatype = Datatype.STRING
    vb.spObject = Vinfo4().setSValue(svalue)
    g4.gb.addVariable(vb)
}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret")
    }
}

fun checkErrNeg (where : String, ret: Int) {
    if (ret < 0) {
        throw IOException("$where return $ret")
    }
}

data class Group4(val name : String, val parent: Group4?) {
    var gb = Group.Builder(name)
    init {
        parent?.gb?.addGroup(gb)
    }
}

data class VSInfo(val vs_ref : Int, val nrecords : Int, val recsize : Int, val fldNames : String)

class Vinfo4() {
    var svalue : String? = null
    var sdsIndex : Int? = null
    var grIndex : Int? = null
    var vsInfo : VSInfo? = null

    fun setSDSindex(sdsIndex : Int) : Vinfo4 {
        this.sdsIndex = sdsIndex
        return this
    }

    fun setGRindex(grIndex : Int) : Vinfo4 {
        this.grIndex = grIndex
        return this
    }

    fun setSValue(svalue : String) : Vinfo4 {
        this.svalue = svalue
        return this
    }
}

