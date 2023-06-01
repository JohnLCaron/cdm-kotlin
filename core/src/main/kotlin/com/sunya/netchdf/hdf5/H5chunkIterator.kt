package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.transferMissingNelems
import java.nio.ByteBuffer

internal class H5chunkIterator<T>(val h5 : H5builder, val v2: Variable<T>, val wantSection : Section) : AbstractIterator<ArraySection<T>>() {
    private val debugChunking = false

    val vinfo : DataContainerVariable
    val h5type : H5TypeInfo
    val elemSize : Int
    val datatype : Datatype<*>
    val tiledData : H5TiledData
    val filters : H5filters
    val state : OpenFileState

    private val wantSpace : IndexSpace
    private val chunkIterator : Iterator<BTree1.DataChunkEntry>

    init {
        vinfo = v2.spObject as DataContainerVariable

        h5type = vinfo.h5type
        elemSize = vinfo.storageDims[vinfo.storageDims.size - 1].toInt() // last one is always the elements size
        datatype = h5type.datatype()

        val btreeNew = BTree1(h5, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
        tiledData = H5TiledData(btreeNew)
        filters = H5filters(v2.name, vinfo.mfp, h5type.endian)
        if (debugChunking) println(" H5chunkIterator tiles=${tiledData.tiling}")

        state = OpenFileState(0L, h5type.endian)
        wantSpace = IndexSpace(wantSection)
        chunkIterator = tiledData.dataChunks(wantSpace).iterator()
    }

    override fun computeNext() {
        if (chunkIterator.hasNext()) {
            setNext(getaPair(chunkIterator.next()))
        } else {
            done()
        }
    }

    private fun getaPair(dataChunk : BTree1.DataChunkEntry) : ArraySection<T> {
        val dataSpace = IndexSpace(v2.rank, dataChunk.key.offsets, vinfo.storageDims)

        // TODO we need to intersect the dataChunk with the wanted section.
        // optionally, we could make a view of the array, rather than copying the data.
        val useEntireChunk = wantSpace.contains(dataSpace)
        val intersectSpace = if (useEntireChunk) dataSpace else wantSpace.intersect(dataSpace)

        val bb = if (dataChunk.isMissing()) {
            if (debugChunking) println("   missing ${dataChunk.show(tiledData.tiling)}")
            val sizeBytes = intersectSpace.totalElements * elemSize
            val bbmissing = ByteBuffer.allocate(sizeBytes.toInt())
            bbmissing.order(vinfo.h5type.endian)
            transferMissingNelems(vinfo.fillValue, datatype, intersectSpace.totalElements.toInt(), bbmissing)
            if (debugChunking) println("   missing transfer ${intersectSpace.totalElements} fillValue=${vinfo.fillValue}")
            bbmissing
        } else {
            if (debugChunking) println("  chunkIterator=${dataChunk.show(tiledData.tiling)}")
            state.pos = dataChunk.childAddress
            val chunkData = h5.raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
            val filteredData = filters.apply(chunkData, dataChunk)
            if (useEntireChunk) {
                filteredData
            } else {
                val chunker = Chunker(dataSpace, wantSpace) // each DataChunkEntry has its own Chunker iteration
                chunker.transferBB(filteredData, elemSize, intersectSpace.totalElements.toInt())
            }
        }

        bb.position(0)
        bb.limit(bb.capacity())
        bb.order(h5type.endian)

        val array = if (h5type.datatype5 == Datatype5.Vlen) {
            h5.processVlenIntoArray(h5type, intersectSpace.shape.toIntArray(), bb, intersectSpace.totalElements.toInt(), elemSize)
        } else {
            h5.processDataIntoArray(bb, datatype, intersectSpace.shape.toIntArray(), h5type, elemSize)
        }

        return ArraySection(array as ArrayTyped<T>, intersectSpace.section(v2.shape)) // LOOK use space instead of Section ??
    }
}
