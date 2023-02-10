package com.sunya.netchdf.netcdfClib

import com.sunya.cdm.api.*
import com.sunya.netchdf.netcdf3.*
import com.sunya.netchdf.netcdf3.NetcdfFileFormat.Companion.netcdfFormat
import com.sunya.netchdf.netcdf3.NetcdfFileFormat.Companion.netcdfFormatExtended
import com.sunya.netchdf.netcdf3.NetcdfFileFormat.Companion.netcdfMode
import com.sunya.netchdf.netcdf4.ffm.netcdf_h.*
import java.io.IOException
import java.lang.foreign.*
import java.util.*


private val debug = false
private val debugFormat = true

/*
apt-cache search netcdf
dpkg -L libnetcdf-dev
 /usr/include/netcdf.h
 /usr/lib/x86_64-linux-gnu/libnetcdf.so

apt-cache search libhdf5-dev
dpkg -L libhdf5-dev
 /usr/include/hdf5/serial/hdf5.h
 /usr/lib/x86_64-linux-gnu/hdf5/serial/libhdf5.so

cd /home/snake/install/jextract-19/bin
./jextract --source \
    --header-class-name netcdf_h \
    --target-package sunya.cdm.netcdf4.ffm \
    -I /usr/include/netcdf.h \
    -l /usr/lib/x86_64-linux-gnu/libnetcdf.so \
    --output /home/snake/dev/github/cdm-kotlin/src/main/java \
    /usr/include/netcdf.h
 */

fun main(args: Array<String>) {
    val h = NCheader("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc")
    if (debug) println(h.rootGroup.build(null).cdlString())
}

// Really a builder of the root Group.
class NCheader(val filename: String) {
    val rootGroup = Group.Builder("")
    private var ncid = 0
    private var format = 0
    private var formatx = 0
    private var mode = 0
    private val userTypes = mutableMapOf<Int, UserType>() // hash by typeid

    init {
        MemorySession.openConfined().use { session ->
            build(session)
        }
    }

    @Throws(IOException::class)
    private fun build(session: MemorySession) {
        val filenameSeg: MemorySegment = session.allocateUtf8String(filename)
        val fileHandle: MemorySegment = session.allocate(C_INT, 0)

        checkErr("nc_open", nc_open(filenameSeg, 0, fileHandle))
        this.ncid = fileHandle[C_INT, 0]
        if (debug) println("nc_open $filename fileHandle ${this.ncid}")

        // format
        val format_p: MemorySegment = session.allocate(C_INT, 0)
        checkErr("nc_inq_format", nc_inq_format(ncid, format_p))
        this.format = format_p[C_INT, 0]
        if (debugFormat) println(" nc_inq_format = ${netcdfFormat(this.format)}")

        // format extended
        val mode_p: MemorySegment = session.allocate(C_INT, 0)
        checkErr("nc_inq_format_extended", nc_inq_format_extended(ncid, format_p, mode_p))
        this.formatx = format_p[C_INT, 0]
        this.mode = mode_p[C_INT, 0]
        if (debugFormat) println(" nc_inq_format_extended = ${netcdfFormatExtended(this.formatx)} " +
                "mode = 0x${java.lang.Long.toHexString(this.mode.toLong())} ${netcdfMode(this.mode)}")

        if (debugFormat) println(" nc_inq_libvers = ${nc_inq_libvers().getUtf8String(0)}")

        // read root group
        readGroup(session, Group4(ncid, rootGroup, null))
    }

