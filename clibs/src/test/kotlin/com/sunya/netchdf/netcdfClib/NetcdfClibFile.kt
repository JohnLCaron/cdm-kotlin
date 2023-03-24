package com.sunya.netchdf.netcdfClib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.MaxChunker
import com.sunya.netchdf.netcdfClib.ffm.netcdf_h.*
import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
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

cd /home/oem/install/jextract-19/bin

netcdf library version 4.9.2-development of Mar 19 2023 10:42:31
./jextract --source \
    --header-class-name netcdf_h \
    --target-package sunya.cdm.netcdf4.ffm \
    -I /usr/include/netcdf.h \
    -l /usr/lib/x86_64-linux-gnu/libnetcdf.so \
    --output /home/oem/dev/github/cdm-kotlin/src/main/java \
    /usr/include/netcdf.h

./jextract --source \
    --header-class-name netcdf_h \
    --target-package com.sunya.netchdf.netcdfClib.ffm \
    -I /home/oem/install/netcdf4/include/netcdf.h \
    -l /home/oem/install/netcdf4/lib/libnetcdf.so \
    --output /home/oem/dev/github/cdm-kotlin/src/main/java \
    /home/oem/install/netcdf4/include/netcdf.h
 */

class NetcdfClibFile(val filename: String) : Netchdf {
    private val header: NCheader = NCheader(filename)
    private val rootGroup: Group = header.rootGroup.build(null)

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = header.formatType

