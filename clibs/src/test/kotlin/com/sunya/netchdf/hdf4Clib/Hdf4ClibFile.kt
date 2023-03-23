package com.sunya.netchdf.hdf4Clib

/*
hdf4 library src:
/home/oem/dev/github/hdf4
install:
/home/oem/install/hdf4

cd /home/oem/install/jextract-19/bin

./jextract --source \
    --header-class-name mfhdf_h \
    --target-package com.sunya.netchdf.mfhdfClib.ffm \
    -I /home/oem/install/hdf4/include/mfhdf.h \
    -l /home/oem/install/hdf4/lib/libmfhdf.so \
    --output /home/oem/dev/github/cdm-kotlin/src/main/java \
    /home/oem/install/hdf4/include/mfhdf.h
 */

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*

import com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class Hdf4ClibFile(val filename: String) : Netchdf {
    private val header: HCheader = HCheader(filename)

    override fun rootGroup() = header.rootGroup

    override fun location() = filename
    override fun cdl() = cdl(this)
    override fun type() = "hdf4Clib"

    override fun close() {
        header.close()
    }

    // LOOK SDreadchunk ??
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filled = Section.fill(section, v2.shape)
        val vinfo = v2.spObject as Vinfo4

        val datatype = v2.datatype
        val nbytes = filled.size() * datatype.size

        if (vinfo.sdsIndex != null) {
            return readSDdata(header.sdsStartId, vinfo.sdsIndex!!, datatype, filled, nbytes)

        } else if (vinfo.vsInfo != null) {
            require(filled.rank() <= 1) { "variable = ${v2.name}"}
            val startRecord = if (filled.rank() == 0) 0 else filled.origin(0)
            val numRecords = if (filled.rank() == 0) 1 else filled.shape(0)
            return readVSdata(header.fileOpenId, vinfo.vsInfo!!, datatype, startRecord, numRecords)

        } else if (vinfo.grIndex != null) {
            return readGRdata(header.grStartId, vinfo.grIndex!!, datatype, filled, nbytes)

        }  else if (vinfo.svalue != null) {
            return ArrayString(intArrayOf(), listOf(vinfo.svalue!!))
        }
        throw RuntimeException("cant read ${v2.name}")
    }

    override fun chunkIterator(v2: Variable, section: Section?): Iterator<ArraySection>? {
        return null
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

fun readVSdata(fileOpenId: Int, vsInfo: VSInfo, datatype : Datatype, startRecord: Int, numRecords: Int): ArrayTyped<*> {
    val shape = intArrayOf(numRecords)

    MemorySession.openConfined().use { session ->
        val read_access_mode = session.allocateUtf8String("r")
        val fldnames_p = session.allocateUtf8String(vsInfo.fldNames)
        val data_p = session.allocate(numRecords * vsInfo.recsize.toLong()) // LOOK memory clobber?
        val vdata_id = VSattach(fileOpenId, vsInfo.vs_ref, read_access_mode);

        checkErrNeg("VSsetfields", VSsetfields(vdata_id, fldnames_p))
        checkErrNeg("VSseek", VSseek(vdata_id, startRecord))
        // int32 VSread(int32 vdata_id, uint8 *databuf, int32 n_records, int32 interlace_mode)
        val nread = VSread(vdata_id, data_p, numRecords, FULL_INTERLACE())
        checkErrNeg("VSread", nread)
        require(nread == numRecords)

        VSdetach(vdata_id)

        // As the data is stored contiguously in the vdata, VSfpack should be used to
        // unpack the fields after reading.

        val raw = data_p.toArray(ValueLayout.JAVA_BYTE)
        val values = ByteBuffer.wrap(raw)
        values.order(ByteOrder.LITTLE_ENDIAN) // LOOK ??

        if (datatype.typedef is CompoundTypedef) {
            val members = (datatype.typedef as CompoundTypedef).members
            return ArrayStructureData(shape, values, vsInfo.recsize, members)
        } else {
            // a single field is made into a regular variable
            return shapeData(datatype, values, shape)
        }
    }
}

fun readGRdata(
            grStartId: Int,
            grIdx: Int,
            datatype: Datatype,
            filledSection: Section,
            nbytes: Long
): ArrayTyped<*> {

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
        checkErr("GRreadimage", GRreadimage(grId, origin_p, stride_p, shape_p, data_p))
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