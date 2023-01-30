package sunya.cdm.netcdfClib

import sunya.cdm.api.Attribute
import sunya.cdm.api.Dimension
import sunya.cdm.api.Group
import sunya.cdm.api.Variable
import sunya.cdm.iosp.Iosp
import sunya.cdm.netcdf.ffm.*
import sunya.cdm.netcdf.ffm.netcdf_h.*
import sunya.cdm.netcdf3.*
import sunya.cdm.netcdf3.N3header.Companion.getDataType
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.util.*

private val debug = false

fun main(args: Array<String>) {
    val h = NCheader("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc")
    if (debug) println(h.rootGroup.build().cdlString())
}

class NCheader(filename: String) {
    var filename: String? = null
    var session: MemorySession? = null
    var rootGroup = Group.Builder()
    var ncid = 0
    var format = 0

    init {
        this.filename = filename
        MemorySession.openConfined().use { session ->
            this.session = session
            rootGroup = Group.Builder()
            build(session, filename)
        }
    }

    @Throws(IOException::class)
    private fun build(session: MemorySession, filename: String) {
        val filenameSeg: MemorySegment = session.allocateUtf8String(filename)
        val fileHandle: MemorySegment = session.allocate(C_INT, 0)

        // nc_open(const char *path, int mode, int *ncidp);
        // public static int nc_open ( Addressable path,  int mode,  Addressable ncidp) {
        checkErr("nc_open", nc_open(filenameSeg, 0, fileHandle))
        this.ncid = fileHandle[C_INT, 0]
        if (debug) println("nc_open $filename fileHandle ${this.ncid}")

        // format
        val formatp: MemorySegment = session.allocate(C_INT, 0)
        checkErr("nc_inq_format", nc_inq_format(ncid, formatp))
        this.format = formatp[C_INT, 0]
        if (debug) println(" nc_inq_format = ${this.format}")

        // read root group
        makeGroup(session, Group4(ncid, rootGroup, null))
    }

    private fun makeGroup(session: MemorySession, g4: Group4) {
        // groupBuilderHash[g4.gb] = g4.grpid
        readGroupDimensions(session, g4)

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
            val ndims = ndims_p[C_INT, 0].toInt()
            val natts = natts_p[C_INT, 0].toInt()
            if (debug) println(" nc_inq_var $vname = $typeid $ndims $natts")

            // figure out the dimensions
            val dimIds = IntArray(ndims)
            for (i in 0 until ndims) {
                dimIds[i] = dimids_p.getAtIndex(C_INT, i.toLong())
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

            // nc_inq_att(int ncid, int varid, const char *name, nc_type *xtypep, size_t *lenp);
            //     public static int nc_inq_att ( int ncid,  int varid,  Addressable name,  Addressable xtypep,  Addressable lenp) {
            checkErr("nc_inq_att", nc_inq_att(grpid, varid, name_p, type_p, size_p))
            val attName: String = name_p.getUtf8String(0)
            val attType = type_p[C_INT, 0]
            val attLength = size_p[C_LONG, 0]
            val arrayType = getDataType(attType)
            if (debug) println("  nc_inq_att $grpid $varid = $attName $arrayType nelems=$attLength")

            val attb = Attribute.Builder().setName(attName).setDataType(arrayType)
            if (attLength > 0) {
                attb.values = readAttributeValues(session, grpid, varid, name_p, attType, attLength)
            }
            result.add(attb)

        }
        return result
    }

    fun readAttributeValues(session: MemorySession, grpid : Int, varid : Int, name_p : MemorySegment, type : Int, nelems : Long) : List<Any> {
        when (type) {

            NC_BYTE() -> {
                val val_p = session.allocate(nelems) // add 1 to make sure its zero terminated ??
                // nc_get_att_uchar(int ncid, int varid, const char *name, unsigned char *ip);
                //     public static int nc_get_att_uchar ( int ncid,  int varid,  Addressable name,  Addressable ip) {
                checkErr("nc_get_att_schar", nc_get_att_schar(grpid, varid, name_p, val_p))
                val result = mutableListOf<Byte>()
                val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                for (i in 0 until nelems.toInt()) {
                    result.add(raw[i])
                }
                return result
            }

            NC_CHAR() -> {
                val val_p = session.allocate(nelems+1) // add 1 to make sure its zero terminated ??
                // nc_get_att_uchar(int ncid, int varid, const char *name, unsigned char *ip);
                //     public static int nc_get_att_uchar ( int ncid,  int varid,  Addressable name,  Addressable ip) {
                checkErr("nc_get_att_text", nc_get_att_text(grpid, varid, name_p, val_p))
                val text: String = val_p.getUtf8String(0)
                return listOf(text)
            }

            NC_DOUBLE() -> {
                val val_p = session.allocateArray(C_DOUBLE, nelems)
                checkErr("nc_get_att_double", nc_get_att_double(grpid, varid, name_p, val_p))
                val result = mutableListOf<Double>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_DOUBLE, i))
                }
                return result
            }

            NC_FLOAT() -> {
                val val_p = session.allocateArray(C_FLOAT, nelems)
                checkErr("nc_get_att_float", nc_get_att_float(grpid, varid, name_p, val_p))
                val result = mutableListOf<Float>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_FLOAT, i))
                }
                return result
            }

            NC_INT() -> {
                val val_p = session.allocateArray(C_INT, nelems)
                checkErr("nc_get_att_int", nc_get_att_int(grpid, varid, name_p, val_p))
                val result = mutableListOf<Int>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_INT, i))
                }
                return result
            }


            NC_LONG() -> {
                val val_p = session.allocateArray(C_LONG, nelems)
                checkErr("nc_get_att_long", nc_get_att_long(grpid, varid, name_p, val_p))
                val result = mutableListOf<Long>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_LONG, i))
                }
                return result
            }

            NC_SHORT() -> {
                val val_p = session.allocateArray(C_SHORT, nelems)
                checkErr("nc_get_att_short", nc_get_att_short(grpid, varid, name_p, val_p))
                val result = mutableListOf<Short>()
                for (i in 0 until nelems) {
                    result.add(val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                }
                return result
            }

        }
        return emptyList()
    }

    internal class Group4(val grpid: Int, val gb: Group.Builder, val parent: Group4?) {
        var dimIds : IntArray? = null
        var udimIds : IntArray? = null
        val dimHash = mutableMapOf<Int, Dimension>()

        fun makeDimList(dimIds : IntArray) : List<Dimension> {
            return dimIds.map { dimHash[it]?: throw IllegalStateException() }
        }
    }

    internal data class Vinfo(val g4: Group4, val varid: Int, val typeid: Int)

    fun getIosp() : Iosp = NCiosp()

}

fun checkErr (where : String, ret: Int) {
    if (ret != 0) {
        throw IOException("$where return $ret = ${nc_strerror(ret).getUtf8String(0)}")
    }
}