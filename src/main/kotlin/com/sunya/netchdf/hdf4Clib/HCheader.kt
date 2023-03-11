package com.sunya.netchdf.hdf4Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.netchdf.hdf4.H4type
import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.io.IOException
import java.lang.foreign.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private val debug = true
private val debugVS = true
private val MAX_NAME = 255L
private val MAX_FIELDS_NAME = 1000L

// Really a builder of the root Group.
class HCheader(val filename: String) {
    val rootGroup = Group.Builder("")
    var formatType: String = ""
    val completedVdata = mutableSetOf<Int>()

    internal var sd_id = 0
    internal var fileid = 0
    internal var gr_id = 0

    init {
        MemorySession.openConfined().use { session ->
            build(session)
        }
    }

    @Throws(IOException::class)
    private fun build(session: MemorySession) {
        val filenameSeg: MemorySegment = session.allocateUtf8String(filename)

        /* Initialize the SD interface. */
        this.sd_id = SDstart(filenameSeg, DFACC_READ())

        /* Initialize the rest of the API. */
        this.fileid = Hopen(filenameSeg, DFACC_READ(), 0)
        checkErr("Vinitialize", Vinitialize(this.fileid))
        this.gr_id = GRstart(this.fileid)

        val root4 = Group4(rootGroup, null)

        readSDGroup(session, root4, this.sd_id)
        readVSGroup(session, root4, this.fileid)
        readVGroup(session, root4, this.fileid)
        readImages(session, root4, this.gr_id)
    }

    fun close() {
        val sdret = SDend(this.sd_id)
        System.out.printf("SDend ret=%d %n", sdret)
        val gret = GRend(this.gr_id)
        System.out.printf("GRend ret=%d %n", gret)
        val vsret = Vfinish(this.fileid)
        System.out.printf("Vfinish ret=%d %n", vsret)
        val ret = Hclose(this.fileid)
        System.out.printf("Hclose ret=%d %n", ret)
    }

    // the GR API
    private fun readImages(session: MemorySession, g4: Group4, gr_id : Int) {
        val n_datasets_p = session.allocate(C_INT, 0)
        val n_gattrs_p = session.allocate(C_INT, 0)

        // GRfileinfo ( int grid,  Addressable n_datasets,  Addressable n_attrs)
        checkErr("GRfileinfo", GRfileinfo(gr_id, n_datasets_p, n_gattrs_p))
        val n_datasets = n_datasets_p[C_INT, 0]
        val n_gattrs = n_gattrs_p[C_INT, 0]

        if (debug) println("readImages $n_datasets attrs = $n_gattrs")
        repeat(n_datasets) { idx -> makeGRVariable(session, g4, idx) }
    }

    private fun makeGRVariable(session: MemorySession, g4: Group4, gridx : Int) {
        val ri_id = GRselect(gr_id, gridx)
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val n_comps_p = session.allocate(C_INT, 0) // Number of pixel components in the pixel
        val data_type_p = session.allocate(C_INT, 0) // Pixel data type
        val interlace_p = session.allocate(C_INT, 0) // Interlace mode of the data in the raster image
        val dim_sizes_p: MemorySegment = session.allocateArray(C_INT as MemoryLayout, 2)
        val n_attrs_p = session.allocate(C_INT, 0)

        checkErr("GRgetiminfo", GRgetiminfo(ri_id, name_p, n_comps_p, data_type_p, interlace_p, dim_sizes_p, n_attrs_p))

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
        vb.spObject = Vinfo4(g4, gridx, datatype)

        // For GRreadimage, those parameters are expressed in (x,y) or [column,row] order. p 321
        val rdims = IntArray(2) { if (it == 0) dims[1] else dims[0] }
        vb.setDimensionsAnonymous(rdims)

        // read Variable attributes // look probably need GRattrinfo
        // For GRreadimage, those parameters are expressed in (x,y) or [column,row] order. For
        repeat(nattrs) { vb.attributes.add( readAttribute(session, ri_id, it)) }
        g4.gb.addVariable(vb)

        // LOOK probably need GRgetlutinfo(), GRgetnluts

        if (debug) println("  GRgetiminfo '$name', n_comps=$n_comps, type=${vb.datatype}, interlace=$interlace, dims${dims.contentToString()}, attrs=$nattrs")
        GRendaccess(ri_id)
    }

