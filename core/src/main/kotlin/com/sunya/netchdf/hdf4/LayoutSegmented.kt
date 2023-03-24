package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.Layout
import com.sunya.cdm.layout.LayoutChunk
import kotlin.math.min

/**
 * LayoutSegmented has data stored in an array of segments, stored in segPos\[i], segSize\[i]
 * Each segment size must be a multiple of elemSize.
 * The total segment size may be larger than the variable's total size.
 *
 * @param segPos starting address of each segment.
 * @param segSize number of bytes in each segment, multiple of elemSize
 * @param elemSize size of an element in bytes.
 * @param srcShape shape of the entire variables' data (in elements)
 * @param wantSection the wanted section of data (in elements)
 */
class LayoutSegmented(segPos: LongArray, segSize: IntArray, override val elemSize: Int, srcShape: IntArray, wantSection: IndexSpace)
    : Layout {

    override val totalNelems: Long
    private val segPos : LongArray // bytes
    private val segMin : LongArray // elems
    private val segMax : LongArray // elems

    // outer chunk deals with the wanted section of data
    private val chunker = Chunker(IndexSpace(srcShape), wantSection) // One  big chunk
    private var chunkOuter = LayoutChunk(0, 0, 0, 0)

    // inner chunk = deal with segmentation
    private var chunkInner = LayoutChunk(0, 0, 0, 0)
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
        this.chunkInner = if (needOuter <= 0) {
            chunkOuter = nextOuter()
            nextInner(true)
        } else {
            nextInner(false)
        }
        done += chunkInner.nelems
        needOuter -= chunkInner.nelems
        if (debugNext) println(" next chunk: $chunkInner")
        return chunkInner
    }

    private fun nextInner(first: Boolean): LayoutChunk {
        if (first) {
            val maxElemsInSeg = getMaxElemsInSeg(chunkOuter.srcElem)
            val nelems = min(maxElemsInSeg, needOuter)
            val destElem = chunkOuter.destElem
            val srcElem = chunkOuter.srcElem
            return LayoutChunk(getStartPos(srcElem), srcElem, nelems, destElem)
        } else {
            val destElem = chunkInner.nelems + chunkInner.destElem // increment using last chunks' value
            val srcElem = chunkInner.nelems  + chunkInner.srcElem
            val maxElemsInSeg = getMaxElemsInSeg(srcElem)
            val nelems = min(maxElemsInSeg, needOuter)
            return LayoutChunk(getStartPos(srcElem), srcElem, nelems, destElem)
        }
    }

    // LOOK assumes that chunks dont cross Segments.
    fun nextOuter(): LayoutChunk {
        val chunkOuter = chunker.next()
        val srcPos = getStartPos(chunkOuter.srcElem)
        needOuter = chunkOuter.nelems
        return LayoutChunk(srcPos, chunkOuter)
    }

    companion object {
        private const val debugNext = false
    }
}