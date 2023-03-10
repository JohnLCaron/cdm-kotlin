package com.sunya.netchdf.hdf4Clib

import com.sunya.cdm.api.*
import com.sunya.netchdf.hdf4.H4type
import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.io.IOException
import java.lang.foreign.*
import java.util.*

private val debug = true
private val MAX_NAME = 255L

// Really a builder of the root Group.
class HCheader(val filename: String) {
    val rootGroup = Group.Builder("")
    var formatType: String = ""

    internal var hcid = 0

    init {
        MemorySession.openConfined().use { session ->
            build(session)
        }
    }

    @Throws(IOException::class)
    private fun build(session: MemorySession) {
        val filenameSeg: MemorySegment = session.allocateUtf8String(filename)

        this.hcid = SDstart(filenameSeg, DFACC_READ())
        if (debug) println("SDstart $filename hcid=${this.hcid}")

        // read root group
        readGroup(session, Group4(hcid, rootGroup, null))
        // rootGroup.addAttribute(Attribute(Netcdf4.NCPROPERTIES, version))
    }

    fun close() {
        val ret = SDend(this.hcid)
        System.out.printf("SDend ret=%d %n", ret)
    }

    private fun readGroup(session: MemorySession, g4: Group4) {
        if (debug) println("readGroup '${g4.gb.name}'")

        // readGroupDimensions(session, g4)

        /* group attributes
        val numAtts_p = session.allocate(C_INT, 0)
        checkErr("nc_inq_natts", nc_inq_natts(g4.grpid, numAtts_p)) { "g4.grpid= ${g4.grpid}"}
        val numAtts = numAtts_p[C_INT, 0]

        if (numAtts > 0) {
            if (debug) println(" group attributes")
            val gatts: List<Attribute.Builder> = readAttributes(session, g4.grpid, NC_GLOBAL(), numAtts)
            for (attb in gatts) {
                val att = attb.build()
                g4.gb.addAttribute(att)
            }
        } */

        val nvars_p = session.allocate(C_INT, 0)
        val nattrs_p = session.allocate(C_INT, 0)
        checkErr("SDfileinfo", SDfileinfo(g4.grpid, nvars_p, nattrs_p))
        val nvars = nvars_p[C_INT, 0]
        val nattrs = nattrs_p[C_INT, 0]

        repeat(nvars) { readVariable(session, g4, it) }

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

    /*
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
    } */

    @Throws(IOException::class)
    private fun readVariable(session: MemorySession, g4: Group4, varidx: Int) {

        val varid = SDselect(g4.grpid, varidx)

        val name_p: MemorySegment = session.allocate(MAX_NAME)
        val rank_p = session.allocate(C_INT, 0)
        val dims_p = session.allocateArray(C_INT as MemoryLayout, 100)
        val datatype_p = session.allocate(C_INT, 0)
        val natts_p = session.allocate(C_INT, 0)

        // SDgetinfo(sds_id, sds_name, &rank, dim_sizes, &data_type, &n_attrs)
        // SDgetinfo ( int sdsid,  Addressable name,  Addressable rank,  Addressable dimsizes,  Addressable nt,  Addressable nattr) {
        checkErr("SDgetinfo", SDgetinfo(varid, name_p, rank_p, dims_p, datatype_p, natts_p))

        val vname: String = name_p.getUtf8String(0)
        val rank = rank_p[C_INT, 0]
        val dims = IntArray(rank) { dims_p.getAtIndex(ValueLayout.JAVA_INT, it.toLong()) }
        val datatype = datatype_p[C_INT, 0]
        val natts = natts_p[C_INT, 0]
        if (debug) println(" SDgetinfo $vname datatype=$datatype rank=$rank dims=${dims.contentToString()} natts=$natts")

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
            checkErr ("SDdiminfo", SDdiminfo(dimid, name_p, dimLength_p, datatype_p, natts_p))
            val dimName: String = name_p.getUtf8String(0)
            var dimLength = dimLength_p[C_INT, 0]
            val datatype = datatype_p[C_INT, 0] // wtf?
            val natts = natts_p[C_INT, 0] // wtf?
            val isUnlimited = (dimLength == 0)
            if (isUnlimited) {
                dimLength = dims[dimidx]
            }
            g4.gb.addDimensionIfNotExists(Dimension(dimName, dimLength, isUnlimited, true))
            dimList.add(dimName)
        }
        vb.dimList = dimList

        /* read Variable attributes
        if (natts > 0) {
            if (debug) println(" Variable attributes")
            val atts: List<Attribute.Builder> = readAttributes(session, g4.grpid, varid, natts)
            for (attb in atts) {
                val att = attb.build()
                vb.attributes.add(att)
            }
        } */
        g4.gb.addVariable(vb)

        SDendaccess(varid) // ??
    }


    /*
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
                        val tvalue = transcodeString(value)
                        result.add(value)
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

    internal class Group4(val grpid: Int, val gb: Group.Builder, val parent: Group4?) {
        var dimIds : IntArray? = null
        var udimIds : IntArray? = null
        val dimHash = mutableMapOf<Int, Dimension>()

        init {
            parent?.gb?.addGroup(gb)
        }

        fun makeDimList(dimIds : IntArray) : List<Dimension> {
            return dimIds.map {
                val dim = findDim(it)
                if (dim == null)
                    println("HEY")
                findDim(it) ?: Dimension("", it, false, false)
            }
        }

        fun findDim(dimId : Int) : Dimension? {
            return dimHash[dimId] ?: parent?.findDim(dimId)
        }

    }
*/
}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret")
    }
}

data class Group4(val grpid: Int, val gb: Group.Builder, val parent: Group4?)

data class Vinfo4(val g4: Group4, val sds_index: Int, val datatype : Int)

