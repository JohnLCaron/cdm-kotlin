package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import java.lang.Integer.max

/**
 * from iosp.IndexChunker
 * Finds contiguous chunks of data to copy from dataChunk to destination
 * The iteration is monotonic in both src and dest positions.

 * @param dataChunkRaw the dataChunk index space, may have a trailing dimension that is ignored
 * @param wantSection the requested section of data
 */
class Chunker(dataChunkRaw: IndexSpace, val elemSize: Int, wantSpace: IndexSpace, merge : Boolean = true) : AbstractIterator<TransferChunk>() {
    val nelems : Int // number of elements to read at one time
    val totalNelems: Long // total number of elements in wantSection

    private val srcOdometer : Odometer
    private val dstOdometer : Odometer
    private val incrDigit : Int
    var transferChunks = 0

    init {
        val intersectSpace = wantSpace.intersect(dataChunkRaw)

        // shift intersect to dataChunk and wantSection origins
        val dataChunkShifted = intersectSpace.shift(dataChunkRaw.start) // dataChunk origin
        val wantSectionShifted = intersectSpace.shift(wantSpace.start) // wantSection origin

        // construct odometers over source and destination index spaces
        this.srcOdometer = Odometer(dataChunkShifted, dataChunkRaw.nelems)
        this.dstOdometer = Odometer(wantSectionShifted, wantSpace.nelems)
        this.totalNelems = intersectSpace.totalElements

        val rank = intersectSpace.rank
        val mergeDims = countMergeDims(intersectSpace, dataChunkRaw.nelems, wantSpace.nelems, merge)
        // the first dimension to merge
        val firstDim = if (rank == mergeDims) 0 else rank - mergeDims - 1

        var product = 1
        for (idx in rank-1 downTo firstDim) { product *= intersectSpace.nelems[idx] }
        this.nelems = product

        // the digit to increment when iterating
        this.incrDigit = if (firstDim == 0) 0 else firstDim - 1
    }

    fun countMergeDims(intersect : IndexSpace, dataChunkShape : IntArray, dataSubsetShape : IntArray, merge : Boolean) : Int {
        if (!merge) return 0

        var mergeDims = 0 // how many dimensions can be merged?
        for (idx in intersect.rank - 1 downTo 0) {
            if ((intersect.nelems[idx] == dataChunkShape[idx]) and (intersect.nelems[idx] == dataSubsetShape[idx])) {
                mergeDims++
            } else {
                break
            }
        }
        return mergeDims
    }

    //// iterator, TODO strides

    private var done: Long = 0 // done so far
    private var first = true

    override fun computeNext() {
        if (done >= totalNelems) {
            return done()
        }
        if (!first) {
            srcOdometer.incr(incrDigit)
            dstOdometer.incr(incrDigit)
        }
        val srcElem = srcOdometer.element()
        val dstElem = dstOdometer.element()
        //println(" srcElem = ${srcOdometer.current.contentToString()} = ${srcElem}")
        //println(" dstElem = ${dstOdometer.current.contentToString()} = ${dstElem}")
        setNext(TransferChunk(srcElem, nelems, dstElem))

        done += nelems.toLong()
        first = false
        transferChunks++
    }

    override fun toString(): String {
        return "Chunker(nelems=$nelems, elemSize=$elemSize totalNelems=$totalNelems, dstOdometer=$dstOdometer)"
    }
}