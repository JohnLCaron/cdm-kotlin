package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.iosp.IndexChunker
import kotlin.math.min

/**
 * LayoutRegularSegmented has data stored in segments that are regularly spaced.
 * This is how Netcdf-3 "record variables" are laid out.
 *
 * @param startPos starting address of the entire data array.
 * @param elemSize size of an element in bytes.
 * @param recSize size of outer stride in bytes
 * @param srcShape shape of the entire data array. must have rank &gt; 0
 * @param wantSection the wanted section of data
*/
class LayoutRegularSegmented(val startPos: Long, override val elemSize: Int, val recSize: Long, srcShape: IntArray, wantSection: Section?) :
    Layout {
    override val totalNelems: Long
    private val innerNelems: Long

    // outer chunk
    private val chunker: IndexChunker
    private val chunkOuter = IndexChunker.Chunk(0, 0, 0) // LOOK can we get rid of this ??

    // inner chunk = deal with segmentation
    private val chunkInner: IndexChunker.Chunk = IndexChunker.Chunk(0, 0, 0) // LOOK can we get rid of this ??
    private var needInner = 0
    private var doneInner = 0
    private var done: Long

    init {
        require(startPos > 0)
        require(elemSize > 0)
        require(recSize > 0)
        require(srcShape.size > 0)
        chunker = IndexChunker(srcShape, wantSection)
        totalNelems = chunker.totalNelems
        innerNelems = if (srcShape[0] == 0) 0 else Section.computeSize(srcShape) / srcShape[0]
        done = 0
    }

    override fun hasNext(): Boolean {
        return done < totalNelems
    }

    private fun getFilePos(elem: Long): Long {
        val segno = elem / innerNelems
        val offset = elem % innerNelems
        return startPos + segno * recSize + offset * elemSize
    }

    // how many more elements are in this segment ?
    private fun getMaxElem(startElem: Long): Int {
        return (innerNelems - startElem % innerNelems).toInt()
    }

    override fun next(): Layout.Chunk {
        var result: IndexChunker.Chunk
        if (needInner > 0) {
            result = nextInner(false, 0)
        } else {
            result = nextOuter()
            val nelems = getMaxElem(result.srcElem)
            if (nelems < result.nelems) result = nextInner(true, nelems)
        }
        done += result.nelems
        doneInner += result.nelems
        needInner -= result.nelems
        if (debugNext) println(" next chunk: $result")
        return result
    }

    private fun nextInner(first: Boolean, nelems: Int): IndexChunker.Chunk {
        if (first) {
            chunkInner.nelems = nelems
            chunkInner.destElem = chunkOuter.destElem
            needInner = chunkOuter.nelems
            doneInner = 0
        } else {
            chunkInner.incrDestElem(chunkInner.nelems) // increment using last chunks' value
            val nnelems = getMaxElem(chunkOuter.srcElem + doneInner)
            chunkInner.nelems = min(nnelems, needInner)
        }
        chunkInner.srcElem = chunkOuter.srcElem + doneInner
        chunkInner.srcPos = getFilePos(chunkOuter.srcElem + doneInner)
        return chunkInner
    }

    fun nextOuter(): IndexChunker.Chunk {
        val nextChunk = chunker.next()
        nextChunk.srcPos = getFilePos(nextChunk.srcElem)
        return chunkOuter.set(nextChunk) // LOOK can we get rid of this ??
    }

    companion object {
        private const val debugNext = false
    }
}