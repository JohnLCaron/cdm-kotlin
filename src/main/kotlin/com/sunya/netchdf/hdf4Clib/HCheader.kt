package com.sunya.netchdf.hdf4Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.makeStringZ
import com.sunya.netchdf.hdf4.*
import com.sunya.netchdf.hdf4.H4builder.Companion.tagid
import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.io.IOException
import java.lang.foreign.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private val debugVGroup = true
private val debugVdata = true
private val debugSD = true
private val debugVS = false
private val debugGR = true
private val debug = false

private val MAX_NAME = 255L
private val MAX_FIELDS_NAME = 1000L

// Really a builder of the root Group.
class HCheader(val filename: String) {
    val rootGroup4 = Group4("", null)
    val rootGroup : Group
    val read_access_mode : MemorySegment

    private val completedObjects = mutableSetOf<Int>()
    internal var sds_id = 0
    private var fileid = 0
    private var grs_id = 0
    private val metadata = mutableListOf<Attribute>()
    private var structMetadata : String? = null

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
        this.sds_id = SDstart(filenameSeg, DFACC_READ())

        /* Initialize the rest of the API. */
        this.fileid = Hopen(filenameSeg, DFACC_READ(), 0)
        checkErr("Vinitialize", Vinitialize(this.fileid))
        this.grs_id = GRstart(this.fileid)

        val nsd = iterateSDs(session, rootGroup4, this.sds_id)
        println("There are $nsd SDs")
        if (nsd == 0) {
            iterateVgroups(session, rootGroup4, this.fileid)
            iterateVdata(session, rootGroup4, this.fileid)
        }
        iterateGRs(session, rootGroup4, this.grs_id)

