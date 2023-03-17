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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class Hdf4ClibFile(val filename: String) : Iosp, Netcdf {
    private val header: HCheader = HCheader(filename)

    override fun rootGroup() = header.rootGroup

    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = "hdf4Clib"

    override fun close() {
        header.close()
    }

    // LOOK SDreadchunk ??
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filledSection = Section.fill(section, v2.shape)
        val vinfo = v2.spObject as Vinfo4

        val datatype = v2.datatype
        val nbytes = filledSection.size() * datatype.size

        if (vinfo.sdsIndex != null) {
            return readSDdata(header.sdsStartId, vinfo.sdsIndex!!, datatype, filledSection, nbytes)

        } else if (vinfo.vsInfo != null) {
            require(filledSection.rank() == 1)
            val startRecord = filledSection.origin(0)
            val numRecords = filledSection.shape(0)
            return readVSdata(header.fileOpenId, vinfo.vsInfo!!, datatype, startRecord, numRecords, nbytes)

        } else if (vinfo.grIndex != null) {
            return readGRdata(header.grStartId, vinfo.grIndex!!, datatype, filledSection, nbytes)
        }
        throw RuntimeException("cant read ${v2.name}")
    }

    companion object {
        var valueCharset: Charset = StandardCharsets.UTF_8
    }

}

fun readSDdata(sdsStartId: Int, sdindex: Int, datatype: Datatype, filledSection: Section, nbytes: Long): ArrayTyped<*> {
    val rank = filledSection.rank()

    MemorySession.openConfined().use { session ->
        val intArray = MemoryLayout.sequenceLayout(rank.toLong(), C_INT)
        val origin_p = session.allocateArray(intArray, rank.toLong())
        val shape_p = session.allocateArray(intArray, rank.toLong())
        val stride_p = session.allocateArray(intArray, rank.toLong())
        for (i in 0 until rank) {
            origin_p.setAtIndex(C_INT, i.toLong(), filledSection.origin(i))
            shape_p.setAtIndex(C_INT, i.toLong(), filledSection.shape(i))
            stride_p.setAtIndex(C_INT, i.toLong(), filledSection.stride(i))
        }
        val data_p = session.allocate(nbytes)

        val sds_id = SDselect(sdsStartId, sdindex)
        checkErr("SDreaddata", SDreaddata(sds_id, origin_p, stride_p, shape_p, data_p))
        SDendaccess(sds_id)

        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val values = ByteBuffer.wrap(raw)
        values.order(ByteOrder.LITTLE_ENDIAN) // LOOK ??

        return shapeData(datatype, values, filledSection.shape)
    }
}

fun readVSdata(
    fileOpenId: Int,
    vsInfo: VSInfo,
    datatype: Datatype,
    startRecord: Int,
    numRecords: Int,
    nbytes: Long
): ArrayTyped<*> {
    val shape = intArrayOf(numRecords)

    MemorySession.openConfined().use { session ->
        val read_access_mode = session.allocateUtf8String("r")
        val data_p = session.allocate(nbytes)
        val vdata_id = VSattach(fileOpenId, vsInfo.vs_ref, read_access_mode);

        // VSseek(int32 vdata_id, int32 record_pos)
        checkErrNeg("VSseek", VSseek(vdata_id, startRecord))
        // int32 VSread(int32 vdata_id, uint8 *databuf, int32 n_records, int32 interlace_mode)
        checkErr("VSread", VSread(vdata_id, data_p, numRecords, FULL_INTERLACE()))

        VSdetach(vdata_id)

        // As the data is stored contiguously in the vdata, VSfpack should be used to
        // unpack the fields after reading.

        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val values = ByteBuffer.wrap(raw)
        values.order(ByteOrder.LITTLE_ENDIAN) // LOOK ??
        return shapeData(datatype, values, shape)
    }
}

fun readGRdata(
            grStartId: Int,
            grIdx: Int,
            datatype: Datatype,
            filledSection: Section,
            nbytes: Long
): ArrayTyped<*> {
    val rank = filledSection.rank()

    MemorySession.openConfined().use { session ->
        // flip the shape
        val rank = filledSection.shape.size
        val flipShape = IntArray(rank) { filledSection.shape[rank - it - 1] }

        val intArray = MemoryLayout.sequenceLayout(rank.toLong(), C_INT)
        val origin_p = session.allocateArray(intArray, rank.toLong())
        val shape_p = session.allocateArray(intArray, rank.toLong())
        val stride_p = session.allocateArray(intArray, rank.toLong())
        for (i in 0 until rank) {
            origin_p.setAtIndex(C_INT, i.toLong(), filledSection.origin(i))
            shape_p.setAtIndex(C_INT, i.toLong(), flipShape[i])
            stride_p.setAtIndex(C_INT, i.toLong(), filledSection.stride(i))
        }
        val data_p = session.allocate(nbytes)

        val grId = GRselect(grStartId, grIdx)
        println("$grStartId $grIdx == $grId")
        checkErr("GRreadimage", GRreadimage(grId, origin_p, stride_p, shape_p, data_p))
        // checkErr("GRreadimage", GRreadimage(grId, origin_p, null, shape_p, data_p))
        GRendaccess(grId)

        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val values = ByteBuffer.wrap(raw)
        values.order(ByteOrder.LITTLE_ENDIAN) // LOOK ??

        return shapeData(datatype, values, filledSection.shape)
    }
}

private fun shapeData(datatype: Datatype, values: ByteBuffer, shape: IntArray): ArrayTyped<*> {
    return when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, values)
        Datatype.UBYTE -> ArrayUByte(shape, values)
        Datatype.CHAR, Datatype.STRING -> ArrayUByte(shape, values).makeStringsFromBytes()
        Datatype.DOUBLE -> ArrayDouble(shape, values.asDoubleBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, values.asFloatBuffer())
        Datatype.INT -> ArrayInt(shape, values.asIntBuffer())
        Datatype.UINT -> ArrayUInt(shape, values.asIntBuffer())
        Datatype.LONG -> ArrayLong(shape, values.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, values.asLongBuffer())
        Datatype.SHORT -> ArrayShort(shape, values.asShortBuffer())
        Datatype.USHORT -> ArrayUShort(shape, values.asShortBuffer())
        else -> throw IllegalArgumentException("datatype ${datatype}")
    }
}