package sunya.cdm.netcdfClib

import sunya.cdm.api.Group
import sunya.cdm.api.Section
import sunya.cdm.api.Variable
import sunya.cdm.iosp.*
import sunya.cdm.netcdf.ffm.netcdf_h.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.*

class NetcdfClibFile(val filename : String) : Iosp {
    private val header : NCheader = NCheader(filename)
    private val rootGroup : Group = header.rootGroup.build(null)

    override fun rootGroup() = rootGroup

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*>  {
        TODO("Not yet implemented")
    }

    override fun readArrayData(v2: Variable): ArrayTyped<*> {
        val vinfo = v2.spObject as NCheader.Vinfo
        require(v2.nelems < Int.MAX_VALUE)

        MemorySession.openConfined().use { session ->
            val longArray = MemoryLayout.sequenceLayout(v2.rank.toLong(), C_LONG)
            val origin_p = session.allocateArray(longArray, v2.rank.toLong())
            val shape_p = session.allocateArray(longArray, v2.rank.toLong())
            val stride_p = session.allocateArray(longArray, v2.rank.toLong())
            for (i in 0 until v2.rank) {
                origin_p.setAtIndex(C_LONG, i.toLong(), 0L)
                shape_p.setAtIndex(C_LONG, i.toLong(), v2.shape[i].toLong())
                stride_p.setAtIndex(C_LONG, i.toLong(), 1L)
            }

            when (vinfo.typeid) {
                NC_BYTE() -> {
                    val val_p = session.allocate(v2.nelems)
                    checkErr("nc_get_vars_schar",
                        nc_get_vars_schar(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayByte(values, v2.shape)
                }

                NC_CHAR() -> {
                    val val_p = session.allocate(v2.nelems)
                    checkErr("nc_get_vars_text",
                        nc_get_vars_text(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayByte(values, v2.shape)
                }

                NC_DOUBLE() -> {
                    // can you allocate DoubleBuffer on heap directly?
                    val val_p = session.allocateArray(C_DOUBLE, v2.nelems)
                    checkErr("nc_get_vars_double",
                        nc_get_vars_double(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = DoubleBuffer.allocate(v2.nelems.toInt())
                    for (i in 0 until v2.nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_DOUBLE, i))
                    }
                    return ArrayDouble(values, v2.shape)
                }

                NC_FLOAT() -> {
                    val val_p = session.allocateArray(C_FLOAT, v2.nelems)
                    checkErr("nc_get_vars_float",
                        nc_get_vars_float(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = FloatBuffer.allocate(v2.nelems.toInt())
                    for (i in 0 until v2.nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_FLOAT, i))
                    }
                    return ArrayFloat(values, v2.shape)
                }

                NC_INT() -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_INT, v2.nelems)
                    checkErr("nc_get_vars_int",
                        nc_get_vars_int(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = IntBuffer.allocate(v2.nelems.toInt())
                    for (i in 0 until v2.nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_INT, i))
                    }
                    return ArrayInt(values, v2.shape)
                }

                NC_LONG() -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_LONG, v2.nelems)
                    checkErr("nc_get_vars_long",
                        nc_get_vars_long(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = LongBuffer.allocate(v2.nelems.toInt())
                    for (i in 0 until v2.nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_LONG, i))
                    }
                    return ArrayLong(values, v2.shape)
                }

                NC_SHORT() -> {
                    val val_p = session.allocateArray(C_SHORT, v2.nelems)
                    checkErr("nc_get_vars_short",
                        nc_get_vars_short(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = ShortBuffer.allocate(v2.nelems.toInt())
                    for (i in 0 until v2.nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                    }
                    return ArrayShort(values, v2.shape)
                }

                else -> throw IllegalArgumentException()
            }
        }
    }
}