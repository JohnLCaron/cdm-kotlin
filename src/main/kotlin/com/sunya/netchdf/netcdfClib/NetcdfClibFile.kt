package com.sunya.netchdf.netcdfClib

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Datatype.Companion.VLEN
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.netcdf4.ffm.netcdf_h.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.*

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

class NetcdfClibFile(val filename: String) : Iosp, Netcdf {
    private val header: NCheader = NCheader(filename)
    private val rootGroup: Group = header.rootGroup.build(null)

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl(strict: Boolean) = com.sunya.cdm.api.cdl(this, strict)

    override fun close() {
        // NOOP
    }

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = if (section == null) Section(v2.shape) else Section.fill(section, v2.shape)
        val nelems = wantSection.size()
        require(nelems < Int.MAX_VALUE)

        val vinfo = v2.spObject as NCheader.Vinfo
        val datatype = header.convertType(vinfo.typeid)
        if (datatype == VLEN) {
            println("vlen not supported")
            return ArrayString(intArrayOf(), emptyList())
        }
        MemorySession.openConfined().use { session ->
            val longArray = MemoryLayout.sequenceLayout(v2.rank.toLong(), C_LONG)
            val origin_p = session.allocateArray(longArray, v2.rank.toLong())
            val shape_p = session.allocateArray(longArray, v2.rank.toLong())
            val stride_p = session.allocateArray(longArray, v2.rank.toLong())
            for (i in 0 until v2.rank) {
                origin_p.setAtIndex(C_LONG, i.toLong(), wantSection.origin(i).toLong())
                shape_p.setAtIndex(C_LONG, i.toLong(), wantSection.shape(i).toLong())
                stride_p.setAtIndex(C_LONG, i.toLong(), wantSection.stride(i).toLong())
            }

            when (datatype) {
                Datatype.ENUM1 -> {
                    val nbytes = nelems * datatype.size
                    val val_p = session.allocate(nbytes)
                    checkErr("nc_get_var", nc_get_var(vinfo.g4.grpid, vinfo.varid, origin_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayByte(wantSection.shape, values)
                }

                Datatype.BYTE -> {
                    val val_p = session.allocate(nelems)
                    checkErr("nc_get_vars_schar",
                        nc_get_vars_schar(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayByte(wantSection.shape, values)
                }

                Datatype.UBYTE -> {
                    val val_p = session.allocate(nelems)
                    checkErr("nc_get_vars_uchar",
                        nc_get_vars_uchar(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayUByte(wantSection.shape, values)
                }

                Datatype.CHAR -> {
                    val val_p = session.allocate(nelems)
                    checkErr("nc_get_vars_text",
                        nc_get_vars_text(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    return ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
                }

                Datatype.DOUBLE -> {
                    // can you allocate DoubleBuffer on heap directly?
                    val val_p = session.allocateArray(C_DOUBLE, nelems)
                    checkErr("nc_get_vars_double",
                        nc_get_vars_double(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = DoubleBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_DOUBLE, i))
                    }
                    return ArrayDouble(wantSection.shape, values)
                }

                Datatype.FLOAT -> {
                    val val_p = session.allocateArray(C_FLOAT, nelems)
                    checkErr("nc_get_vars_float",
                        nc_get_vars_float(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = FloatBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_FLOAT, i))
                    }
                    return ArrayFloat(wantSection.shape, values)
                }

                Datatype.INT -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_INT, nelems)
                    checkErr("nc_get_vars_int",
                        nc_get_vars_int(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = IntBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_INT, i))
                    }
                    return ArrayInt(wantSection.shape, values)
                }

                Datatype.UINT -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_INT, nelems)
                    checkErr("nc_get_vars_uint",
                        nc_get_vars_uint(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = IntBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_INT, i))
                    }
                    return ArrayInt(wantSection.shape, values)
                }

                Datatype.LONG -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_LONG, nelems)
                    checkErr("nc_get_vars_long",
                        nc_get_vars_long(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = LongBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_LONG, i))
                    }
                    return ArrayLong(wantSection.shape, values)
                }

                Datatype.ULONG -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_LONG, nelems)
                    checkErr("nc_get_vars_ulonglong",
                        nc_get_vars_ulonglong(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = LongBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(JAVA_LONG, i))
                    }
                    return ArrayULong(wantSection.shape, values)
                }

                Datatype.SHORT -> {
                    val val_p = session.allocateArray(C_SHORT, nelems)
                    checkErr("nc_get_vars_short",
                        nc_get_vars_short(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = ShortBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                    }
                    return ArrayShort(wantSection.shape, values)
                }

                Datatype.USHORT -> {
                    val val_p = session.allocateArray(C_SHORT, nelems)
                    checkErr("nc_get_vars_ushort",
                        nc_get_vars_ushort(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = ShortBuffer.allocate(nelems.toInt())
                    for (i in 0 until nelems) {
                        values.put(i.toInt(), val_p.getAtIndex(ValueLayout.JAVA_SHORT, i))
                    }
                    return ArrayUShort(wantSection.shape, values)
                }

                else -> throw IllegalArgumentException("unsupported datatype ${datatype}")
            }
        }
    }
}