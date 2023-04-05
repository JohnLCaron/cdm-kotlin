package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.util.Indent
import com.sunya.netchdf.hdf4.ODLparser
import com.sunya.netchdf.hdf5.Datatype5
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_IGNORE_ATTS
import com.sunya.netchdf.hdf5Clib.ffm.*
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h.*
import java.io.IOException
import java.lang.foreign.*
import java.nio.ByteBuffer

const val MAX_NAME = 255L
const val MAX_DIMS = 255L

// Really a builder of the root Group.
class H5Cbuilder(val filename: String) {
    val rootBuilder = Group.Builder("")
    var formatType : String = ""

    val file_id : Long
    val structMetadata = mutableListOf<String>()
    internal val typeinfo = mutableListOf<H5CTypeInfo>() // by group ?

    init {
        MemorySession.openConfined().use { session ->
            val filenameSeg: MemorySegment = session.allocateUtf8String(filename)
            val fileHandle: MemorySegment = session.allocate(C_INT, 0)

            //     public static long H5Fopen ( Addressable filename,  int flags,  long access_plist) {
            file_id = H5Fopen(filenameSeg, H5F_ACC_RDONLY(), H5P_DEFAULT())
            if (debug) println("H5Fopen $filename fileHandle ${this.file_id}")

            /* format
            val format_p: MemorySegment = session.allocate(C_INT, 0)
            checkErr("nc_inq_format", nc_inq_format(ncid, format_p))
            this.format = format_p[C_INT, 0]
            if (debugFormat) println(" nc_inq_format = ${netcdfFormat(this.format)}")
            this.formatType = netcdfFormat(this.format).toString()

            // format extended
            val mode_p: MemorySegment = session.allocate(C_INT, 0)
            checkErr("nc_inq_format_extended", nc_inq_format_extended(ncid, format_p, mode_p))
            this.formatx = format_p[C_INT, 0]
            this.mode = mode_p[C_INT, 0]
            if (debugFormat) println(
                " nc_inq_format_extended = ${netcdfFormatExtended(this.formatx)} " +
                        "mode = 0x${java.lang.Long.toHexString(this.mode.toLong())} ${netcdfMode(this.mode)}"
            )

            val version = nc_inq_libvers().getUtf8String(0)
            if (debugFormat) println(" nc_inq_libvers = $version")

             */

            // read root group
            readGroup("/", GroupContext(session, rootBuilder, this.file_id, Indent(2, 0)))
            println("here")
            // rootGroup.addAttribute(Attribute(Netcdf4.NCPROPERTIES, version))

            // hdf-eos5
            if (structMetadata.isNotEmpty()) {
                val sm = structMetadata.joinToString("")
                ODLparser(rootBuilder, false).applyStructMetadata(sm)
            }
        }
    }

    private fun readGroup( g5name: String, context : GroupContext) {
        println("${context.indent}readGroup for '$g5name'")
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

        // H5O_iterate_t
        //typedef herr_t(* H5O_iterate_t) (hid_t obj, const char *name, const H5O_info_t *info, void *op_data)
        //Prototype for H5Ovisit(), H5Ovisit_by_name() operator (version 3)
        //
        //Parameters
        //[in]	obj	Object that serves as the root of the iteration; the same value as the H5Ovisit() obj_id parameter
        //[in]	name	Name of object, relative to obj, being examined at current step of the iteration
        //[out]	info	Information about that object
        //[in,out]	op_data	User-defined pointer to data required by the application in processing the object; a pass-through of the op_data pointer provided with the H5Ovisit3() function call
        //Returns
        //Zero causes the iterator to continue, returning zero when the iteration is complete.
        //A positive value causes the iterator to immediately return that positive value, indicating short-circuit success.
        //A negative value causes the iterator to immediately return that value, indicating failure.
        //
        //herr_t H5Ovisit(hid_t 	obj_id, H5_index_t 	idx_type, H5_iter_order_t 	order, H5O_iterate_t 	op, void *op_data )
        val opo_p = H5O_iterate_t.allocate(H5Oreceiver(nestedContext), context.session)
        // H5Ovisit ( long obj_id,  int idx_type,  int order,  Addressable op,  Addressable op_data)
        checkErr("H5Ovisit", H5Ovisit(group_id, H5_INDEX_NAME(), H5_ITER_INC(), opo_p, groupName))

        val oinfo_p = H5O_info_t.allocate(context.session)
        checkErr("H5Oget_info", H5Oget_info(group_id, oinfo_p))
        val num_attr = H5O_info_t.`num_attrs$get`(oinfo_p)
        val atts = readAttributes(g5name, num_attr.toInt(), context)
        atts.forEach{context.group.addAttribute(it)}

        // LOOK 1.12 uses H5Lvisit2, we probably want to upgrade
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
            readGroup(session, Group5(grpid, Group.Builder(name), g4))
        }

