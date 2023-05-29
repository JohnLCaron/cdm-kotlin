package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

class H4chunkReader(val h4 : H4builder) {

    private val debugChunkingDetail = false
    private val debugChunking = false
    private val debugMissing = false

    internal fun <T> readChunkedData(v2: Variable<T>, wantSection : Section) : ArrayTyped<T> {
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

        val tiledData = H4tiledData(h4, v2.shape, vinfo.chunkLengths, vinfo.chunks!!)
        if (debugChunking) println(" ${tiledData.tiling}")

        var count = 0
        var transferChunks = 0
        for (dataChunk in tiledData.findDataChunks(wantSpace)) { // : Iterable<BTree1New.DataChunkEntry>
            val dataSection = IndexSpace(v2.rank, dataChunk.offsets.toLongArray(), vinfo.chunkLengths.toLongArray())
            val chunker = Chunker(dataSection, wantSpace) // each dataChunk has its own Chunker iteration
            if (dataChunk.isMissing()) {
                if (debugMissing) println(" ${dataChunk.show(tiledData.tiling)}")
                chunker.transferMissing(vinfo.fillValue, datatype, elemSize, bb)
            } else {
                if (debugChunkingDetail and (count < 1)) println(" ${dataChunk.show(tiledData.tiling)}")
                val filteredData = dataChunk.getByteBuffer() // filter already applied
                chunker.transfer(filteredData, elemSize, bb)
                transferChunks += chunker.transferChunks
            }
            count++
        }

        bb.position(0)
        bb.limit(bb.capacity())

        val shape = wantSpace.shape.toIntArray()
        val result = when (datatype) {
            Datatype.BYTE -> ArrayByte(shape, bb)
            Datatype.STRING, Datatype.CHAR, Datatype.UBYTE -> ArrayUByte(shape, bb)
            Datatype.SHORT -> ArrayShort(shape, bb)
            Datatype.USHORT -> ArrayUShort(shape, bb)
            Datatype.INT -> ArrayInt(shape, bb)
            Datatype.UINT -> ArrayUInt(shape, bb)
            Datatype.FLOAT -> ArrayFloat(shape, bb)
            Datatype.DOUBLE -> ArrayDouble(shape, bb)
            Datatype.LONG -> ArrayLong(shape, bb)
            Datatype.ULONG -> ArrayULong(shape, bb)
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }
        return result as ArrayTyped<T>
    }

}