        // hdfeos
        if (structMetadata != null) {
            rootGroup4.gb = applyStructMetadata(structMetadata!!)
        }
    }

    fun close() {
        val sdret = SDend(this.sds_id)
        System.out.printf("SDend ret=%d %n", sdret)
        val gret = GRend(this.grs_id)
        System.out.printf("GRend ret=%d %n", gret)
        val vsret = Vfinish(this.fileid)
        System.out.printf("Vfinish ret=%d %n", vsret)
        val ret = Hclose(this.fileid)
        System.out.printf("Hclose ret=%d %n", ret)
    }

    //////////////////////////////////////////////////////////////////////////////

    // The Vgroup API: group related objects. DFTAG_VG.
    // "vgroups can contain any combination of HDF data objects". so that narrows it down.

    // Iterates over Vgroups (1965)
    private fun iterateVgroups(session: MemorySession, g4: Group4, fileid : Int) {
        if (debugVGroup) println("iterateVgroups '${g4.gb.name}'")

        var last_ref = -1
        while (true) {
            val vgroup_ref = Vgetid(fileid, last_ref)
            if (vgroup_ref == -1) {
                break
            }
            readVGroup(session, g4, vgroup_ref)
            last_ref = vgroup_ref
        }

        // LOOK is there anything in here we need?
        /* retrieves the reference numbers of lone vdatas in the file
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
         */
    }

    // read a VGroup (1965)
    private fun readVGroup(session: MemorySession, g4: Group4, vgroup_ref: Int) {
        val tagid = tagid(vgroup_ref, TagEnum.VG.code)
        if (completedObjects.contains(tagid)) {
            if (debugVGroup) println(" Vgroup skip $vgroup_ref")
            return
        }

        val vgroup_id = Vattach(fileid, vgroup_ref, read_access_mode)

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
        if (debugVGroup) println("  readVGroup $vgroup_ref vclass '$vclass' name='$groupName' nobjects=$max_nobjects")

        val tag_array_p = session.allocateArray(C_INT as MemoryLayout, max_nobjects.toLong())
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_nobjects.toLong())
        val nobjects = Vgettagrefs(vgroup_id, tag_array_p, ref_array_p, max_nobjects)
        val ref_array = IntArray(nobjects) { ref_array_p.getAtIndex(C_INT, it.toLong()) }
        val tag_array = IntArray(nobjects) { tag_array_p.getAtIndex(C_INT, it.toLong()) }

        if (vclass.startsWith("Dim")) {
            makeDim(session, g4, groupName, vgroup_id, ref_array, tag_array)
        } else if (vclass.startsWith("Var")) { // ??
            makeVar(session, g4, groupName, vgroup_id, ref_array, tag_array)
        } else if (vclass.startsWith("CDF0.0") or vclass.startsWith("RIG0.0")) {
            // skip
        } else {
            makeGroup(session, g4, groupName, vgroup_id, ref_array, tag_array)
        }

        checkErr("Vdetach", Vdetach(vgroup_id))
        // completedObjects.add(tagid)
    }

    private fun makeGroup(session: MemorySession, parent: Group4, groupName : String, vgroup_id : Int, ref_array : IntArray, tag_array : IntArray) {
        require(ref_array.size == tag_array.size)
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)

        val nested4 = Group4(groupName, parent)

        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            if (debugVGroup) println("   object $objIdx tag = '$tage' ref=$ref")
            if (tage == TagEnum.VG) {
                readVGroup(session, nested4, ref)
            } //else if (tage == TagEnum.VH) {
             //   makeVStructure(session, nested4, ref)
            // } //else if (tage == TagEnum.NDG) {
              ///  readNDG(session, nested4, ref)
            //}
        }

        val nattrs = Vnattrs (vgroup_id)

        val aname_p: MemorySegment = session.allocate(MAX_NAME)
        val datatype_p = session.allocate(C_INT, 0)
        val nvalues_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_INT, 0)
        repeat(nattrs) {idx ->
            checkErr("Vattrinfo", Vattrinfo (vgroup_id, idx, aname_p, datatype_p, nvalues_p, size_p))
            val aname = aname_p.getUtf8String(0)
            val datatype = datatype_p[C_INT, 0]
            val nvalues = nvalues_p[C_INT, 0]
            val size = size_p[C_INT, 0]
            if (debugVGroup) println("    readVGroupAttr '$aname' datatype='$datatype' nvalues=$nvalues size =$size")
        }
    }

    private fun readNDG(session: MemorySession, parent: Group4, refNDG : Int) {
        val vdata_id = VSattach(fileid, refNDG, read_access_mode);

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

        if (debug) println("    readNDG name='$vsname' class = $vclass nrecords=$nrecords fieldnames='$fieldnames' recsize=$recsize")
    }

    private fun makeVar(session: MemorySession, parent: Group4, groupName : String, vgroup_id : Int, ref_array : IntArray, tag_array : IntArray) {
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)
        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            val vdata_id = Vattach(fileid, ref, read_access_mode)
            if (vdata_id >= 0) {
                checkErr("Vgetclass", Vgetclass(vdata_id, vclass_p))
                val vclassn = vclass_p.getUtf8String(0)
                if (debug) println("    makeVar object $objIdx tag = '$tage' class='$vclassn' ref = $ref vdata_id=$vdata_id")
                checkErr("VSdetach", Vdetach(vdata_id))
            } else {
                if (debug) println("      object $objIdx tag = '$tage' ref = $ref vdata_id=$vdata_id")
            }
        }
    }

    private fun makeDim(session: MemorySession, parent: Group4, groupName : String, vgroup_id : Int, ref_array : IntArray, tag_array : IntArray) {
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)
        repeat(ref_array.size) { objIdx ->
            val tag = tag_array[objIdx]
            val ref = ref_array[objIdx]
            val tage = TagEnum.byCode(tag)
            val vdata_id = Vattach(fileid, ref, read_access_mode)
            if (vdata_id >= 0) {
                checkErr("Vgetclass", Vgetclass(vdata_id, vclass_p))
                val vclassn = vclass_p.getUtf8String(0)
                if (debug) println("    makeDim object $objIdx tag = '$tage' class='$vclassn' ref = $ref vdata_id=$vdata_id")
                checkErr("VSdetach", Vdetach(vdata_id))
            } else {
                if (debug) println("      object $objIdx tag = '$tage' ref = $ref vdata_id=$vdata_id")
            }
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

    private fun makeVHvariable(session: MemorySession, g4: Group4, vName : String, vdata_id: Int) : Boolean {
        val vclass_p: MemorySegment = session.allocate(MAX_NAME)
        checkErr("Vgetclass", Vgetclass(vdata_id, vclass_p))
        val vclass = vclass_p.getUtf8String(0)

        val n_members_p = session.allocate(C_INT, 0)
        val name_p: MemorySegment = session.allocate(MAX_NAME)

        val ret = Vinquire(vdata_id, n_members_p, name_p)
        if (ret != 0) {
            println("Vinquire $ret")
            return false
        }
        val n_members = n_members_p[C_INT, 0]
        val name = name_p.getUtf8String(0)

        if (debug) println("  readVvariable name='$name' n_members=$n_members")

        val vb = Variable.Builder()
        vb.name = vName
        //vb.datatype = H4type.getDataType(datatype)
        // vb.spObject = Vinfo4(g4, varidx, datatype)
        //vb.dimList = dimList

        // read Variable attributes
        // repeat(nattrs) { vb.attributes.add( readAttribute(session, vdata_id, it)) }
        g4.gb.addVariable(vb)

        if (debug) println("  readVvariable name='$name' n_members=$n_members")
        return true
    }

    //////////////////////////////////////////////////////////////////////////////


    // Vdata interface (also called the VS interface or the VS API). Lets call it VStructure
    // A vdata is a collection of records whose values are stored in fixed-length fields.

    // sequentially iterates through an HDF file to obtain the vdata
    private fun iterateVdata(session: MemorySession, g4: Group4, fileid : Int) {
        if (debugVdata) println("iterateVdata '${g4.gb.name}'")
        var last_ref = -1
        while (true) {
            val vdata_ref = VSgetid(fileid, last_ref)
            if (vdata_ref == -1) {
                break
            }
            makeVStructure(session, g4, vdata_ref)

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

        // retrieves the reference numbers of lone vdatas in the file
        if (debugVdata) println(" VSlone")
        val max_refs = 128
        val ref_array_p = session.allocateArray(C_INT as MemoryLayout, max_refs.toLong())
        val num_of_lone_vdatas = VSlone(fileid, ref_array_p, max_refs);
        val ref_array = IntArray(num_of_lone_vdatas) { ref_array_p.getAtIndex(C_INT, it.toLong()) }

        for (idx in 0 until num_of_lone_vdatas) {
            makeVStructure(session, g4, ref_array[idx])
        }
    }

    private fun makeVStructure(session: MemorySession, g4: Group4, ref: Int) {
        val vdata_id = VSattach(fileid, ref, read_access_mode);

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
        if (g4.gb.variables.find {it.name == vsname } != null) return

        if (debugVdata) {
            println("   makeVStructure name='$vsname' class = $vclass nrecords=$nrecords fieldnames='$fieldnames' recsize=$recsize")
        }
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
            if (debugVS) println("    VSfield name='$name' fdatatype=$fdatatype offset='$offset' nelems=$nelems esize =$esize isize = $isize")
            val m = StructureMember(name, fdatatype, offset, intArrayOf(nelems))
            members.add(m)
            offset += isize
        }

        if (members.size == 1) {
            val member = members[0]
            vb.datatype = member.datatype
            vb.setDimensionsAnonymous(intArrayOf(member.nelems))
        } else {
            val typedef = CompoundTypedef(vsname, members)
            vb.datatype = Datatype.COMPOUND.withTypedef(typedef)
            g4.gb.typedefs.add(typedef)
            vb.setDimensionsAnonymous(intArrayOf(nrecords))
        }

        // LOOK need Vinfo
        // use VSnattrs()
        // repeat(nattrs) { vb.attributes.add( readAttribute(session, vdata_id, it)) }

        if (vclass.startsWith("Attr") and !vsname.startsWith("ProductMetadata")) {
            g4.gb.addAttribute(Attribute(vsname, "wtf"))
        } else {
            g4.gb.addVariable(vb)
        }

        checkErr("VSdetach", VSdetach(vdata_id))
    }

    //////////////////////////////////////////////////////////////////////////////

    private fun iterateSDs(session: MemorySession, g4: Group4, sd_id : Int) : Int {
        if (debugSD) println("iterateSDs '${g4.gb.name}'")

        val nvars_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)
        checkErr("SDfileinfo", SDfileinfo(sd_id, nvars_p, nattrs_p))
        val nvars = nvars_p[C_INT, 0]
        val nattrs = nattrs_p[C_INT, 0]

        repeat(nvars) { readSD(session, g4, sd_id, it) }

        repeat(nattrs) {
            val attr = readAttribute(session, sd_id, it)
            if ((attr.name != "ProductMetadata.0") and (attr.name != "CoreMetadata.0") and (attr.name != "StructMetadata.0")) {
                g4.gb.attributes.add(attr)
            } else {
                this.metadata.add(attr)
                if (attr.name == "StructMetadata.0") {
                    this.structMetadata = attr.values[0] as String
                }
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

        return nvars
    }

    @Throws(IOException::class)
    private fun readSD(session: MemorySession, g4: Group4, sd_id : Int, varidx: Int) {
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
            if (dimLength == 0) {
                dimLength = dims[dimidx]
            }
            val want = Dimension(dimName, dimLength, false, true)
            g4.gb.addDimensionIfNotExists(want)
            dimList.add(want.name) // name has been cleaned up
        }

        // create the Variable
        val vb = Variable.Builder()
        vb.name = vname
        vb.datatype = H4type.getDataType(datatype)
        vb.spObject = Vinfo4(g4, varidx, datatype)
        vb.dimList = dimList

        // read Variable attributes
        repeat(nattrs) { vb.attributes.add( readAttribute(session, varid, it)) }
        g4.gb.addVariable(vb)

        SDendaccess(varid)
        if (debugSD) println("  readSD '$vname' datatype=${vb.datatype} dims=${dims.contentToString()} nattrs=$nattrs")
    }

    //////////////////////////////////////////////////////////////////////

    // the GR API
    private fun iterateGRs(session: MemorySession, g4: Group4, gr_id : Int) {
        val n_datasets_p = session.allocate(C_INT, 0)
        val n_gattrs_p = session.allocate(C_INT, 0)

        // GRfileinfo ( int grid,  Addressable n_datasets,  Addressable n_attrs)
        val ret = GRfileinfo(gr_id, n_datasets_p, n_gattrs_p)
        if (ret != 0) {
            return
        }
        val n_datasets = n_datasets_p[C_INT, 0]
        val n_gattrs = n_gattrs_p[C_INT, 0]

        if (debugGR) println("readImages $n_datasets attrs = $n_gattrs")
        repeat(n_datasets) { idx -> makeGRVariable(session, g4, idx) }
    }

    private fun makeGRVariable(session: MemorySession, g4: Group4, gridx : Int) {
        val ri_id = GRselect(grs_id, gridx)
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

        if (debugGR) println("  GRgetiminfo '$name', n_comps=$n_comps, type=${vb.datatype}, interlace=$interlace, dims${dims.contentToString()}, attrs=$nattrs")
        GRendaccess(ri_id)
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

        val data_p: MemorySegment = session.allocate(nelems * datatype.size.toLong())
        checkErr ("SDreadattr", SDreadattr(sd_id, idx, data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(ByteOrder.LITTLE_ENDIAN) // ??
        bb.position(0)
        bb.limit(bb.capacity())
        val shape = intArrayOf(nelems)

        if (datatype == Datatype.CHAR) {
            val svalue = makeStringZ(bb.array(), Hdf4ClibFile.valueCharset)
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun applyStructMetadata(structMetadata : String) : Group.Builder {
        val odl = ODLparser().parseFromString((structMetadata))
        println("$odl")
        val odlt = ODLtransform().transform(odl)
        println("$odlt")
        val root = Group.Builder("")
        odlt.applyStructMetadata(root)
        val test = root.build(null)
        println("testRoot = ${test.cdl(true)}")
        return root
    }

    fun ODLgroup.applyStructMetadata(parent : Group.Builder) {
        val nested = Group.Builder(this.name)
        parent.addGroup(nested)
        this.variables.forEach { v ->
            if (v.name == "Dimensions") {
                v.attributes.forEach { att ->
                    nested.addDimension(Dimension(att.component1(), att.component2().toInt()))
                }
            }
            if (v.name == "Variables") {
                v.attributes.forEach { att ->
                    val name = att.component1()
                    val dimList = att.component2().split(",")
                    val vb = rootGroup4.gb.variables.find { it.name == name }
                    if (vb == null) {
                        println("Cant find variable $name")
                    } else {
                        vb.dimList = dimList
                        vb.dimensions.clear()
                        nested.addVariable(vb)
                    }
                }
            }
        }
        this.nested.forEach { it.applyStructMetadata(nested)}
    }

}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret")
    }
}

data class Group4(val name : String, val parent: Group4?) {
    var gb = Group.Builder(name)
    init {
        parent?.gb?.addGroup(gb)
    }
}

data class Vinfo4(val g4: Group4, val sds_index: Int, val datatype : Int)

