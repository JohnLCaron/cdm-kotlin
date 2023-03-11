package com.sunya.netchdf.hdf4Clib

/*
hdf4 library src:
/home/snake/dev/github/hdf4
install:
/home/snake/install/hdf4

cd /home/snake/install/jextract-19/bin

./jextract --source \
    --header-class-name mfhdf4_h \
    --target-package com.sunya.netchdf.hdf4Clib.ffm \
    -I /home/snake/install/hdf4/include/hdf.h \
    -l /home/snake/install/hdf4/lib/libdf.so \
    --output /home/snake/dev/github/cdm-kotlin/src/main/java \
    /home/snake/install/hdf4/include/hdf.h

./jextract --source \
    --header-class-name mfhdf_h \
    --target-package com.sunya.netchdf.mfhdfClib.ffm \
    -I /home/snake/install/hdf4/include/mfhdf.h \
    -l /home/snake/install/hdf4/lib/libmfhdf.so \
    --output /home/snake/dev/github/cdm-kotlin/src/main/java \
    /home/snake/install/hdf4/include/mfhdf.h

    ./jextract --source \
    --header-class-name hdf_h \
    --target-package com.sunya.netchdf.hdfClib.ffm \
    -I /home/snake/install/hdf4/include/hdf.h \
    -l /home/snake/install/hdf4/lib/libmfhdf.so \
    --output /home/snake/dev/github/cdm-kotlin/src/main/java \
    /home/snake/install/hdf4/include/hdf.h

 */


import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.Iosp

import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Hdf4ClibFile(val filename: String) : Iosp, Netcdf {
    private val header: HCheader = HCheader(filename)
    private val rootGroup: Group = header.rootGroup.build(null)

    override fun rootGroup() = rootGroup

    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = header.formatType

    override fun close() {
        header.close()
    }

    // LOOK SDreadchunk ??
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filledSection = Section.fill(section, v2.shape)

        val vinfo = v2.spObject as Vinfo4
        val datatype = v2.datatype
        val nbytes = filledSection.size() * datatype.size

        MemorySession.openConfined().use { session ->
            val intArray = MemoryLayout.sequenceLayout(v2.rank.toLong(), C_INT)
            val origin_p = session.allocateArray(intArray, v2.rank.toLong())
            val shape_p = session.allocateArray(intArray, v2.rank.toLong())
            val stride_p = session.allocateArray(intArray, v2.rank.toLong())
            for (i in 0 until v2.rank) {
                origin_p.setAtIndex(C_INT, i.toLong(), filledSection.origin(i))
                shape_p.setAtIndex(C_INT, i.toLong(), filledSection.shape(i))
                stride_p.setAtIndex(C_INT, i.toLong(), filledSection.stride(i))
            }
            val data_p = session.allocate(nbytes)

            val sds_id = SDselect(header.sd_id, vinfo.sds_index)
            checkErr("SDreaddata", SDreaddata(sds_id, origin_p, stride_p, shape_p, data_p))
            SDendaccess(sds_id)

            val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
            val values = ByteBuffer.wrap(raw)
            values.order(ByteOrder.LITTLE_ENDIAN) // LOOK ??

            return when (v2.datatype) {
                Datatype.BYTE -> ArrayByte(filledSection.shape, values)
                Datatype.UBYTE -> ArrayUByte(filledSection.shape, values)
                Datatype.CHAR, Datatype.STRING -> ArrayUByte(filledSection.shape, values).makeStringsFromBytes()
                Datatype.DOUBLE -> ArrayDouble(filledSection.shape, values.asDoubleBuffer())
                Datatype.FLOAT -> ArrayFloat(filledSection.shape, values.asFloatBuffer())
                Datatype.INT -> ArrayInt(filledSection.shape, values.asIntBuffer())
                Datatype.UINT -> ArrayUInt(filledSection.shape, values.asIntBuffer())
                Datatype.LONG -> ArrayLong(filledSection.shape, values.asLongBuffer())
                Datatype.ULONG -> ArrayULong(filledSection.shape, values.asLongBuffer())
                Datatype.SHORT -> ArrayShort(filledSection.shape, values.asShortBuffer())
                Datatype.USHORT -> ArrayUShort(filledSection.shape, values.asShortBuffer())
                else -> throw IllegalArgumentException("datatype ${v2.datatype}")
            }
        }
    }

}