         */
    }

    private inner class H5Lreceiver(val context : GroupContext) : H5L_iterate_t {

        override fun apply(group: Long, name_p: MemoryAddress, infoAddress: MemoryAddress, op_data: MemoryAddress): Int {
            val linkname = name_p.getUtf8String(0)
            println("${context.indent}H5Lreceiver link='$linkname'")
            val indent = context.indent.incr()

            // long H5Oopen ( long loc_id,  Addressable name,  long lapl_id) {
            val loc_id = H5Oopen(group, name_p, H5P_DEFAULT())

            // int H5Oget_info ( long loc_id,  Addressable oinfo) {
            val oinfo_p = H5O_info_t.allocate(context.session)
            checkErr("H5Oget_info", H5Oget_info(loc_id, oinfo_p))
            val otype = H5O_info_t.`type$get`(oinfo_p)
            println("${indent}otype=$otype")

            val info = H5L_info_t.ofAddress(infoAddress, context.session)
            val type = H5L_info_t.`type$get`(info, 0L) // H5L_type_t
            if (type == H5L_TYPE_HARD()) {
                val address = H5L_info_t.u.`address$get`(info, 0L)
                println("${indent}H5L_TYPE_HARD, address=$address")
            } else if (type == H5L_TYPE_SOFT()) {
                val val_size = H5L_info_t.u.`val_size$get`(info, 0L)
                println("${indent}H5L_TYPE_SOFT, val_size=$val_size")
            }
            // recurse
            if (otype == H5O_TYPE_GROUP()) {
                val nestedGroup = Group.Builder(linkname)
                context.group.addGroup(nestedGroup)
                val nestedContext = context.copy(group = nestedGroup, indent = indent.incr())
                readGroup(linkname, nestedContext)
                return 0
            }
            return 0
        }
    }
    // H5O_TYPE_UNKNOWN  Unknown object type
    // H5O_TYPE_GROUP Object is a group
    // H5O_TYPE_DATASET Object is a dataset
    // H5O_TYPE_NAMED_DATATYPE Object is a named data type
    // H5O_TYPE_NTYPES  Number of different object types (must be last!)

    // typedef enum {
    //    H5L_TYPE_ERROR = (-1),      /* Invalid link type id         */
    //    H5L_TYPE_HARD = 0,          /* Hard link id                 */
    //    H5L_TYPE_SOFT = 1,          /* Soft link id                 */
    //    H5L_TYPE_EXTERNAL = 64,     /* External link id             */
    //    H5L_TYPE_MAX = 255	        /* Maximum link type id         */
    //} H5L_type_t;

    /*
    @Throws(IOException::class)
    private fun readGroupDimensions(session: MemorySession, g4: Group5) {
        //// Get dimension ids
        val numDims_p = session.allocate(C_INT, 0)
        // nc_inq_ndims(int ncid, int *ndimsp);
        //     public static int nc_inq_ndims ( int ncid,  Addressable ndimsp) {
        checkErr("nc_inq_ndims", nc_inq_ndims(g4.grpid, numDims_p))
        val numDims = numDims_p[C_INT, 0]

        val dimids_p = session.allocateArray(C_INT, numDims.toLong())
        // nc_inq_dimids(int ncid, int *ndims, int *dimids, int include_parents);
        // public static int nc_inq_dimids ( int ncid,  Addressable ndims, java.lang.foreign.Addressable dimids,  int include_parents) {
        checkErr("nc_inq_dimids", nc_inq_dimids(g4.grpid, numDims_p, dimids_p, 0))
        val numDims2 = numDims_p[C_INT, 0]
        if (debug) print(" nc_inq_dimids ndims = $numDims2")
        if (numDims != numDims2) {
            throw RuntimeException("numDimsInGroup $numDims != numDimsInGroup2 $numDims2")
        }
        val dimIds = IntArray(numDims)
        for (i in 0 until numDims) {
            dimIds[i] = dimids_p.getAtIndex(C_INT, i.toLong())
        }
        if (debug) println(" dimids = ${dimIds.toList()}")
        g4.dimIds = dimIds

        //// Get unlimited dimension ids
        checkErr("nc_inq_unlimdims", nc_inq_unlimdims(g4.grpid, numDims_p, dimids_p))
        val unumDims = numDims_p[C_INT, 0]
        val udimIds = IntArray(unumDims)
        for (i in 0 until unumDims) {
            udimIds[i] = dimids_p.getAtIndex(C_INT, i.toLong())
        }
        if (debug) println(" nc_inq_unlimdims ndims = $unumDims udimIds = ${udimIds.toList()}")
        g4.udimIds = udimIds

        val dimName_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
        val size_p = session.allocate(C_LONG, 0)
        for (dimId in dimIds) {
            // LOOK size_t
            // nc_inq_dim(int ncid, int dimid, char *name, size_t *lenp);
            //     public static int nc_inq_dim ( int ncid,  int dimid,  Addressable name,  Addressable lenp) {
            checkErr("nc_inq_dim", nc_inq_dim(g4.grpid, dimId, dimName_p, size_p))
            val dimName: String = dimName_p.getUtf8String(0)
            val dimLength = size_p[C_LONG, 0]
            val isUnlimited = udimIds.contains(dimId)
            if (debug) println(" nc_inq_dim $dimId = $dimName $dimLength $isUnlimited")

            if (dimName.startsWith("phony_dim_")) {
                val dimension = Dimension(dimLength.toInt())
                g4.dimHash[dimId] = dimension
            } else {
                val dimension = Dimension(dimName, dimLength.toInt(), true)
                g4.gb.addDimension(dimension)
                g4.dimHash[dimId] = dimension
            }
        }
    }

     */

    private inner class H5Oreceiver(val context : GroupContext) : H5O_iterate_t {

        // typedef herr_t(* H5O_iterate_t) (hid_t obj, const char *name, const H5O_info_t *info, void *op_data)
        //     int apply(long obj, java.lang.foreign.MemoryAddress name, java.lang.foreign.MemoryAddress info, java.lang.foreign.MemoryAddress op_data);
        override fun apply(group: Long, name_p: MemoryAddress, infoAddress: MemoryAddress, op_data: MemoryAddress): Int {
            val linkname = name_p.getUtf8String(0)
            println("${context.indent}H5Oreceiver link='$linkname'")
            if (linkname.contains("/")) // skip nested ones
                return 0
            val indent = context.indent.incr()

            // long H5Oopen ( long loc_id,  Addressable name,  long lapl_id) {
            val loc_id = H5Oopen(group, name_p, H5P_DEFAULT())

            // int H5Oget_info ( long loc_id,  Addressable oinfo) {
            val oinfo_p = H5O_info_t.allocate(context.session)
            checkErr("H5Oget_info", H5Oget_info(loc_id, oinfo_p))
            val otype = H5O_info_t.`type$get`(oinfo_p)
            val num_attr = H5O_info_t.`num_attrs$get`(oinfo_p)
            println("${indent}otype=$otype, num_attr='$num_attr'")

            /*
            val info = H5L_info_t.ofAddress(infoAddress, context.session)
            val type = H5L_info_t.`type$get`(info, 0L) // H5L_type_t
            if (type == H5L_TYPE_HARD()) {
                val address = H5L_info_t.u.`address$get`(info, 0L)
                println("${indent}H5L_TYPE_HARD, address=$address")
            } else if (type == H5L_TYPE_SOFT()) {
                val val_size = H5L_info_t.u.`val_size$get`(info, 0L)
                println("${indent}H5L_TYPE_SOFT, val_size=$val_size")
            } */
            if (otype == H5O_TYPE_DATASET()) {
                readDataset(linkname, num_attr.toInt(), context)
            }
            return 0
        }
    }

    @Throws(IOException::class)
    private fun readDataset(obj_name: String, numAtts : Int, context: GroupContext) {
        println("${context.indent}readDataset for '$obj_name'")
        if (obj_name == "Geo-Location/Latitude")
            println()
        val indent = context.indent.incr()

        // hid_t H5Dopen2(hid_t loc_id, const char * name, hid_t dapl_id)
        val obj_name_p: MemorySegment = context.session.allocateUtf8String(obj_name)
        val dataset_id = H5Dopen2(context.group5id,  obj_name_p, H5P_DEFAULT())

        // hid_t H5Dget_space	(	hid_t 	attr_id	)
        val dataspace_id = H5Dget_space(dataset_id)
        // hssize_t H5Sget_select_npoints	(	hid_t 	spaceid	)
        val dataspace_npoints = H5Sget_select_npoints(dataspace_id)
        // int H5Sget_simple_extent_dims	(	hid_t 	space_id, hsize_t 	dims[], hsize_t 	maxdims[] )
        //     public static int H5Sget_simple_extent_dims ( long space_id,  Addressable dims,  Addressable maxdims) {
        val dims_p = context.session.allocateArray(C_LONG as MemoryLayout, MAX_DIMS)
        val maxdims_p = context.session.allocateArray(C_LONG  as MemoryLayout, MAX_DIMS)
        val ndims = H5Sget_simple_extent_dims(dataspace_id, dims_p, maxdims_p)
        val dims = IntArray(ndims) { dims_p.getAtIndex(C_LONG, it.toLong()).toInt() }
        // hsize_t H5Dget_storage_size	(	hid_t 	attr_id	)
        val size = H5Dget_storage_size(dataset_id)

        val type_id = H5Dget_type(dataset_id)
        val h5ctype = readH5CTypeInfo(context, type_id, obj_name)

        // create the Variable
        val vb = Variable.Builder(obj_name)
        vb.datatype = h5ctype.datatype()
        vb.setDimensionsAnonymous(dims)

        val atts = readAttributes(obj_name, numAtts, context)
        vb.attributes.addAll(atts)

        context.group.addVariable(vb)

        println("${indent}'$obj_name' h5ctype=$h5ctype npoints=$dataspace_npoints data_size=$size" +
                " dims=${dims.contentToString()}")

        /*
        val nvars_p = session.allocate(C_INT, 0)
        checkErr("nc_inq_nvars", nc_inq_nvars(g4.grpid, nvars_p))
        val nvars = nvars_p[C_INT, 0]
        
        val varids_p = session.allocateArray(C_INT, nvars.toLong())
        checkErr("nc_inq_varids", nc_inq_varids(g4.grpid, nvars_p, varids_p))

        val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
        val xtype_p = session.allocate(C_INT, 0)
        val ndims_p = session.allocate(C_INT, 0)
        val dimids_p = session.allocateArray(C_INT, NC_MAX_DIMS().toLong())
        val natts_p = session.allocate(C_INT, 0)

        for (i in 0 until nvars.toLong()) {
            val varid = varids_p.getAtIndex(ValueLayout.JAVA_INT, i)

            // nc_inq_var(int ncid, int varid, char *name, nc_type *xtypep, int *ndimsp, int *dimidsp, int *nattsp);
            checkErr("nc_inq_var", nc_inq_var(g4.grpid, varid, name_p, xtype_p, ndims_p, dimids_p, natts_p))

            val vname: String = name_p.getUtf8String(0)
            val typeid = xtype_p[C_INT, 0]
            val ndims = ndims_p[C_INT, 0]
            val natts = natts_p[C_INT, 0]
            if (debug) println(" nc_inq_var $vname = $typeid $ndims $natts")

            // figure out the dimensions
            val dimIds = IntArray(ndims)
            for (idx in 0 until ndims) {
                dimIds[idx] = dimids_p.getAtIndex(C_INT, idx.toLong())
            }

            // create the Variable
            val vb = Variable.Builder(vname)
            vb.datatype = convertType(typeid)
            vb.dimensions.addAll(g4.makeDimList(dimIds))

            val usertype = if (typeid >= 32) userTypes[typeid] else null

            vb.spObject = Vinfo(g4, varid, typeid, usertype)

            // read Variable attributes
            if (natts > 0) {
                if (debug) println(" Variable attributes")
                val atts: List<Attribute.Builder> = readAttributes(session, g4.grpid, varid, natts)
                for (attb in atts) {
                    val att = attb.build()
                    vb.addAttribute(att)
                }
            }
            g4.gb.addVariable(vb)
        }

         */
    }

    @Throws(IOException::class)
    private fun readAttributes(obj_name: String, numAtts : Int, context: GroupContext): List<Attribute> {
        println("${context.indent}readAttributes for '$obj_name'")
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
            if (HDF5_IGNORE_ATTS.contains(aname))
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
            if (!h5ctype.isVlenString) {
                val data_p = context.session.allocate(size)
                // herr_t H5Aread(hid_t attr_id, hid_t 	type_id, void * 	buf)
                checkErr("H5Aread", H5Aread(attr_id, h5ctype.type_id, data_p))
                val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
                val bb = ByteBuffer.wrap(raw)
                bb.order(h5ctype.endian)

                //println("  '$aname' type_class=$type_class type_size=$type_size npoints=$dataspace_npoints data_size=$size" +
                //    " dims=${dims.contentToString()}")
                if (!h5ctype.isVlenString and (h5ctype.datatype5 != Datatype5.Vlen)) {
                    require(size == nelems * h5ctype.elemSize)
                }

                val values = processDataIntoArray(bb, h5ctype.datatype5, h5ctype.datatype(), dims, h5ctype.elemSize)
                val att = Attribute(aname, values.datatype, values.toList())
                results.add(att)
            } else {
                val strings_p: MemorySegment = context.session.allocateArray(ValueLayout.ADDRESS, nelems)
                checkErr("H5Aread", H5Aread(attr_id, h5ctype.type_id, strings_p))

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
            }
        }

        return results
    }

    /*
    fun readAttributeValues(session: MemorySession, grpid : Int, varid : Int, attname : String, datatype : Datatype, nelems : Long) : List<Any> {
        val name_p: MemorySegment = session.allocateUtf8String(attname)
        when (datatype) {
            Datatype.BYTE -> {
                val val_p = session.allocate(nelems) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_schar", nc_get_att_schar(grpid, varid, name_p, val_p))
                val result = mutableListOf<Byte>()
                val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                for (i in 0 until nelems.toInt()) {
                    result.add(raw[i])
                }
                return result
            }

            Datatype.UBYTE -> {
                val val_p = session.allocate(nelems) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_uchar", nc_get_att_uchar(grpid, varid, name_p, val_p))
                val result = mutableListOf<UByte>()
                val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                for (i in 0 until nelems.toInt()) {
                    result.add(raw[i].toUByte())
                }
                return result
            }

            Datatype.CHAR -> {
                if (nelems == 0L) return emptyList()
                val val_p = session.allocate(nelems+1) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_text", nc_get_att_text(grpid, varid, name_p, val_p))
                val text: String = val_p.getUtf8String(0)
                return listOf(text)
                // return if (text.isNotEmpty()) listOf(text) else emptyList() LOOK NIL
            }

            Datatype.DOUBLE -> {
                val val_p = session.allocateArray(C_DOUBLE, nelems)
                checkErr("nc_get_att_double", nc_get_att_double(grpid, varid, name_p, val_p))
                val result = mutableListOf<Double>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_DOUBLE, i))
                }
                return result
            }

            Datatype.FLOAT -> {
                val val_p = session.allocateArray(C_FLOAT, nelems)
                checkErr("nc_get_att_float", nc_get_att_float(grpid, varid, name_p, val_p))
                val result = mutableListOf<Float>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_FLOAT, i))
                }
                return result
            }

            Datatype.INT -> {
                val val_p = session.allocateArray(C_INT, nelems)
                checkErr("nc_get_att_int", nc_get_att_int(grpid, varid, name_p, val_p))
                val result = mutableListOf<Int>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_INT, i))
                }
                return result
            }

            Datatype.UINT -> {
                val val_p = session.allocateArray(C_INT, nelems)
                checkErr("nc_get_att_uint", nc_get_att_uint(grpid, varid, name_p, val_p))
                val result = mutableListOf<UInt>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_INT, i).toUInt())
                }
                return result
            }

            Datatype.LONG -> {
                val val_p = session.allocateArray(ValueLayout.JAVA_LONG as MemoryLayout, nelems)
                checkErr("nc_get_att_longlong", nc_get_att_longlong(grpid, varid, name_p, val_p))
                val result = mutableListOf<Long>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_LONG, i))
                }
                return result
            }

            Datatype.ULONG -> {
                val val_p = session.allocateArray(ValueLayout.JAVA_LONG as MemoryLayout, nelems)
                checkErr("nc_get_att_ulonglong", nc_get_att_ulonglong(grpid, varid, name_p, val_p))
                val result = mutableListOf<ULong>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_LONG, i).toULong())
                }
                return result
            }

            Datatype.SHORT -> {
                val val_p = session.allocateArray(C_SHORT, nelems)
                checkErr("nc_get_att_short", nc_get_att_short(grpid, varid, name_p, val_p))
                val result = mutableListOf<Short>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                }
                return result
            }

            Datatype.USHORT -> {
                val val_p = session.allocateArray(C_SHORT, nelems)
                checkErr("nc_get_att_ushort", nc_get_att_ushort(grpid, varid, name_p, val_p))
                val result = mutableListOf<UShort>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_SHORT, i).toUShort())
                }
                return result
            }

            Datatype.STRING -> {
                // this is fixed length, right? assume 0 terminated ???
                val strings_p : MemorySegment = session.allocateArray(ValueLayout.ADDRESS, nelems)
                /* for (i in 0 until nelems) {
                    // Allocate a string off-heap, then store a pointer to it
                    val cString: MemorySegment = session.allocate(NC_MAX_ATTRS().toLong()) // LOOK wrong
                    strings_p.setAtIndex(ValueLayout.ADDRESS, i, cString)
                } */
                // ( int ncid,  int varid,  Addressable name,  Addressable ip)
                checkErr("nc_get_att_string", nc_get_att_string(grpid, varid, name_p, strings_p))
                //val suggestions: MemoryAddress = strings_p.get(ValueLayout.ADDRESS, 0)
                val result = mutableListOf<String>()
                for (i in 0 until nelems) {
                    // val s1 = strings_p.getUtf8String(i*8) // LOOK wrong
                    val s2 : MemoryAddress = strings_p.getAtIndex(ValueLayout.ADDRESS, i)
                    if (s2 != MemoryAddress.NULL) {
                        val value = s2.getUtf8String(0)
                        val tvalue = transcodeString(value)
                        result.add(tvalue)
                    } else {
                        result.add("")
                    }
                }
                // nc_free_string() or does session handle this ??
                return result
            }

            else -> throw RuntimeException("Unsupported attribute data type == $datatype")
        }
    }

    private fun transcodeString(systemString: String): String {
        val byteArray = systemString.toByteArray(Charset.defaultCharset())
        return String(byteArray, StandardCharsets.UTF_8)
    }

    internal class ConvertedType internal constructor(val dt: Datatype) {
        var isVlen = false
    }

     */

    companion object {
        val debug = true
        val debugFormat = false
    }
}