    // The Vgroup API: group related objects. DFTAG_VG.
    // "vgroups can contain any combination of HDF data objects". so that narrows it down.
    private fun readVGroup(session: MemorySession, g4: Group4, fileid : Int) {
        if (debug) println("readVGroup '${g4.gb.name}'")
        val vdata_access_mode: MemorySegment = session.allocateUtf8String("r")

        // sequentially searches through an HDF file to obtain the vdata
        if (debug) println(" Vgetid iteration")
        var last_ref = -1
        while (true) {
            val vdata_ref = Vgetid(fileid, last_ref)
            if (vdata_ref == -1) {
                break
            }

            val vdata_id = Vattach(fileid, vdata_ref, vdata_access_mode);
            val vclass_p: MemorySegment = session.allocate(MAX_NAME)
            checkErr("Vgetclass", Vgetclass(vdata_id, vclass_p))
            val vclass = vclass_p.getUtf8String(0)
            println(" vclass = $vclass vdata_ref = $vdata_ref vdata_id = $vdata_id")
            makeVvariable(session, g4, vdata_id)

            checkErr("Vdetach", Vdetach(vdata_id))
            last_ref = vdata_ref
        }

        // retrieves the reference numbers of lone vdatas in the file
        if (debug) println(" Vlone")
        val max_refs = 128
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_refs.toLong())
        val num_of_lone_vdatas = Vlone(fileid, ref_array_p, max_refs);
        val ref_array = IntArray(num_of_lone_vdatas) { ref_array_p.getAtIndex(C_INT, it.toLong()) }

