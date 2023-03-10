package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.iosp.IndexChunker
import com.sunya.cdm.iosp.Layout
import kotlin.math.min


/**
 * LayoutSegmented has data stored in sequential segments.
 * Each segment size must be a multiple of elemSize.
 * The total segment size may be large than the variable's total size.
 *
 * @param segPos starting address of each segment.
 * @param segSize number of bytes in each segment, multiple of elemSize
 * @param elemSize size of an element in bytes.
 * @param srcShape shape of the entire variables' data (in elements)
 * @param wantSection the wanted section of data (in elements)
 */
class LayoutSegmented(segPos: LongArray, segSize: IntArray, override val elemSize: Int, srcShape: IntArray, wantSection: Section?)
    : Layout {

    override val totalNelems: Long
    private val segPos : LongArray // bytes
    private val segMin : LongArray // elems
    private val segMax : LongArray // elems

    // outer chunk deals with the wanted section of data
    private val chunker = IndexChunker(srcShape, wantSection)
    private var chunkOuter: IndexChunker.Chunk = IndexChunker.Chunk(0, 0, 0) // fake

    // inner chunk = deal with segmentation
    private val chunkInner = IndexChunker.Chunk(0, 0, 0)
    private var done: Long = 0    // number elements done overall
    private var needOuter = 0 // remaining elements to do in the outer chunk

    init {
        require(segPos.size == segSize.size)
        this.segPos = segPos
        val nsegs = segPos.size
        segMin = LongArray(nsegs)
        segMax = LongArray(nsegs)
        var totalBytes: Long = 0
        for (i in 0 until nsegs) {
            require(segPos[i] >= 0)
            require(segSize[i] > 0)
            require(segSize[i] % elemSize == 0)
            segMin[i] = totalBytes / elemSize
            totalBytes += segSize[i].toLong()
            segMax[i] = totalBytes / elemSize
        }
        require(totalBytes >= computeSize(srcShape) * elemSize)
        totalNelems = chunker.totalNelems
    }

    override fun hasNext(): Boolean {
        return done < totalNelems
    }

    // get starting position in the file of the wanted element
    private fun getStartPos(wantElem: Long): Long {
        var segno = 0
        while (wantElem >= segMax[segno]) {
            segno++
        }
        return segPos[segno] + elemSize * (wantElem - segMin[segno])
    }

    // how many more elements are in the segment from startElm?
    private fun getMaxElemsInSeg(startElem: Long): Int {
        var segno = 0
        while (startElem >= segMax[segno]) segno++
        return (segMax[segno] - startElem).toInt()
    }

    override fun next(): Layout.Chunk {
        val innerChunk = if (needOuter <= 0) {
            chunkOuter = nextOuter()
            nextInner(true)
        } else {
            nextInner(false)
        }
        done += innerChunk.nelems
        needOuter -= innerChunk.nelems
        if (debugNext) println(" next chunk: $innerChunk")
        return innerChunk
    }

    private fun nextInner(first: Boolean): IndexChunker.Chunk {
        if (first) {
            val maxElemsInSeg = getMaxElemsInSeg(chunkOuter.srcElem)
            chunkInner.nelems = min(maxElemsInSeg, needOuter)
            chunkInner.destElem = chunkOuter.destElem
            chunkInner.srcElem = chunkOuter.srcElem
        } else {
            chunkInner.destElem += chunkInner.nelems // increment using last chunks' value
            chunkInner.srcElem += chunkInner.nelems
            val maxElemsInSeg = getMaxElemsInSeg(chunkInner.srcElem)
            chunkInner.nelems = min(maxElemsInSeg, needOuter)
        }
        chunkInner.srcPos = getStartPos(chunkInner.srcElem)
        return chunkInner
    }

    fun nextOuter(): IndexChunker.Chunk {
        val chunkOuter = chunker.next()
        chunkOuter.srcPos = getStartPos(chunkOuter.srcElem)
        needOuter = chunkOuter.nelems
        return chunkOuter
    }

    companion object {
        private const val debugNext = false
    }
}