package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

class H4chunkReader(val h4 : H4builder) {

    private val debugChunkingDetail = false
    private val debugChunking = false
    private val debugMissing = false

    internal fun readChunkedDataNew(v2: Variable, wantSection : Section) : ArrayTyped<*> {
        val vinfo = v2.spObject as Vinfo
        val elemSize = vinfo.elemSize
        val datatype = v2.datatype

        val wantSpace = IndexSpace(wantSection)
        val sizeBytes = wantSpace.totalElements * elemSize
        if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
            throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
        }
        val bb = ByteBuffer.allocate(sizeBytes.toInt())
        bb.order(vinfo.endian)

        // prefill with fill value
        /* val sbb = bb.asShortBuffer()
        sbb.position(0)
        val fill = vinfo.fillValue as Short
        repeat(wantSpace.totalElements.toInt()) { bb.putShort(fill) } // performance ?? */

        val tiledData = H4tiledData(h4, v2.shape, vinfo.chunkLengths, vinfo.chunks!!)
        // val filters = H5filters(v2.name, vinfo.mfp, vinfo.endian)
        if (debugChunking) println(" ${tiledData.tiling}")

        var count = 0
        var transferChunks = 0
        for (dataChunk in tiledData.findDataChunks(wantSpace)) { // : Iterable<BTree1New.DataChunkEntry>
            val dataSection = IndexSpace(dataChunk.offsets, vinfo.chunkLengths)
            val chunker = Chunker(dataSection, elemSize, wantSpace)
            if (dataChunk.isMissing()) {
                if (debugMissing) println(" ${dataChunk.show(tiledData.tiling)}")
                chunker.transferMissing(vinfo.fillValue, datatype, bb)
            } else {
                if (debugChunkingDetail and (count < 1)) println(" ${dataChunk.show(tiledData.tiling)}")
                val filteredData = dataChunk.getByteBuffer()
                chunker.transfer(filteredData, bb)
                transferChunks += chunker.transferChunks
            }
            count++
        }

        bb.position(0)
        bb.limit(bb.capacity())

        val shape = wantSpace.nelems
        val result = when (datatype) {
            Datatype.BYTE -> ArrayByte(shape, bb)
            Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
            Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
            Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
            Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
            Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
            Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
            Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
            Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
            Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
            Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }
        return result
    }

}