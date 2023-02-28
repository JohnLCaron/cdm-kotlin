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
class Chunker(dataChunkRaw: IndexSpace, val elemSize: Int, wantSection: Section) : AbstractIterator<TransferChunk>() {
    val rank : Int
    val nelems : Int // number of elements to read at one time
    val totalNelems: Long // total number of elements in wantSection

    private val srcOdometer : Odometer
    private val dstOdometer : Odometer
    // private val mergedIntersect : IndexSpace
    // private val dataShape : IntArray

    init {
        val wantSpace = IndexSpace(wantSection)
        val intersectSpace = wantSpace.intersect(dataChunkRaw)

        // shift intersect to dataChunk
        val dataChunkShifted = intersectSpace.shift(dataChunkRaw.start) // dataChunk origin
        val wantSectionShifted = intersectSpace.shift(wantSection.origin) // wantSection origin

        this.srcOdometer = Odometer(dataChunkShifted, dataChunkRaw.nelems)
        this.dstOdometer = Odometer(wantSectionShifted, wantSection.shape)

        // TODO merge TODO stride

        this.rank = intersectSpace.rank
        this.totalNelems = intersectSpace.totalElements
        this.nelems = intersectSpace.nelems[rank - 1]


        /* first shift to dataSubset origin
        val dataSubsetShifted = Section(wantSection.shape) // throw away the offset in the variable's index space
        //val dataChunkShifted = dataChunkRaw.shift(wantSection.origin) // shift to that origin

        // here is the subset of dataChunkRaw we want to copy
        val intersectSection3 = dataSubsetShifted.intersect(dataChunkShifted.makeSection())
        val intersectSpace3 = IndexSpace(intersectSection)
        println("intersect section = ${intersectSection} space = ${intersectSpace}")
        intersectSpace.start.forEach { require(it >= 0) }

        this.totalNelems = intersectSection.computeSize()
        if (totalNelems == 0L) {
            println("doesnt intersect")
        }

        // merge dimensions if possible
        // val (mergedIntersect, mergedShape) = merge(intersectSpace, wantSection.shape)
        this.destOdometer = Odometer(mergedIntersect)
        this.srcOdometer = Odometer(IndexSpace()) // ??
        require(totalNelems == mergedIntersect.totalElements)

        this.rank = mergedIntersect.rank
        this.nelems = mergedIntersect.nelems[rank - 1]
        val wtf3 = mergedIntersect.element(mergedIntersect.start, mergedShape)
        val wtf = intersectSpace.element(intersectSpace.start, wantSection.shape)
        this.srcOffset = intersectSpace.element(intersectSpace.start, wantSection.shape)
        this.dataShape = mergedShape
        this.mergedIntersect = mergedIntersect

         */
    }

    // intersect not 0 based
    fun merge(intersect : IndexSpace, dataSubsetShape : IntArray) : Pair<IndexSpace, IntArray> {
        val rank = dataSubsetShape.size
        var mergeDims = 0 // how many dimensions can be merged?
        var accumDims = 1 // the length of the merged dimensions
        for (idx in rank-1 downTo 0) {
            accumDims *= intersect.nelems[idx]
            if (intersect.nelems[idx] == dataSubsetShape[idx]) {
                mergeDims++
            } else {
                break
            }
        }
        if (mergeDims == 0) {
            return Pair(intersect, dataSubsetShape)
        }
        val newRank = max(rank - mergeDims, 1)
        val newIntersectShape = IntArray(newRank) { idx ->
            if (idx < newRank - 1) intersect.nelems[idx] else accumDims
        }
        val newIntersectStart = IntArray(newRank) { intersect.start[it] }
        val newIntersect = IndexSpace(newIntersectStart, newIntersectShape)
        val newDataShape = IntArray(newRank) { idx ->
            if (idx < newRank - 1) dataSubsetShape[idx] else accumDims
        }
        return Pair(newIntersect, newDataShape)
    }

    //// iterator, TODO strides

    private var done: Long = 0 // done so far
    private var first = true

    override fun computeNext() {
        if (done >= totalNelems) {
            return done()
        }
        if (!first and (rank > 1)) {
            srcOdometer.incr(rank - 2)
            dstOdometer.incr(rank - 2)
        }
        val srcElem = srcOdometer.element()
        val dstElem = dstOdometer.element()
        //println(" srcElem = ${srcOdometer.current.contentToString()} = ${srcElem}")
        //println(" dstElem = ${dstOdometer.current.contentToString()} = ${dstElem}")
        setNext(TransferChunk(srcElem, nelems, dstElem))

        done += nelems.toLong()
        first = false
    }
}