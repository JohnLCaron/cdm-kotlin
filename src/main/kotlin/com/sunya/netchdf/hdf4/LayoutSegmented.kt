package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.iosp.IndexChunker
import com.sunya.cdm.iosp.Layout

/**
 * LayoutSegmented has data stored in segments.
 * Assume that each segment size is a multiple of elemSize.
 * Used by HDF4.
 * 
 * @param segPos starting address of each segment.
 * @param segSize number of bytes in each segment. Assume multiple of elemSize
 * @param elemSize size of an element in bytes.
 * @param srcShape shape of the entire data array.
 * @param wantSection the wanted section of data
 */
class LayoutSegmented(segPos: LongArray, segSize: IntArray, elemSize: Int, srcShape: IntArray, wantSection: Section?) :
    Layout {
    override val totalNelems: Long
    override val elemSize : Int // size of each element
    private val segPos : LongArray // bytes
    private val segMax : LongArray// bytes
    private val segMin : LongArray// bytes

    // outer chunk
    private val chunker: IndexChunker
    private var chunkOuter: IndexChunker.Chunk = IndexChunker.Chunk(0, 0, 0)

    // inner chunk = deal with segmentation
    private val chunkInner = IndexChunker.Chunk(0, 0, 0)
    private var done: Long
    private var needInner = 0
    private var doneInner = 0
    
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
            segMin[i] = totalBytes
            totalBytes += segSize[i].toLong()
            segMax[i] = totalBytes
        }
        require(totalBytes >= computeSize(srcShape) * elemSize)
        chunker = IndexChunker(srcShape, wantSection)
        totalNelems = chunker.totalNelems
        done = 0
        this.elemSize = elemSize
    }

    override fun hasNext(): Boolean {
        return done < totalNelems
    }

    ///////////////////
    private fun getFilePos(elem: Long): Long {
        var segno = 0
        while (elem >= segMax[segno]) segno++
        return segPos[segno] + elem - segMin[segno]
    }

    // how many more bytes are in this segment ?
    private fun getMaxBytes(start: Long): Int {
        var segno = 0
        while (start >= segMax[segno]) segno++
        return (segMax[segno] - start).toInt()
    }

    override fun next(): Layout.Chunk {
        var result: IndexChunker.Chunk
        if (needInner > 0) {
            result = nextInner(false, 0)
        } else {
            result = nextOuter()
            val nbytes = getMaxBytes(chunkOuter.srcElem * elemSize)
            if (nbytes < result.nelems * elemSize) result = nextInner(true, nbytes)
        }
        done += result.nelems
        doneInner += result.nelems
        needInner -= result.nelems
        if (debugNext) println(" next chunk: $result")
        return result
    }

    private fun nextInner(first: Boolean, nbytes: Int): IndexChunker.Chunk {
        if (first) {
            chunkInner.nelems = (nbytes / elemSize)
            chunkInner.destElem = chunkOuter.destElem
            needInner = chunkOuter.nelems
            doneInner = 0
        } else {
            chunkInner.incrDestElem(chunkInner.nelems) // increment using last chunks' value
            var chunkBytes = getMaxBytes((chunkOuter.srcElem + doneInner) * elemSize)
            chunkBytes = Math.min(chunkBytes, needInner * elemSize)
            chunkInner.nelems = (chunkBytes / elemSize) // set this chunk's value
        }
        chunkInner.srcPos = (getFilePos((chunkOuter.srcElem + doneInner) * elemSize))
        return chunkInner
    }

    fun nextOuter(): IndexChunker.Chunk {
        val chunkOuter = chunker.next()
        val srcPos = getFilePos(chunkOuter.srcElem * elemSize)
        chunkOuter.srcPos = (srcPos)
        return chunkOuter
    }

    companion object {
        private const val debugNext = false
    }
}