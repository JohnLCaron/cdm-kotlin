package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

class H5chunkReader(val h5 : H5builder) {

    private val debugChunkingDetail = false
    private val debugChunking = false
    private val debugMissing = false

    internal fun readChunkedDataNew(v2: Variable, wantSection : Section) : ArrayTyped<*> {
        val vinfo = v2.spObject as DataContainerVariable
        val h5type = vinfo.h5type

        if ((h5type.hdfType == Datatype5.Vlen) or (h5type.hdfType == Datatype5.Compound)) {
            val layout = if (vinfo.mfp != null) H5tiledLayoutBB(h5, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
            else H5tiledLayout(h5, v2, wantSection, v2.datatype)
            when (h5type.hdfType) {
                Datatype5.Vlen -> return h5.readVlenData(vinfo, layout, wantSection)
                Datatype5.Compound -> return h5.readCompoundData(vinfo, layout, wantSection)
                // Datatype5.Reference -> return readReferenceData(h5type, layout, wantSection)
                else -> { } // fall through
            }
        }

        val elemSize = vinfo.storageDims.get(vinfo.storageDims.size - 1) // last one is always the elements size
        val datatype = vinfo.h5type.datatype(h5)

        val wantSpace = IndexSpace(wantSection)
        val sizeBytes = wantSpace.totalElements * elemSize
        if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
            throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
        }
        val bb = ByteBuffer.allocate(sizeBytes.toInt())
        bb.order(vinfo.h5type.endian)

        // prefill with fill value
        /* val sbb = bb.asShortBuffer()
        sbb.position(0)
        val fill = vinfo.fillValue as Short
        repeat(wantSpace.totalElements.toInt()) { bb.putShort(fill) } // performance ?? */

        val btreeNew =  BTree1New(h5, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
        val tiledData = TiledData(btreeNew)
        val filters = H5filters(v2.name, vinfo.mfp, vinfo.h5type.endian)
        if (debugChunking) println(" ${tiledData.tiling}")

        var count = 0
        var transferChunks = 0
        val state = OpenFileState(0L, vinfo.h5type.endian)
        for (dataChunk in tiledData.findDataChunks(wantSpace)) { // : Iterable<BTree1New.DataChunkEntry>
            val dataSection = IndexSpace(dataChunk.key.offsets, vinfo.storageDims)
            val chunker = Chunker(dataSection, elemSize, wantSpace)
            if (dataChunk.isMissing()) {
                if (debugMissing) println(" ${dataChunk.show(tiledData.tiling)}")
                chunker.transferMissing(vinfo.fillValue, datatype, bb)
            } else {
                if (debugChunkingDetail and (count < 1)) println(" ${dataChunk.show(tiledData.tiling)}")
                state.pos = dataChunk.childAddress
                val chunkData = h5.raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
                val filteredData = filters.apply(chunkData, dataChunk)
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
        if (h5type.hdfType == Datatype5.String) {
            return (result as ArrayUByte).makeStringsFromBytes()
        }
        if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
            return ArrayString(shape, h5.convertReferencesToDataObjectName(result as ArrayLong))
        }
        return result
    }
}