        for (idx in 0 until num_of_lone_vdatas) {
            val vdata_id = VSattach(fileid, ref_array[idx], vdata_access_mode)
            if (vdata_id == -1) break
            makeVvariable(session, g4, vdata_id)
            checkErr("Vdetach", Vdetach(vdata_id))
        }
    }

    private fun makeVvariable(session: MemorySession, g4: Group4, vdata_id: Int) : Boolean {
        val n_members_p = session.allocate(C_INT, 0)
        val name_p: MemorySegment = session.allocate(MAX_NAME)

        val ret = Vinquire(vdata_id, n_members_p, name_p)
        if (ret != 0) {
            println("Vinquire $ret")
            return false
        }
        val n_members = n_members_p[C_INT, 0]
        val name = name_p.getUtf8String(0)

        /*
        val vb = Variable.Builder()
        vb.name = vsname
        vb.datatype = H4type.getDataType(datatype)
        vb.spObject = Vinfo4(g4, varidx, datatype)
        vb.dimList = dimList

        // read Variable attributes
        repeat(nattrs) { vb.attributes.add( readAttribute(session, vdata_id, it)) }
        g4.gb.addVariable(vb)

         */

        println("  readVvariable name='$name' n_members=$n_members")
        return true
    }

    // Vdata interface (also called the VS interface or the VS API). Lets call it VStructure
    // A vdata is a collection of records whose values are stored in fixed-length fields.
    private fun readVSGroup(session: MemorySession, g4: Group4, fileid : Int) {
        if (debug) println("readVSGroup '${g4.gb.name}'")
        val vdata_access_mode: MemorySegment = session.allocateUtf8String("r")

        // sequentially searches through an HDF file to obtain the vdata
        if (debug) println(" VSgetid iteration")
        var last_ref = -1
        while (true) {
            val vdata_ref = VSgetid(fileid, last_ref)
            if (vdata_ref == -1) {
                break
            }

            val vdata_id = VSattach(fileid, vdata_ref, vdata_access_mode);
            val vclass_p: MemorySegment = session.allocate(MAX_NAME)
            checkErr("VSgetclass", VSgetclass(vdata_id, vclass_p))
            val vclass = vclass_p.getUtf8String(0)

            if (VSisattr(vdata_id) == 0) { // not an attribute
                makeVStructure(session, g4, vdata_id)
            } else {
                println("  VSisattr vclass = $vclass vdata_ref = $vdata_ref vdata_id = $vdata_id")
                makeVStructure(session, g4, vdata_id)
            }

            checkErr("VSdetach", VSdetach(vdata_id))
            last_ref = vdata_ref
        }

        // retrieves the reference numbers of lone vdatas in the file
        if (debug) println(" VSlone")
        val max_refs = 128
        val ref_array_p = session.allocateArray(C_INT, max_refs)
        val num_of_lone_vdatas = VSlone(fileid, ref_array_p, max_refs);
        val ref_array = IntArray(num_of_lone_vdatas) { ref_array_p.getAtIndex(C_INT, it.toLong()) }

        for (idx in 0 until num_of_lone_vdatas) {
            val vdata_id = VSattach(fileid, ref_array[idx], vdata_access_mode);
            makeVStructure(session, g4, vdata_id)
            checkErr("VSdetach", VSdetach(vdata_id))
        }
    }

    private fun makeVStructure(session: MemorySession, g4: Group4, vdata_id: Int) {
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
        if (g4.gb.variables.find {it.name == vsname } != null) return

        if (debug) println("  readVSvariable name='$vsname' nrecords=$nrecords fieldnames='$fieldnames' recsize=$recsize")
        val vb = Variable.Builder()
        vb.name = vsname
        if (g4.gb.variables.find {it.name == vsname } != null) return

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
            if (debugVS) println("   VSfield name='$name' fdatatype=$fdatatype offset='$offset' nelems=$nelems esize =$esize isize = $isize")
            val m = StructureMember(name, fdatatype, offset, intArrayOf(nelems))
            members.add(m)
            offset += isize
        }

        // LOOK need Vinfo
        val typedef = CompoundTypedef(vsname, members)
        vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
        vb.setDimensionsAnonymous(intArrayOf(nrecords))
        g4.gb.typedefs.add(typedef)

        // use VSnattrs()
        // repeat(nattrs) { vb.attributes.add( readAttribute(session, vdata_id, it)) }
        g4.gb.addVariable(vb)
    }

    private fun readSDGroup(session: MemorySession, g4: Group4, sd_id : Int) {
        if (debug) println("readSDGroup '${g4.gb.name}'")

        val nvars_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)
        checkErr("SDfileinfo", SDfileinfo(sd_id, nvars_p, nattrs_p))
        val nvars = nvars_p[C_INT, 0]
        val nattrs = nattrs_p[C_INT, 0]

        repeat(nvars) { readSDVariable(session, g4, sd_id, it) }

        repeat(nattrs) {
            val attr = readAttribute(session, sd_id, it)
            if (attr.name != "ProductMetadata.0") { // jeesh
                g4.gb.attributes.add(readAttribute(session, sd_id, it))
            }
        }

        /* subgroups
        val numGrps_p = session.allocate(C_INT, 0)
        val MAX_GROUPS = 1000L // bad API
        val grpids_p = session.allocateArray(C_INT, MAX_GROUPS)
        checkErr("nc_inq_grps", nc_inq_grps(g4.grpid, numGrps_p, grpids_p))
        val numGrps = numGrps_p[C_INT, 0]
        if (numGrps == 0) {
            return
        }

        for (idx in 0 until numGrps) {
            val grpid = grpids_p.getAtIndex(ValueLayout.JAVA_INT, idx.toLong())
            val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
            checkErr("nc_inq_grpname", nc_inq_grpname(grpid, name_p))
            val name = name_p.getUtf8String(0)
            readGroup(session, Group4(grpid, Group.Builder(name), g4))
        } */
    }

    @Throws(IOException::class)
    private fun readSDVariable(session: MemorySession, g4: Group4, sd_id : Int, varidx: Int) {

        val varid = SDselect(sd_id, varidx)

        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val rank_p = session.allocate(C_INT, 0)
        val dims_p = session.allocateArray(C_INT as MemoryLayout, 100)
        val datatype_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)

        // SDgetinfo(sds_id, sds_name, &rank, dim_sizes, &data_type, &n_attrs)
        // SDgetinfo ( int sdsid,  Addressable name,  Addressable rank,  Addressable dimsizes,  Addressable nt,  Addressable nattr) {
        checkErr("SDgetinfo", SDgetinfo(varid, name_p, rank_p, dims_p, datatype_p, nattrs_p))

        val vname: String = name_p.getUtf8String(0)
        val rank = rank_p[C_INT, 0]
        val dims = IntArray(rank) { dims_p.getAtIndex(ValueLayout.JAVA_INT, it.toLong()) }
        val datatype = datatype_p[C_INT, 0]
        val nattrs = nattrs_p[C_INT, 0]

        // create the Variable
        val vb = Variable.Builder()
        vb.name = vname
        vb.datatype = H4type.getDataType(datatype)
        vb.spObject = Vinfo4(g4, varidx, datatype)

        // find available dimensions by using the variables
        val dimList = mutableListOf<String>()
        val dimLength_p = session.allocate(C_INT, 0)
        for (dimidx in 0 until rank) {
            val dimid = SDgetdimid(varid, dimidx)
            checkErr ("SDdiminfo", SDdiminfo(dimid, name_p, dimLength_p, datatype_p, nattrs_p))
            val dimName: String = name_p.getUtf8String(0)
            var dimLength = dimLength_p[C_INT, 0]
            val datatype = datatype_p[C_INT, 0] // wtf?
            val nattrs = nattrs_p[C_INT, 0] // wtf?
            val isUnlimited = (dimLength == 0)
            if (isUnlimited) {
                dimLength = dims[dimidx]
            }
            g4.gb.addDimensionIfNotExists(Dimension(dimName, dimLength, isUnlimited, true))
            dimList.add(dimName)
        }
        vb.dimList = dimList

        // read Variable attributes
        repeat(nattrs) { vb.attributes.add( readAttribute(session, varid, it)) }
        g4.gb.addVariable(vb)

        SDendaccess(varid)
        println("  readSD vname='$vname' datatype=${vb.datatype} dims=${dims.contentToString()}")
    }

    private fun readAttribute(session: MemorySession, sd_id : Int, idx : Int) : Attribute {
        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val count_p = session.allocate(C_INT, 0)

        //     public static int SDattrinfo ( int id,  int idx,  Addressable name,  Addressable nt,  Addressable count) {
        checkErr("SDattrinfo", SDattrinfo(sd_id, idx, name_p, datatype_p, count_p))
        val aname: String = name_p.getUtf8String(0)
        val datatype4 = datatype_p[C_INT, 0]
        val nelems = count_p[C_INT, 0]
        val datatype = H4type.getDataType(datatype4)
        //if (aname == "long_name")
        //    println()

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        checkErr ("SDreadattr", SDreadattr(sd_id, idx, data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())
        val shape = intArrayOf(nelems)

        if (datatype == Datatype.CHAR) {
            val svalue = String(bb.array())
            return Attribute(aname, svalue)
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
        return Attribute(aname, datatype, values.toList())
    }
}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret")
    }
}

data class Group4(val gb: Group.Builder, val parent: Group4?)

data class Vinfo4(val g4: Group4, val sds_index: Int, val datatype : Int)

