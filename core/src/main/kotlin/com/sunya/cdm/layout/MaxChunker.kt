package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize

/**
 * The case where you have a regular (non-chunked) layout, and you want to chunk it efficiently into
 * chunks of approx maxElems.
 * Used in Netchdf.chunkIterator() for non-chunked data.
 *
 * @param maxElems the approx size of the dataChunk
 * @param elemSize size in bytes of one element
 * @param wantSpace the requested section of data.
 * @param mergeFirst merge strategy for dimensions that can be merged and still keep contiguous transfer
 */
class MaxChunker(val maxElems: Int, val wantSpace: IndexSpace, varshape : IntArray) : AbstractIterator<IndexSpace>() {
    val totalNelems = wantSpace.totalElements
    val rank = wantSpace.rank
    val strider = IntArray(rank)
    val odo = Odometer(wantSpace, varshape)

    init {
        var accumStride = 1
        for (k in rank - 1 downTo 0) {
            strider[k] = accumStride
            accumStride *= wantSpace.shape[k]
        }
    }

    //// iterator

    private var done: Long = 0 // done so far
    private var first = true

    // start with the case where varshape == wantshape
    override fun computeNext() {
        if (done >= totalNelems) {
            return done()
        }

        val chunk = maxChunkShape(wantSpace.shape, odo.current)
        val sectionSpace = IndexSpace(odo.current.copyOf(), chunk)
        setNext(sectionSpace)

        done += chunk.computeSize()
        odo.setFromElement(done)
    }

    fun maxChunkShape(shape: IntArray, current: IntArray): IntArray {
        // always use the full length of the innermost dimension
        val chunkShape = IntArray(rank) { idx ->
            if (idx == rank - 1) shape[idx] else {
                var size: Int = (maxElems / strider[idx])
                size = if (size == 0) 1 else size
                Math.min(size, shape[idx] - current[idx])
            }
        }
        return chunkShape
    }
}