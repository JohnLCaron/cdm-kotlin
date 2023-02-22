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
private val debugFormat = false

fun main(args: Array<String>) {
    val h = NCheader("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc")
    if (debug) println(h.rootGroup.build(null).cdl(true))
}

internal val userTypes = mutableMapOf<Int, UserType>() // hash by typeid

// Really a builder of the root Group.
class NCheader(val filename: String) {
    val rootGroup = Group.Builder("")
    private var ncid = 0
    private var format = 0
    private var formatx = 0
    private var mode = 0

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

        val version = nc_inq_libvers().getUtf8String(0)
        if (debugFormat) println(" nc_inq_libvers = $version")

        // read root group
        readGroup(session, Group4(ncid, rootGroup, null))
        // rootGroup.addAttribute(Attribute(Netcdf4.NCPROPERTIES, version))
    }

    private fun readGroup(session: MemorySession, g4: Group4) {
        // groupBuilderHash[g4.gb] = g4.grpid
        readGroupDimensions(session, g4)

        readUserTypes(session, g4.grpid, g4.gb, userTypes)

        // group attributes
        val numAtts_p = session.allocate(C_INT, 0)
        checkErr("nc_inq_natts", nc_inq_natts(g4.grpid, numAtts_p))
        val numAtts = numAtts_p[C_INT, 0]

        if (numAtts > 0) {
            if (debug) println(" root group")
            val gatts: List<Attribute.Builder> = readAttributes(session, g4.grpid, NC_GLOBAL(), numAtts)
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

            if (dimName.startsWith("phony_dim_")) {
                val dimension = Dimension(dimLength.toInt())
                g4.dimHash[dimId] = dimension
            } else {
                val dimension = Dimension(dimName, dimLength.toInt(), isUnlimited, true)
                g4.gb.addDimension(dimension)
                g4.dimHash[dimId] = dimension
            }
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
            vb.datatype = convertType(typeid)
            vb.dimensions.addAll(g4.makeDimList(dimIds))

            val usertype = if (typeid >= 32) userTypes[typeid] else null

            vb.spObject = Vinfo(g4, varid, typeid, usertype)

            // read Variable attributes
            if (natts > 0) {
                if (debug) println(" Variable $vname")
                val atts: List<Attribute.Builder> = readAttributes(session, g4.grpid, varid, natts)
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
    private fun readAttributes(session: MemorySession, grpid: Int, varid: Int, natts: Int): List<Attribute.Builder> {
        val result = mutableListOf<Attribute.Builder>()
        val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
        val type_p = session.allocate(C_INT, 0)
        val size_p = session.allocate(C_LONG, 0)
        for (attnum in 0 until natts) {
            checkErr("nc_inq_attname", nc_inq_attname(grpid, varid, attnum, name_p))

            checkErr("nc_inq_att", nc_inq_att(grpid, varid, name_p, type_p, size_p))
            val attName: String = name_p.getUtf8String(0)
            val attType = type_p[C_INT, 0]
            val attLength = size_p[C_LONG, 0]
            val datatype = convertType(attType)
            if (debug) println("  nc_inq_att $grpid $varid = $attName $datatype nelems=$attLength")

            val userType = userTypes[attType]
            if (userType != null) {
                result.add(readUserAttributeValues(session, grpid, varid, attName, datatype, userType, attLength))
            } else {
                val attb = Attribute.Builder().setName(attName).setDatatype(datatype)
                if (attLength > 0) {
                    attb.values = readAttributeValues(session, grpid, varid, attName, datatype, attLength)
                } else {
                    attb.values = emptyList<Any>()
                }
                result.add(attb)
            }

        }
        return result
    }

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
                        if (value.isNotEmpty()) result.add(value)
                    }
                }
                // nc_free_string() or does session handle this ??
                return result
            }

            else -> throw RuntimeException("Unsupported attribute data type == $datatype")
        }
    }

    internal class ConvertedType internal constructor(val dt: Datatype) {
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

    internal data class Vinfo(val g4: Group4, val varid: Int, val typeid: Int, val userType : UserType?)
}

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

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret = ${nc_strerror(ret).getUtf8String(0)}")
    }
}