/*
fun convertType(type: Int): Datatype {
    return when (type) {
        NC_BYTE() -> Datatype.BYTE
        NC_CHAR() -> Datatype.CHAR
        NC_SHORT()-> Datatype.SHORT
        NC_INT() -> Datatype.INT
        NC_FLOAT() -> Datatype.FLOAT
        NC_DOUBLE() -> Datatype.DOUBLE
        NC_UBYTE() -> Datatype.UBYTE
        NC_USHORT() -> Datatype.USHORT
        NC_UINT() -> Datatype.UINT
        NC_INT64() -> Datatype.LONG
        NC_UINT64() -> Datatype.ULONG
        NC_STRING() -> Datatype.STRING
        else -> {
            val userType: UserType = userTypes[type] ?: throw RuntimeException("Unknown User data type == $type")
            return when (userType.typedef.kind) {
                TypedefKind.Enum -> {
                    when (userType.size) {
                        1 -> Datatype.ENUM1.withTypedef(userType.typedef)
                        2 -> Datatype.ENUM2.withTypedef(userType.typedef)
                        4 -> Datatype.ENUM4.withTypedef(userType.typedef)
                        else -> throw RuntimeException("Unknown enum elem size == ${userType.size}")
                    }
                }
                TypedefKind.Opaque -> Datatype.OPAQUE.withTypedef(userType.typedef)
                TypedefKind.Vlen -> Datatype.VLEN.withTypedef(userType.typedef)
                TypedefKind.Compound -> Datatype.COMPOUND.withTypedef(userType.typedef)
                else -> throw RuntimeException("Unsupported data type == $type")
            }
        }
    }
}

     */

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
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }

    /*
    if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
        return ArrayString(shape, this.convertReferencesToDataObjectName(result as ArrayLong))
    }

     */

    return result
}

data class GroupContext(val session : MemorySession, val group: Group.Builder, val group5id: Long, val indent : Indent)
