package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.util.Indent
import com.sunya.netchdf.hdf4.ODLparser
import com.sunya.netchdf.hdf5.*
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_CLASS
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_LIST
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_NAME
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_SCALE
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_IGNORE_ATTS
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_SKIP_ATTS
import com.sunya.netchdf.hdf5Clib.ffm.*
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h.*
import com.sunya.netchdf.netcdf4.Netcdf4
import com.sunya.netchdf.netcdfClib.ffm.netcdf_h
import java.io.IOException
import java.lang.foreign.*
import java.nio.ByteBuffer

const val MAX_NAME = 2048L
const val MAX_DIMS = 255L

// Really a builder of the root Group.
class H5Cbuilder(val filename: String) {
    val rootBuilder = Group.Builder("")
    val file_id : Long
    fun formatType() = if (structMetadata.isEmpty()) "hdf5    " else "hdf-eos5"

    private val structMetadata = mutableListOf<String>()
    private val typeinfoMap = mutableMapOf<H5CTypeInfo, MutableList<Group.Builder>>()
    private val datasetMap = mutableMapOf<Long, Pair<Group.Builder, Variable.Builder>>()

    init {
        MemorySession.openConfined().use { session ->
            val filenameSeg: MemorySegment = session.allocateUtf8String(filename)
            val fileHandle: MemorySegment = session.allocate(C_INT, 0)

            //     public static long H5Fopen ( Addressable filename,  int flags,  long access_plist) {
            file_id = H5Fopen(filenameSeg, H5F_ACC_RDONLY(), H5P_DEFAULT())
            if (debug) println("H5Fopen $filename fileHandle ${this.file_id}")

            // read root group
            readGroup("/", GroupContext(session, rootBuilder, this.file_id, Indent(2, 0)))
        }

        addTypesToGroups()
        convertReferences(rootBuilder)

        // hdf-eos5
        if (structMetadata.isNotEmpty()) {
            val sm = structMetadata.joinToString("")
            ODLparser(rootBuilder, false).applyStructMetadata(sm)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    internal fun registerTypedef(typeInfo : H5CTypeInfo, gb : Group.Builder) : H5CTypeInfo {
        val groups = typeinfoMap.getOrPut(typeInfo) { mutableListOf() }
        groups.add(gb)
        return typeInfo
    }
    internal fun findTypeFromId(typeId : Long) : H5CTypeInfo? {
        return typeinfoMap.keys.find { H5Tequal(it.type_id, typeId) > 0 }
    }
    internal fun addTypesToGroups() {
        typeinfoMap.forEach { typeInfo, groupList ->
            if (groupList.size == 1) {
                groupList[0].addTypedef(typeInfo.typedef!!)
            } else {
                var topgroup = groupList[0]
                for (idx in 1 until groupList.size) {
                    topgroup = topgroup.commonParent(groupList[idx])
                }
            topgroup.addTypedef(typeInfo.typedef!!)
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////
    fun convertReferences(gb : Group.Builder) {
        val refAtts = gb.attributes.filter{ it.datatype == Datatype.REFERENCE}
        refAtts.forEach { att ->
            val convertAtt = convertAttribute(att)
            if (convertAtt != null) {
                gb.addAttribute(convertAtt)
            }
            gb.attributes.remove(att)
        }

        gb.variables.forEach{ vb ->
            val refAtts = vb.attributes.filter{ it.datatype == Datatype.REFERENCE}
            refAtts.forEach { att ->
                val convertAtt = convertAttribute(att)
                if (convertAtt != null) {
                    if (att.name == HDF5_DIMENSION_LIST) {
                        vb.dimList = convertAtt.values as List<String>
                    } else {
                        vb.addAttribute(convertAtt)
                    }
                }
                vb.attributes.remove(att)
            }
        }

        gb.groups.forEach{ convertReferences(it) }
    }

    fun convertAttribute(att : Attribute) : Attribute? {
        val svalues = mutableListOf<String>()
        att.values.forEach {
            val dsetId = it as Long
             val pair = datasetMap[dsetId]
              if (pair == null)  {
                println("Cant find dataset reference for $att")
                return null
            }
            val (gb, vb) = pair
            val name = vb.fullname(gb)
            svalues.add(name)
        }
        return att.copy(values=svalues)
    }

    ////////////////////////////////////////////////////////////////////////////////
    private fun readGroup( g5name: String, context : GroupContext) {
        if (debug) println("${context.indent}readGroup for '$g5name'")
        // group = H5Gopen(file, "Data", H5P_DEFAULT);
        val groupName: MemorySegment = context.session.allocateUtf8String(g5name)
        val group_id : Long = H5Gopen2(context.group5id, groupName, H5P_DEFAULT())
        if (debug) println("${context.indent}H5Gopen2 '$g5name' group_id=${group_id}")
        if (group_id < 0)
            return
        val indent = context.indent.incr()

        val nestedContext = GroupContext(context.session, context.group, group_id, indent.incr())

        //readGroupDimensions(session, group5)
        //readVariables(session, group5)

        // herr_t H5Gget_info	(hid_t loc_id, H5G_info_t *ginfo)
        //val ginfo_p = H5G_info_t.allocate(context.session)
        //checkErr("H5Gget_info", H5Gget_info(group_id, ginfo_p))
        //val nlinks = H5G_info_t.`nlinks$get`(ginfo_p)
        //if (debug) println("${indent}H5Gget_info nlinks=${nlinks}")

        // herr_t H5Gget_num_objs	(hid_t loc_id, hsize_t * 	num_objs)
        val num_objs_p = context.session.allocate(C_LONG, 0L)
        checkErr("H5Gget_num_objs", H5Gget_num_objs(group_id, num_objs_p))
        val num_objs = num_objs_p[C_LONG, 0]
        if (debug) println("${indent}H5Gget_num_objs num_objs=${num_objs}")

        // LOOK probably should use H5Giterate()? wait, "This function is deprecated in favor of the function H5Literate1()."

        // Iterates over links in a group, with user callback routine, according to the order within an index.
        //
        //Parameters
        //[in]	grp_id	Group identifier
        //[in]	idx_type	Index type H5_INDEX_NAME or H5_INDEX_CRT_ORDER
        //[in]	order	Iteration order
        //[in,out]	idx	Pointer to an iteration index to allow continuing a previous iteration
        //[in]	op	Callback function: typedef herr_t(* H5L_iterate_t) (hid_t group, const char *name, const H5L_info_t *info, void *op_data)
        //[in,out]	op_data	User-defined callback function context
        //     public static int H5Literate ( long grp_id,  int idx_type,  int order,  Addressable idx,  Addressable op,  Addressable op_data) {
        val idx_p = context.session.allocate(C_LONG, 0L)
        val op_p = H5L_iterate_t.allocate(H5Lreceiver(nestedContext), context.session)
        checkErr("H5Literate", H5Literate(group_id, H5_INDEX_NAME(), H5_ITER_INC(), idx_p, op_p, groupName))

        val oinfo_p = H5O_info_t.allocate(context.session)
        checkErr("H5Oget_info", H5Oget_info(group_id, oinfo_p))
        val num_attr = H5O_info_t.`num_attrs$get`(oinfo_p)
        val atts = readAttributes(group_id, g5name, num_attr.toInt(), context)

        atts.forEach { attr ->
            val moveup = !strict && attr.isString && attr.values.size == 1 && (attr.values[0] as String).length > attLengthMax
            if (moveup) { // too big for an attribute
                val vb = Variable.Builder(attr.name).setDatatype(Datatype.STRING)
                // LOOK vb.spObject = DataContainerAttribute(it.name, H5TypeInfo(it.mdt), it.dataPos, it.mdt, it.mds)
                context.group.addVariable( vb)
            } else {
                context.group.addAttribute(attr)
            }
        }

        // LOOK 1.12 uses H5Lvisit2, we might want to upgrade?
        // see https://docs.hdfgroup.org/hdf5/v1_12/
        //     public static int H5Lvisit ( long grp_id,  int idx_type,  int order,  Addressable op,  Addressable op_data) {
        // Recursively visits all links starting from a specified group.
        //
        //Parameters
        //[in]	grp_id	Group identifier
        //[in]	idx_type	Index type
        //[in]	order	Iteration order
        //[in]	op	Callback function
        //[in,out]	op_data	User-defined callback function context
        // For non-recursive iteration across the members of a group, see H5Literate1().
        //val groupName2: MemorySegment = session.allocateUtf8String(g5name)
        //val status2 = H5Lvisit(group_id, groupName, H5P_DEFAULT());
        //if (debug) println("H5Lvisit $groupName group_id ${group_id}")
    }

    // links are the subgroups and the data objects == variables.
    private inner class H5Lreceiver(val context : GroupContext) : H5L_iterate_t {

        override fun apply(group: Long, name_p: MemoryAddress, infoAddress: MemoryAddress, op_data: MemoryAddress): Int {
            val linkname = name_p.getUtf8String(0)
            if (debug) println("${context.indent}H5Lreceiver link='$linkname'")
            val indent = context.indent.incr()

            // long H5Oopen ( long loc_id,  Addressable name,  long lapl_id) {
            val loc_id = H5Oopen(group, name_p, H5P_DEFAULT())

            // int H5Oget_info ( long loc_id,  Addressable oinfo) {
            val oinfo_p = H5O_info_t.allocate(context.session)
            checkErr("H5Oget_info", H5Oget_info(loc_id, oinfo_p))
            val otype = H5O_info_t.`type$get`(oinfo_p)
            val num_attr = H5O_info_t.`num_attrs$get`(oinfo_p)

            val info = H5L_info_t.ofAddress(infoAddress, context.session)
            val ltype = H5L_info_t.`type$get`(info, 0L) // H5L_type_t
            if (ltype == H5L_TYPE_HARD()) {
                val address = H5L_info_t.u.`address$get`(info, 0L)
                if (debug) println("${indent}H5L_TYPE_HARD, address=$address")
            } else if (ltype == H5L_TYPE_SOFT()) {
                val val_size = H5L_info_t.u.`val_size$get`(info, 0L)
                if (debug) println("${indent}H5L_TYPE_SOFT, val_size=$val_size")
            }
            if (debugGraph) println("${context.indent}${H5O_TYPE.of(otype)} '$linkname' H5Lreceiver ${H5L_TYPE.of(ltype)}")

            if (otype == H5O_TYPE_GROUP()) {
                val nestedGroup = Group.Builder(linkname)
                context.group.addGroup(nestedGroup)
                val nestedContext = context.copy(group = nestedGroup)
                readGroup(linkname, nestedContext)
                return 0
            } else if ((otype == H5O_TYPE_DATASET()) and ((ltype == H5L_TYPE_HARD()) or useSoftLinks)) {
                // the soft links are symbolic links that point to existing datasets
                readDataset(linkname, num_attr.toInt(), context)
            }
            return 0
        }
    }

    enum class H5O_TYPE { UNKNOWN, GROUP, DATASET, NAMED_DATATYPE;
        companion object {
            fun of(num: Int) : H5O_TYPE {
                return when (num) {
                    -1 -> UNKNOWN
                    0 -> GROUP
                    1 -> DATASET
                    2 -> NAMED_DATATYPE
                    else -> throw RuntimeException("Unknown H5O_TYPE $num")
                }
            }
        }
    }

    enum class H5L_TYPE { ERROR, HARD, SOFT, EXTERNAL;
        companion object {
            fun of(num: Int) : H5L_TYPE {
                return when (num) {
                    -1 -> ERROR
                    0 -> HARD
                    1 -> SOFT
                    64 -> EXTERNAL
                    else -> throw RuntimeException("Unknown H5O_TYPE $num")
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun readDataset(obj_name: String, numAtts : Int, context: GroupContext) {
        if (debug) println("${context.indent}readDataset for '$obj_name'")
        val indent = context.indent.incr()

        // hid_t H5Dopen2(hid_t loc_id, const char * name, hid_t dapl_id)
        val obj_name_p: MemorySegment = context.session.allocateUtf8String(obj_name)
        val datasetId = H5Dopen2(context.group5id,  obj_name_p, H5P_DEFAULT())

        // hid_t H5Dget_space	(	hid_t 	attr_id	)
        val dataspace_id = H5Dget_space(datasetId)
        // hssize_t H5Sget_select_npoints	(	hid_t 	spaceid	)
        val dataspace_npoints = H5Sget_select_npoints(dataspace_id)
        // int H5Sget_simple_extent_dims	(	hid_t 	space_id, hsize_t 	dims[], hsize_t 	maxdims[] )
        //     public static int H5Sget_simple_extent_dims ( long space_id,  Addressable dims,  Addressable maxdims) {
        val dims_p = context.session.allocateArray(C_LONG as MemoryLayout, MAX_DIMS)
        val maxdims_p = context.session.allocateArray(C_LONG  as MemoryLayout, MAX_DIMS)
        val ndims = H5Sget_simple_extent_dims(dataspace_id, dims_p, maxdims_p)
        val dims = IntArray(ndims) { dims_p.getAtIndex(C_LONG, it.toLong()).toInt() }

        val type_id = H5Dget_type(datasetId)
        val h5ctype = readH5CTypeInfo(context, type_id, obj_name)

        // create the Variable
        val vb = Variable.Builder(obj_name)
        vb.datatype = h5ctype.datatype()

        var isDimensionScale = false
        var isVariable = true
        val atts = readAttributes(datasetId, obj_name, numAtts, context)
        atts.forEach { att ->
            if (att.name == HDF5_CLASS && (att.values[0] as String) == HDF5_DIMENSION_SCALE) {
                isDimensionScale = true
            } else if (att.name == HDF5_DIMENSION_NAME && (att.values[0] as String).startsWith(Netcdf4.NETCDF4_NOT_VARIABLE)) {
                isVariable = false
            }
        }
        val fatts = if (!isDimensionScale) {
            vb.setDimensionsAnonymous(dims)
            atts
        } else {
            val dim = Dimension(obj_name, dims[0])
            context.group.addDimension(dim)
            vb.addDimension(dim)
            if (hideInternalAttributes) atts.filter{ !HDF5_IGNORE_ATTS.contains(it.name) } else atts
        }

        vb.attributes.addAll(fatts)
        if (isVariable) context.group.addVariable(vb)

        // datasetId is transient
        val address = H5Dget_offset(datasetId)
        if (address > 0) datasetMap[address] = Pair(context.group, vb)

        if (obj_name.startsWith("StructMetadata")) {
            val data = readRegularData(context, datasetId, h5ctype, dims)
            require (data is ArrayString)
            structMetadata.add(data.values.get(0))
        }

        if (debug) println("${indent}'$obj_name' h5ctype=$h5ctype npoints=$dataspace_npoints dims=${dims.contentToString()}")
    }

    @Throws(IOException::class)
    private fun readAttributes(dataset_id : Long, obj_name: String, numAtts : Int, context: GroupContext): List<Attribute> {
        if (debug) println("${context.indent}readAttributes for '$obj_name'")
        val results = mutableListOf<Attribute>()

        repeat(numAtts) { idx ->
            // hid_t H5Aopen_by_idx	(	hid_t 	loc_id, const char * 	obj_name, H5_index_t 	idx_type, H5_iter_order_t 	order, hsize_t 	n, hid_t 	aapl_id, hid_t 	lapl_id)
            // long H5Aopen_by_idx ( long loc_id,  Addressable obj_name,  int idx_type,  int order,  long n,  long aapl_id,  long lapl_id) {
            val obj_name_p: MemorySegment = context.session.allocateUtf8String(obj_name)
            val attr_id = H5Aopen_by_idx(
                context.group5id,
                obj_name_p,
                H5_INDEX_NAME(),
                H5_ITER_INC(),
                idx.toLong(),
                H5P_DEFAULT(),
                H5P_DEFAULT()
            )

            // herr_t H5Aget_info	(	hid_t 	attr_id, H5A_info_t * 	ainfo)

            // ssize_t H5Aget_name	(	hid_t 	attr_id, size_t 	buf_size, char * 	buf)
            // long H5Aget_name ( long attr_id,  long buf_size,  Addressable buf)
            val name_p = context.session.allocate(MAX_NAME)
            val name_len = H5Aget_name(attr_id, MAX_NAME, name_p)
            val aname: String = name_p.getUtf8String(0)
            if (hideInternalAttributes && HDF5_SKIP_ATTS.contains(aname))
                return@repeat

            // hid_t H5Aget_space	(	hid_t 	attr_id	)
            val dataspace_id = H5Aget_space(attr_id)
            // hssize_t H5Sget_select_npoints	(	hid_t 	spaceid	)
            val nelems = H5Sget_select_npoints(dataspace_id)
            // int H5Sget_simple_extent_dims	(	hid_t 	space_id, hsize_t 	dims[], hsize_t 	maxdims[] )
            //     public static int H5Sget_simple_extent_dims ( long space_id,  Addressable dims,  Addressable maxdims) {
            val dims_p = context.session.allocateArray(C_LONG as MemoryLayout, MAX_DIMS)
            val maxdims_p = context.session.allocateArray(C_LONG as MemoryLayout, MAX_DIMS)
            val ndims = H5Sget_simple_extent_dims(dataspace_id, dims_p, maxdims_p)
            val dims = IntArray(ndims) { dims_p.getAtIndex(C_LONG, it.toLong()).toInt() }
            // hsize_t H5Aget_storage_size	(	hid_t 	attr_id	)
            val size = H5Aget_storage_size(attr_id)

            // hid_t H5Dget_type	(	hid_t 	attr_id	)
            val type_id = H5Aget_type(attr_id)
            val h5ctype = readH5CTypeInfo(context, type_id, aname)

            // read data
            if (h5ctype.isVlenString) {
                val strings_p: MemorySegment = context.session.allocateArray(ValueLayout.ADDRESS, nelems)
                checkErr("H5Aread VlenString", H5Aread(attr_id, h5ctype.type_id, strings_p))

                val slist = mutableListOf<String>()
                for (i in 0 until nelems) {
                    val s2: MemoryAddress = strings_p.getAtIndex(ValueLayout.ADDRESS, i)
                    if (s2 != MemoryAddress.NULL) {
                        val value = s2.getUtf8String(0)
                        // val tvalue = transcodeString(value)
                        slist.add(value)
                    } else {
                        slist.add("")
                    }
                }
                val att = Attribute(aname, Datatype.STRING, slist)
                results.add(att)

                // not sure about this
                // checkErr("H5Dvlen_reclaim", H5Dvlen_reclaim(attr_id, h5ctype.type_id, H5S_ALL(), strings_p)) // ??
            } else if (h5ctype.datatype5 == Datatype5.Vlen) {
                val vlen_p: MemorySegment = hvl_t.allocateArray(nelems.toInt(), context.session)
                checkErr("H5Aread Vlen", H5Aread(attr_id, h5ctype.type_id, vlen_p))
                val base = h5ctype.base!!
                val att = if (base.datatype5 == Datatype5.Reference) {
                    val values = readVlenReferences(attr_id, nelems, vlen_p)
                    Attribute(aname, Datatype.REFERENCE, values)
                } else {
                    val values = readVlenData(nelems, base.datatype(), vlen_p)
                    Attribute(aname, h5ctype.datatype(), values)
                }
                results.add(att)

                // not sure about this
                // checkErr("H5Dvlen_reclaim", H5Dvlen_reclaim(attr_id, h5ctype.type_id, H5S_ALL(), strings_p)) // ??
            } else if (h5ctype.datatype5 == Datatype5.Reference) {
                val refdata_p = context.session.allocate(nelems.toInt() * size)
                checkErr("H5Aread", H5Aread(attr_id, h5ctype.type_id, refdata_p))
                //     public static long H5Rdereference1 ( long obj_id,  int ref_type,  Addressable ref) {
                // LOOK nelems??

                // long H5Rdereference2 ( long obj_id,  long oapl_id,  int ref_type,  Addressable ref)
                // Returns identifier of referenced object
                val refobjId = H5Rdereference2(attr_id, H5P_DEFAULT(), H5R_OBJECT(), refdata_p)
                val address = H5Dget_offset(refobjId)
                val att = Attribute(aname, Datatype.REFERENCE, listOf(address))
                results.add(att)

            } else {
                val data_p = context.session.allocate(size)
                // herr_t H5Aread(hid_t attr_id, hid_t 	type_id, void * 	buf)
                checkErr("H5Aread", H5Aread(attr_id, h5ctype.type_id, data_p))
                val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
                val bb = ByteBuffer.wrap(raw)
                bb.order(h5ctype.endian)
                require(size == nelems * h5ctype.elemSize)

                val values = processDataIntoArray(bb, h5ctype.datatype5, h5ctype.datatype(), dims, h5ctype.elemSize)
                val att = Attribute(aname, values.datatype, values.toList())
                results.add(att)
            }
        }

        return results
    }

    // LOOK same as in nclib UserTypes
    internal fun readVlenData(nelems : Long, basetype : Datatype, vlen_p : MemorySegment) : List<*> {
        val parray = mutableListOf<Any>()
        for (elem in 0 until nelems) {
            val count = hvl_t.`len$get`(vlen_p, elem)
            val address: MemoryAddress = hvl_t.`p$get`(vlen_p, elem)
            for (idx in 0 until count) {
                val wtf = when (basetype) {
                    Datatype.BYTE-> address.get(ValueLayout.JAVA_BYTE, idx)
                    Datatype.SHORT -> address.getAtIndex(netcdf_h.C_SHORT, idx)
                    Datatype.INT -> address.getAtIndex(netcdf_h.C_INT, idx)
                    Datatype.LONG -> address.getAtIndex(netcdf_h.C_LONG, idx)
                    Datatype.DOUBLE -> address.getAtIndex(netcdf_h.C_DOUBLE,  idx)
                    Datatype.FLOAT -> address.getAtIndex(netcdf_h.C_FLOAT, idx)
                    Datatype.STRING -> address.getUtf8String(0)
                    else -> throw RuntimeException("readVlenDataList unknown type = ${basetype}")
                }
                parray.add(wtf)
            }
        }
        return parray
    }

    internal fun readVlenReferences(obj_id : Long, nelems : Long, vlen_p : MemorySegment) : List<Long> {
        val parray = mutableListOf<Long>()
        for (elem in 0 until nelems) {
            val count = hvl_t.`len$get`(vlen_p, elem)
            val address: MemoryAddress = hvl_t.`p$get`(vlen_p, elem)
            // hid_t H5Rdereference1(hid_t obj_id, H5R_type_t ref_type, const void *ref)
            // H5Rdereference1 ( long obj_id,  int ref_type,  Addressable ref)
            val refobjId = H5Rdereference1(obj_id, H5R_OBJECT(), address)
            val refaddress = H5Dget_offset(refobjId)
            parray.add(refaddress)
        }
        return parray
    }

    internal fun readRegularData(context : GroupContext, datasetId : Long, h5ctype : H5CTypeInfo, dims : IntArray) : ArrayTyped<*> {
        val size = dims.computeSize() * h5ctype.elemSize.toLong()
        val data_p = context.session.allocate(size)

        // int H5Dread ( long dset_id,  long mem_type_id,  long mem_space_id,  long file_space_id,  long plist_id,  Addressable buf) {
        // herr_t H5Dread(hid_t dset_id, hid_t 	mem_type_id, hid_t 	mem_space_id, hid_t file_space_id, hid_t dxpl_id, void *buf)
        //[in]	dset_id	Dataset identifier Identifier of the dataset to read from
        //[in]	mem_type_id	Identifier of the memory datatype
        //[in]	mem_space_id	Identifier of the memory dataspace
        //[in]	file_space_id	Identifier of the dataset's dataspace in the file
        //[in]	dxpl_id	Identifier of a transfer property list
        //[out]	buf	Buffer to receive data read from file

        checkErr("H5Dread", H5Dread(datasetId, h5ctype.type_id, H5S_ALL(), H5S_ALL(), H5P_DEFAULT(), data_p))
        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val bb = ByteBuffer.wrap(raw)
        bb.order(h5ctype.endian)

        return processDataIntoArray(bb, h5ctype.datatype5, h5ctype.datatype(), dims, h5ctype.elemSize)
    }

    companion object {
        val debug = false
        val debugGraph = false
        val useSoftLinks = false
        val hideInternalAttributes = true
    }
}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret")
    }
}

internal fun processDataIntoArray(bb: ByteBuffer, datatype5 : Datatype5, datatype: Datatype, shape : IntArray, elemSize : Int): ArrayTyped<*> {

    /*
    if (h5type.hdfType == Datatype5.Compound) {
        val members = (datatype.typedef as CompoundTypedef).members
        val sdataArray =  ArrayStructureData(shape, bb, elemSize, members)
        return processCompoundData(sdataArray, bb.order())
    }
    */

    // convert to array of Strings by reducing rank by 1, tricky shape shifting for non-scalars
    if (datatype5 == Datatype5.String) {
        val extshape = IntArray(shape.size + 1) {if (it == shape.size) elemSize else shape[it] }
        val result = ArrayUByte(extshape, bb)
        return result.makeStringsFromBytes()
    }

    val result = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
        else -> {
            return ArraySingle(shape, Datatype.INT, 0)
            // throw IllegalStateException("unimplemented type= $datatype")
        }
    }

    /*
    if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
        return ArrayString(shape, this.convertReferencesToDataObjectName(result as ArrayLong))
    }

     */

    return result
}

data class GroupContext(val session : MemorySession, val group: Group.Builder, val group5id: Long, val indent : Indent)