    private fun readGroup(session: MemorySession, g4: Group4) {
        // groupBuilderHash[g4.gb] = g4.grpid
        readGroupDimensions(session, g4)

        readUserTypes(session, g4.grpid, g4.gb)

        // group attributes
        val numAtts_p = session.allocate(C_INT, 0)
        checkErr("nc_inq_natts", nc_inq_natts(g4.grpid, numAtts_p))
        val numAtts = numAtts_p[C_INT, 0]

        if (numAtts > 0) {
            if (debug) println(" root group")
            val gatts: List<com.sunya.cdm.api.Attribute.Builder> = readAttributes(session, g4.grpid, NC_GLOBAL(), numAtts)
            for (attb in gatts) {
                val att = attb.build()
                g4.gb.addAttribute(attb.build())
                if (debug) println("  att = $att")
            }
        }

        readVariables(session, g4)

        // subgroups
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
        }
    }

    @Throws(IOException::class)
    private fun readGroupDimensions(session: MemorySession, g4: Group4) {
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

            val dimension = Dimension(dimName, dimLength.toInt(), isUnlimited)
            g4.gb.addDimension(dimension)
            g4.dimHash[dimId] = dimension
        }
    }

    @Throws(IOException::class)
    private fun readVariables(session: MemorySession, g4: Group4) {
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
            val vb = Variable.Builder()
            vb.name = vname
            vb.dataType = getDataType(typeid)
            vb.dimensions.addAll(g4.makeDimList(dimIds))
            vb.spObject = Vinfo(g4, varid, typeid)

            // read Variable attributes
            if (natts > 0) {
                if (debug) println(" Variable $vname")
                val atts: List<com.sunya.cdm.api.Attribute.Builder> = readAttributes(session, g4.grpid, varid, natts)
                for (attb in atts) {
                    val att = attb.build()
                    vb.attributes.add(attb.build())
                    if (debug)  println("  att = $att")
                }
            }

            g4.gb.addVariable(vb)
        }
    }

    @Throws(IOException::class)
    private fun readAttributes(session: MemorySession, grpid: Int, varid: Int, natts: Int): List<com.sunya.cdm.api.Attribute.Builder> {
        val result = mutableListOf<com.sunya.cdm.api.Attribute.Builder>()
        val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
        val type_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_LONG, 0)
        for (attnum in 0 until natts) {
            checkErr("nc_inq_attname", nc_inq_attname(grpid, varid, attnum, name_p))

            // nc_inq_att(int ncid, int varid, const char *name, nc_type *xtypep, size_t *lenp);
            //     public static int nc_inq_att ( int ncid,  int varid,  Addressable name,  Addressable xtypep,  Addressable lenp) {
            checkErr("nc_inq_att", nc_inq_att(grpid, varid, name_p, type_p, size_p))
            val attName: String = name_p.getUtf8String(0)
            val attType = type_p[C_INT, 0]
            val attLength = size_p[C_LONG, 0]
            val dataType = getDataType(attType)
            if (debug) println("  nc_inq_att $grpid $varid = $attName $dataType nelems=$attLength")

            val attb = com.sunya.cdm.api.Attribute.Builder().setName(attName).setDataType(dataType)
            if (attLength > 0) {
                attb.values = readAttributeValues(session, grpid, varid, name_p, dataType, attLength)
            }
            result.add(attb)

        }
        return result
    }

    fun readAttributeValues(session: MemorySession, grpid : Int, varid : Int, name_p : MemorySegment, dataType : DataType, nelems : Long) : List<Any> {
        when (dataType) {
            DataType.BYTE -> {
                val val_p = session.allocate(nelems) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_schar", nc_get_att_schar(grpid, varid, name_p, val_p))
                val result = mutableListOf<Byte>()
                val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                for (i in 0 until nelems.toInt()) {
                    result.add(raw[i])
                }
                return result
            }

            DataType.UBYTE -> {
                val val_p = session.allocate(nelems) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_uchar", nc_get_att_uchar(grpid, varid, name_p, val_p))
                val result = mutableListOf<UByte>()
                val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                for (i in 0 until nelems.toInt()) {
                    result.add(raw[i].toUByte())
                }
                return result
            }

            DataType.CHAR -> {
                val val_p = session.allocate(nelems+1) // add 1 to make sure its zero terminated ??
                checkErr("nc_get_att_text", nc_get_att_text(grpid, varid, name_p, val_p))
                val text: String = val_p.getUtf8String(0)
                return listOf(text)
            }

            DataType.DOUBLE -> {
                val val_p = session.allocateArray(C_DOUBLE, nelems)
                checkErr("nc_get_att_double", nc_get_att_double(grpid, varid, name_p, val_p))
                val result = mutableListOf<Double>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_DOUBLE, i))
                }
                return result
            }

            DataType.FLOAT -> {
                val val_p = session.allocateArray(C_FLOAT, nelems)
                checkErr("nc_get_att_float", nc_get_att_float(grpid, varid, name_p, val_p))
                val result = mutableListOf<Float>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_FLOAT, i))
                }
                return result
            }

            DataType.INT -> {
                val val_p = session.allocateArray(C_INT, nelems)
                checkErr("nc_get_att_int", nc_get_att_int(grpid, varid, name_p, val_p))
                val result = mutableListOf<Int>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_INT, i))
                }
                return result
            }

            DataType.UINT -> {
                val val_p = session.allocateArray(C_INT, nelems)
                checkErr("nc_get_att_uint", nc_get_att_uint(grpid, varid, name_p, val_p))
                val result = mutableListOf<UInt>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_INT, i).toUInt())
                }
                return result
            }

            DataType.LONG -> {
                val val_p = session.allocateArray(ValueLayout.JAVA_LONG as MemoryLayout, nelems)
                checkErr("nc_get_att_longlong", nc_get_att_longlong(grpid, varid, name_p, val_p))
                val result = mutableListOf<Long>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_LONG, i))
                }
                return result
            }

            DataType.ULONG -> {
                val val_p = session.allocateArray(ValueLayout.JAVA_LONG as MemoryLayout, nelems)
                checkErr("nc_get_att_ulonglong", nc_get_att_ulonglong(grpid, varid, name_p, val_p))
                val result = mutableListOf<ULong>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_LONG, i).toULong())
                }
                return result
            }

            DataType.SHORT -> {
                val val_p = session.allocateArray(C_SHORT, nelems)
                checkErr("nc_get_att_short", nc_get_att_short(grpid, varid, name_p, val_p))
                val result = mutableListOf<Short>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                }
                return result
            }

            DataType.USHORT -> {
                val val_p = session.allocateArray(C_SHORT, nelems)
                checkErr("nc_get_att_ushort", nc_get_att_ushort(grpid, varid, name_p, val_p))
                val result = mutableListOf<UShort>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_SHORT, i).toUShort())
                }
                return result
            }

            DataType.STRING -> {
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
                        result.add(s2.getUtf8String(0))
                    }
                }
                // nc_free_string() or does session handle this ??
                return result
            }

            else -> { // throw IllegalArgumentException("unknown type == $dataType")
                println("unknown type == $dataType")
                return emptyList()
            }
        }
    }

    @Throws(IOException::class)
    private fun readUserTypes(session: MemorySession, grpid: Int, g: Group.Builder) {
        val numUserTypes_p = session.allocate(C_INT, 0)
        val MAX_USER_TYPES = 1000L // bad API
        val userTypes_p = session.allocateArray(C_INT, MAX_USER_TYPES)
        checkErr("nc_inq_typeids", nc_inq_typeids(grpid, numUserTypes_p, userTypes_p))
        val numUserTypes = numUserTypes_p[C_INT, 0]
        if (numUserTypes == 0) {
            return
        }

        // for each defined "user type", get information, store in Map
        for (idx in 0 until numUserTypes) {
            val userTypeId = userTypes_p.getAtIndex(ValueLayout.JAVA_INT, idx.toLong())

            val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
            val size_p = session.allocate(C_LONG, 0)
            val baseType_p: MemorySegment = session.allocate(C_INT, 0)
            val nfields_p = session.allocate(C_LONG, 0)
            val typeClass_p = session.allocate(C_INT, 0)

            // grpid: the group containing the user defined type.
            // userTypeId: The typeid for this type, as returned by nc_def_compound, nc_def_opaque, nc_def_enum,
            //   nc_def_vlen, or nc_inq_var.
            // name: If non-NULL, the name of the user defined type.. It will be NC_MAX_NAME bytes or less.
            // size: If non-NULL, the (in-memory) size of the type in bytes.. VLEN type size is the size of nc_vlen_t.
            // baseType: may be used to malloc space for the data, no matter what the type.
            // nfields: If non-NULL, the number of fields for enum and compound types.
            // typeClass: the class of the user defined type, NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
            checkErr("nc_inq_user_type", nc_inq_user_type(grpid, userTypeId, name_p, size_p, baseType_p, nfields_p, typeClass_p))
            val name: String = name_p.getUtf8String(0)
            val size = size_p[C_LONG, 0]
            val baseType = baseType_p[C_INT, 0]
            val nfields = nfields_p[C_LONG, 0]
            val typeClass = typeClass_p[C_INT, 0]

            val ut = UserType(session, grpid, userTypeId, name, size, baseType, nfields, typeClass)
            userTypes[userTypeId] = ut

            if (typeClass == NC_ENUM()) {
                //val map: Map<Int, String> = makeEnum(grpid, typeid)
                //ut.e = EnumTypedef(name, map, ut.getEnumBaseType())
                //g.addEnumTypedef(ut.e)
            } else if (typeClass == NC_OPAQUE()) {
                val nameo_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
                val sizeo_p = session.allocate(C_LONG, 0)
                checkErr("nc_inq_opaque", nc_inq_opaque(grpid, userTypeId, nameo_p, sizeo_p))
                ut.setSize(sizeo_p[C_LONG, 0].toInt())
                // doesnt seem to be any new info
                // String nameos = nameo_p.getUtf8String(0)
            }
        }
    }

    internal class UserType(session: MemorySession,
                            var grpid: Int,
                            var typeid: Int,
                            var name: String,
                            size: Long, // the base typeid for vlen and enum types
                            var baseTypeid: Int, // the number of fields for enum and compound types
                            var nfields: Long, // the class of the user defined type: NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
                            var typeClass: Int
    ) {
        var size // the size of the user defined type
                : Int
        // var e: EnumTypedef? = null
        var flds = mutableListOf<CompoundField>()

        init {
            this.size = size.toInt()
            if (typeClass == NC_COMPOUND()) {
                readFields(session)
            }
        }

        // Allow size override for e.g. opaque
        fun setSize(size: Int): UserType {
            this.size = size
            return this
        }

        val enumBaseType: DataType?
            get() {
                // set the enum's basetype
                if (baseTypeid > 0 && baseTypeid <= NC_MAX_ATOMIC_TYPE()) {
                    val cdmtype: DataType
                    cdmtype = when (baseTypeid) {
                        NC_CHAR(), NC_UBYTE(), NC_BYTE() -> DataType.ENUM1
                        NC_USHORT(), NC_SHORT() -> DataType.ENUM2
                        NC_UINT(), NC_INT() -> DataType.ENUM4
                        else -> DataType.ENUM4
                    }
                    return cdmtype
                }
                return null
            }

        fun addField(fld: CompoundField) {
            flds.add(fld)
        }

        fun setFields(flds: List<CompoundField>) {
            this.flds.addAll(flds)
        }

        override fun toString(): String {
            return ("UserType" + "{grpid=" + grpid + ", typeid=" + typeid + ", name='" + name + '\'' + ", size=" + size
                    + ", baseTypeid=" + baseTypeid + ", nfields=" + nfields + ", typeClass=" + typeClass + '}')
        }

        @Throws(IOException::class)
        fun readFields(session: MemorySession) {
            for (fldidx in 0 until nfields.toInt()) {
                val fldname_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
                val offset_p = session.allocate(C_LONG, 0)
                val fldtypeid_p = session.allocate(C_INT, 0)
                val ndims_p = session.allocate(C_INT, 0)

                val dims_p = session.allocateArray(C_INT, NC_MAX_DIMS().toLong())
                checkErr("nc_inq_compound_field", nc_inq_compound_field(grpid, typeid, fldidx, fldname_p, offset_p, fldtypeid_p, ndims_p, dims_p))
                val fldname: String = fldname_p.getUtf8String(0)
                val offset = offset_p[C_LONG, 0]
                val fldtypeid = fldtypeid_p[C_INT, 0]
                val ndims = ndims_p[C_INT, 0]
                val dims = IntArray(ndims) {idx -> dims_p.getAtIndex(C_INT, idx.toLong())}

                addField(CompoundField(grpid, typeid, fldidx, fldname, offset.toInt(), fldtypeid, ndims, dims))
            }
        }
    }

    // A field in a compound type
    internal class CompoundField(
        val grpid: Int,
        val typeid: Int,
        val fldidx: Int,
        val name: String,
        val offset: Int,
        val fldtypeid: Int,
        val ndims: Int,
        val dims: IntArray
    ) {
        var ctype: ConvertedType? = null // wtf?

        // int total_size;
        var data: List<Array<*>>? = null

        init {
            // ctype = convertArrayType(fldtypeid)
            /* if (isVlen(fldtypeid)) {
                val edims = IntArray(ndims + 1)
                if (ndims > 0) {
                    System.arraycopy(dimz, 0, edims, 0, ndims)
                }
                edims[ndims] = -1
                dims = edims
                this.ndims++
            } */
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || javaClass != o.javaClass) {
                return false
            }
            val field = o as CompoundField
            return (grpid == field.grpid && typeid == field.typeid && fldidx == field.fldidx && offset == field.offset && fldtypeid == field.fldtypeid && ndims == field.ndims && name == field.name
                    && Arrays.equals(dims, field.dims))
        }

        override fun hashCode(): Int {
            return Objects.hash(grpid, typeid, fldidx, name, offset, fldtypeid, ndims, dims)
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("Field")
            sb.append("{grpid=").append(grpid)
            sb.append(", typeid=").append(typeid)
            sb.append(", fldidx=").append(fldidx)
            sb.append(", name='").append(name).append('\'')
            sb.append(", offset=").append(offset)
            sb.append(", fldtypeid=").append(fldtypeid)
            sb.append(", ndims=").append(ndims)
            sb.append(", dims=").append(if (dims == null) "null" else "")
            var i = 0
            while (dims != null && i < dims!!.size) {
                sb.append(if (i == 0) "" else ", ").append(dims!![i])
                ++i
            }
            sb.append(", dtype=").append(ctype?.dt)
            // if (ctype.isVlen) sb.append("(vlen)")
            sb.append('}')
            return sb.toString()
        }

        /*
        @Throws(IOException::class)
        fun makeMemberVariable(g: Group.Builder?): Variable.Builder {
            val v = readVariable(g, name, fldtypeid, "")
            v.setDimensionsAnonymous(dims)
            return v
        }

         */
    }

    internal class ConvertedType internal constructor(val dt: DataType) {
        var isVlen = false
    }

    internal class Group4(val grpid: Int, val gb: Group.Builder, val parent: Group4?) {
        var dimIds : IntArray? = null
        var udimIds : IntArray? = null
        val dimHash = mutableMapOf<Int, Dimension>()

        init {
            parent?.gb?.addGroup(gb)
        }

        fun makeDimList(dimIds : IntArray) : List<Dimension> {
            return dimIds.map { findDim(it)?: throw IllegalStateException() }
        }

        fun findDim(dimId : Int) : Dimension? {
            return dimHash[dimId] ?: parent?.findDim(dimId)
        }

    }

    internal data class Vinfo(val g4: Group4, val varid: Int, val typeid: Int)

    fun getDataType(type: Int): DataType {
        when (type) {
            NC_BYTE() -> return DataType.BYTE
            NC_CHAR() -> return DataType.CHAR
            NC_SHORT()-> return DataType.SHORT
            NC_INT() -> return DataType.INT
            NC_FLOAT() -> return DataType.FLOAT
            NC_DOUBLE() -> return DataType.DOUBLE
            NC_UBYTE() -> return DataType.UBYTE
            NC_USHORT() -> return DataType.USHORT
            NC_UINT() -> return DataType.UINT
            NC_INT64() -> return DataType.LONG
            NC_UINT64() -> return DataType.ULONG
            NC_STRING() -> return DataType.STRING
            else -> {
                val userType: UserType? = userTypes[type]
                if (userType == null) {
                    throw RuntimeException("Unsupported attribute data type == $type")
                } else if (userType.typeClass == NC_ENUM()) {
                    return DataType.ENUM1
                } else if (userType.typeClass == NC_OPAQUE()) {
                    return DataType.OPAQUE
                } else if (userType.typeClass == NC_VLEN()) {
                    return DataType.STRUCTURE // LOOK
                } else if (userType.typeClass == NC_COMPOUND()) {
                    return DataType.STRUCTURE
                } else {
                    throw RuntimeException("Unsupported attribute data type == $type")
                }
            }
        }
}

}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret = ${nc_strerror(ret).getUtf8String(0)}")
    }
}