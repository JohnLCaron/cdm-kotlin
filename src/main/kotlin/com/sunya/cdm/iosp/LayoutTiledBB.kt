package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import java.io.IOException
import java.nio.*

/**
 * For datasets where the data are stored in chunks, and must be processed, eg compressed or filtered.
 * The data is read, processed, and placed in a ByteBuffer. Chunks have an offset into the ByteBuffer.
 * "Tiled" means that all chunks are assumed to be equal size.
 * Chunks do not necessarily cover the array, missing data is possible.
 * Used by HDF4 and HDF5.
 */
class LayoutTiledBB(
    val chunkIterator: DataChunkIterator, // iterator over all data chunks
    val chunkSize: IntArray, // all chunks assumed to be the same size
    override val elemSize: Int, // size of an element in bytes.
    val wantSection: Section // the wanted section of data, contains a List of Range objects. Must be complete.
) : Layout {

    // track the overall iteration
    override val totalNelems: Long
    private var totalNelemsDone: Long
    private var index: IndexChunkerTiled? = null // iterate within a chunk
    private var next: Chunk? = null

    init {
        if (debug) println(" want section=$wantSection")
        totalNelems = wantSection.computeSize()
        totalNelemsDone = 0
    }

    override fun hasNext(): Boolean { // have to actually fetch the thing
        if (totalNelemsDone >= totalNelems) return false
        if (index == null || !index!!.hasNext()) { // get new data node
            var dataSection: Section
            var dataChunk: DataChunk
            while (true) { // look for intersecting sections
                if (!chunkIterator.hasNext()) {
                    next = null
                    return false
                }

                // get next dataChunk
                try {
                    dataChunk = chunkIterator.next()
                } catch (e: IOException) {
                    e.printStackTrace()
                    next = null
                    return false
                }

                // make the dataSection for this chunk
                dataSection = Section(dataChunk.offset, chunkSize)
                if (debugIntersection) println(" test intersecting: $dataSection wantSection: $wantSection")
                if (dataSection.intersects(wantSection)) // does it intersect ?
                    break
            }
            if (debug) println(
                " found intersecting dataSection: $dataSection intersect= ${dataSection.intersect(wantSection)}"
            )
            val expectedLengthBytes = dataSection.computeSize().toInt() * elemSize
            index = IndexChunkerTiled(dataSection, wantSection) // new indexer into this chunk
            next = Chunk(dataChunk.getByteBuffer(expectedLengthBytes)) // this does the uncompression
        }
        val chunk = index!!.next()
        totalNelemsDone += chunk.nelems
        next!!.setDelegate(chunk)
        return true
    }

    override fun next(): LayoutBB.Chunk {
        return next!!
    }

    override fun toString(): String {
        return buildString {
            append("wantSection=$wantSection; ")
            append("chunkSize= ${chunkSize.contentToString()}")
            append(" totalNelems=$totalNelems")
            append(" elemSize=$elemSize")
        }
    }

    interface DataChunkIterator {
        operator fun hasNext(): Boolean

        @Throws(IOException::class)
        operator fun next(): DataChunk
    }

    interface DataChunk {
        val offset: IntArray

        @Throws(IOException::class)
        fun getByteBuffer(expectedSizeBytes: Int): ByteBuffer
    }

    /**
     * A chunk of data that is contiguous in both the source and destination.
     * Everything is done in elements, not bytes.
     * Read nelems from src at srcPos, store in destination at destPos.
     */
    class Chunk(override val byteBuffer: ByteBuffer) : LayoutBB.Chunk {
        private var delegate: IndexChunker.Chunk? = null

        fun setDelegate(delegate: IndexChunker.Chunk) {
            this.delegate = delegate
        }

        override fun srcPos(): Long = delegate!!.srcPos
        override fun nelems() = delegate!!.nelems
        override fun srcElem() = delegate!!.srcElem
        override fun destElem() : Long = delegate!!.destElem
        override fun toString() = delegate.toString()
    }

    companion object {
        private const val debug = false
        private const val debugIntersection = false
    }
}