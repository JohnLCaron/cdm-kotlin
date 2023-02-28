package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import java.io.IOException

/**
 * For datasets where the data are stored in chunks.
 * "Tiled" means that all chunks are assumed to be equal size.
 * Chunks have an offset into the complete array.
 * Chunks do not necessarily cover the array, missing data is possible.
 * Used by HDF4 and HDF5.
 * LOOK maybe all shou use LayoutTiledBB? more efficient if "reversed" chunking
 */
class LayoutTiled(val chunkIterator: DataChunkIterator, val chunkSize: IntArray, override val elemSize: Int,
                  wantSection: Section) : Layout {
    private var want: Section
    private var index: IndexChunkerTiled? = null // iterate within a chunk
    private var startSrcPos: Long = 0

    // track the overall iteration
    override val totalNelems: Long
    private var totalNelemsDone: Long
    private var next: Layout.Chunk? = null

    /**
     * Constructor.
     *
     * @param chunkIterator iterator over all available data chunks
     * @param chunkSize all chunks assumed to be the same size
     * @param elemSize size of an element in bytes.
     * @param wantSection the wanted section of data, contains a List of Range objects. Must be complete
     */
    init {
        want = wantSection
        if (want.isVariableLength) {
            // remove the varlen
            val newrange = ArrayList(want.ranges)
            newrange.removeAt(newrange.size - 1)
            want = Section(newrange)
        }
        totalNelems = want.computeSize()
        totalNelemsDone = 0
    }

    override fun hasNext(): Boolean { // have to actually fetch the thing here
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
                    if (debug) System.out.printf(" dataChunk: %s%n", dataSection)
                    if (dataSection.intersects(want)) // does it intersect ?
                        break
                }
                if (debug) println(" found intersecting section: " + dataSection + " for filePos " + dataChunk.filePos)
                index = IndexChunkerTiled(dataSection, want)
                startSrcPos = dataChunk.filePos
        }
        val chunk: IndexChunker.Chunk = index!!.next()
        totalNelemsDone += chunk.nelems
        chunk.srcPos = startSrcPos + chunk.srcElem * elemSize
        next = chunk
        return true
    }

    override fun next(): Layout.Chunk {
        if (debugNext) println("  next=$next")
        return next!!
    }

    override fun toString(): String {
        return buildString {
            append("want=$want; ")
            append("chunkSize=${chunkSize.contentToString()}")
            append(" totalNelems=$totalNelems elemSize=$elemSize")
        }
    }

    /** An iterator over DataChunk's  */
    interface DataChunkIterator {
        operator fun hasNext(): Boolean

        @Throws(IOException::class)
        operator fun next(): DataChunk
    }

    /** The chunks of a tiled layout.  */
    class DataChunk(
        val offset: IntArray, // offset index of this chunk, relative to entire array
        val filePos: Long // filePos of a single raw data chunk
    )

    companion object {
        private const val debug = false
        private const val debugNext = false
    }
}