    override fun close() {
        // NOOP
    }

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)
        val nelems = wantSection.size()
        require(nelems < Int.MAX_VALUE)

        val vinfo = v2.spObject as NCheader.Vinfo
        val datatype = convertType(vinfo.typeid)
        val userType = userTypes[vinfo.typeid]

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
                Datatype.VLEN -> {
                    val basetype = convertType(userType!!.baseTypeid)
                    // an array of vlen structs. each vlen has an address and a size
                    val vlen_p = com.sunya.netchdf.netcdfClib.ffm.nc_vlen_t.allocateArray(nelems.toInt(), session)
                    checkErr("vlen nc_get_vars", nc_get_vars(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, vlen_p))

                    // each vlen pointer is the address of the vlen array of length arraySize
                    val listOfVlen = mutableListOf<Array<*>>()
                    for (elem in 0 until nelems) {
                        val arraySize = com.sunya.netchdf.netcdfClib.ffm.nc_vlen_t.getLength(vlen_p, elem).toInt()
                        val address = com.sunya.netchdf.netcdfClib.ffm.nc_vlen_t.getAddress(vlen_p, elem)
                        listOfVlen.add( readVlenArray(arraySize, address, basetype))
                    }
                    return ArrayVlen(wantSection.shape, listOfVlen, basetype)
                    // TODO nc_free_vlen(nc_vlen_t *vl);
                    //      nc_free_string(size_t len, char **data);
                }

                Datatype.COMPOUND -> {
                    requireNotNull(userType)
                    requireNotNull(datatype.typedef)
                    require(datatype.typedef is CompoundTypedef)

                    val nbytes = nelems * userType.size // LOOK relation of userType.size to datatype.size ??
                    val val_p = session.allocate(nbytes)
                    checkErr("compound nc_get_vars", nc_get_vars(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val bb = ByteBuffer.wrap(raw)
                    bb.order(ByteOrder.LITTLE_ENDIAN)

                    val members = (datatype.typedef as CompoundTypedef).members
                    val sdataArray = ArrayStructureData(wantSection.shape, bb, userType.size, members)
                    // strings vs array of strings, also duplicate readCompoundAttValues
                    sdataArray.putStringsOnHeap {  offset ->
                        val address = val_p.get(ValueLayout.ADDRESS, (offset).toLong())
                        address.getUtf8String(0)
                    }
                    sdataArray.putVlensOnHeap { member, offset ->
                        // look duplicate (maybe)
                        val listOfVlen = mutableListOf<Array<*>>()
                        for (elem in 0 until member.nelems) {
                            val arraySize = val_p.get(ValueLayout.JAVA_LONG, (offset).toLong()).toInt()
                            val address = val_p.get(ValueLayout.ADDRESS, (offset + 8).toLong())
                            listOfVlen.add( readVlenArray(arraySize, address, member.datatype.typedef!!.baseType))
                        }
                        ArrayVlen(member.dims, listOfVlen, member.datatype)
                    }
                    return sdataArray
                }

                Datatype.ENUM1, Datatype.ENUM2, Datatype.ENUM4 -> {
                    val nbytes = nelems * datatype.size
                    val val_p = session.allocate(nbytes)
                    // int 	nc_get_var (int ncid, int varid, void *ip)
                    // 	Read an entire variable in one call.
                    // nc_get_vara (int ncid, int varid, const size_t *startp, const size_t *countp, void *ip)
                    checkErr("enum nc_get_vars", nc_get_vars(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val values = ByteBuffer.wrap(raw)
                    with (datatype.typedef as EnumTypedef) {
                        when (datatype) {
                            Datatype.ENUM1 -> return ArrayUByte(wantSection.shape, values).convertEnums()
                            Datatype.ENUM2 -> return ArrayUShort(wantSection.shape, values.asShortBuffer()).convertEnums()
                            Datatype.ENUM4 -> return ArrayUInt(wantSection.shape, values.asIntBuffer()).convertEnums()
                            else -> throw RuntimeException()
                        }
                    }
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
                    return ArrayUInt(wantSection.shape, values)
                }

                Datatype.LONG -> {
                    // nc_get_vars_int(int ncid, int varid, const size_t *startp, const size_t *countp, const ptrdiff_t *stridep, int *ip);
                    val val_p = session.allocateArray(C_LONG as MemoryLayout, nelems)
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
                    val val_p = session.allocateArray(C_LONG  as MemoryLayout, nelems)
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

                Datatype.STRING -> {
                    val val_p = session.allocateArray(ValueLayout.ADDRESS, nelems)
                    checkErr("nc_get_vars_string",
                        nc_get_vars_string(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val values = mutableListOf<String>()
                    for (i in 0 until nelems) {
                        values.add(val_p.getAtIndex(ValueLayout.ADDRESS, i).getUtf8String(0))
                    }
                    return ArrayString(wantSection.shape, values)
                }

                Datatype.OPAQUE -> {
                    val val_p = session.allocate(nelems * userType!!.size)
                    checkErr("opaque nc_get_var", nc_get_vars(vinfo.g4.grpid, vinfo.varid, origin_p, shape_p, stride_p, val_p))
                    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
                    val bb = ByteBuffer.wrap(raw)
                    return ArrayOpaque(wantSection.shape, bb, userType.size)
                }

                else -> throw IllegalArgumentException("unsupported datatype ${datatype}")
            }
        }
    }

    override fun chunkIterator(v2: Variable, section: Section?, maxElements : Int?): Iterator<ArraySection> {
        val filled = Section.fill(section, v2.shape)
        return NCmaxIterator(v2, filled, maxElements ?: 100_000)
    }

    private inner class NCmaxIterator(val v2: Variable, wantSection : Section, maxElems: Int) : AbstractIterator<ArraySection>() {
        private val debugChunking = false
        private val maxIterator  = MaxChunker(maxElems,  IndexSpace(wantSection), v2.shape)

        override fun computeNext() {
            if (maxIterator.hasNext()) {
                val indexSection = maxIterator.next()
                if (debugChunking) println("  chunk=${indexSection}")

                val section = indexSection.section()
                val array = readArrayData(v2, section)
                setNext(ArraySection(array, section))
            } else {
                done()
            }
        }
    }
}

private fun readVlenArray(arraySize : Int, address : MemoryAddress, datatype : Datatype) : Array<*> {
    return when (datatype) {
        Datatype.FLOAT -> Array(arraySize) { idx -> address.getAtIndex(JAVA_FLOAT, idx.toLong()) }
        Datatype.DOUBLE -> Array(arraySize) { idx -> address.getAtIndex(JAVA_DOUBLE, idx.toLong()) }
        Datatype.BYTE, Datatype.UBYTE, Datatype.ENUM1 -> Array(arraySize) { idx -> address.get(JAVA_BYTE, idx.toLong()) }
        Datatype.SHORT, Datatype.USHORT, Datatype.ENUM2 -> Array(arraySize) { idx -> address.getAtIndex(JAVA_SHORT, idx.toLong()) }
        Datatype.INT,  Datatype.UINT, Datatype.ENUM4 -> Array(arraySize) { idx -> address.getAtIndex(JAVA_INT, idx.toLong()) }
        Datatype.LONG, Datatype.ULONG -> Array(arraySize) { idx -> address.getAtIndex(JAVA_LONG, idx.toLong()) }
        else -> throw IllegalArgumentException("unsupported datatype ${datatype}")
    }
}