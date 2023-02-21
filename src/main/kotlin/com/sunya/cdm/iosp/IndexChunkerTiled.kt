package com.sunya.cdm.iosp

import com.google.common.base.Preconditions
import com.sunya.cdm.api.Range
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import java.util.*

/**
 * Assume that the data is stored divided into sections, described by dataSection. All the data within a dataSection is
 * stored contiguously, in a regular layout. Assume dataSection strides must be = 1, that is, the stored data is not
 * strided.
 *
 *
 * The user asks for some section, wantSection (may have strides).
 * For each dataSection that intersects wantSection, a IndexChunkerTiled is created, which
 * figures out the optimal access pattern, based on reading contiguous runs of data. Each
 * IndexChunkerTiled handles only one dataSection. Typically the calling program loops over
 * all dataSections that intersect the wanted section.
 *
 *
 * Both dataSection and wantSection refer to the variable's overall shape.
 */
class IndexChunkerTiled(dataSection: Section, wantSection: Section) {
    private val dimList: MutableList<Dim> = ArrayList()
    private val dataIndex : IndexLong // Index into the data source section - used to calculate chunk.filePos
    private val resultIndex : IndexLong // Index into the data result section - used to calculate chunk.startElem
    private var chunk: IndexChunker.Chunk? = null // gets returned on next().
    private var nelems = 0 // number of elements to read at one time

    // Indexer methods
    val totalNelems: Long
    private var done: Long = 0
    private val startDestElem : Int // the offset in the result Array of this piece of it
    private val startSrcElem : Int // the offset in the source Array of this piece of it

    /**
     * Constructor.
     * Assume varSection.intersects(wantSection).
     *
     * @param dataSection the section of data we actually have. must have all ranges with stride = 1.
     * @param wantSection the wanted section of data, it will be intersected with dataSection.
     * dataSection.intersects(wantSection) must be true
     * @throws InvalidRangeException if ranges are malformed
     */
    init {

        // The actual wanted data we can get from this section
        val intersect: Section = dataSection.intersect(wantSection)
        totalNelems = intersect.computeSize()
        Preconditions.checkArgument(totalNelems > 0)
        val varRank: Int = intersect.rank()

        // create the List<Dim>
        // Section shifted = intersect.shiftOrigin(dataSection); // want reletive to dataSection
        var wantStride = 1
        var dataStride = 1
        for (ii in varRank - 1 downTo 0) {
            val dr: Range = dataSection.getRange(ii)!!
            val wr: Range = wantSection.getRange(ii)!!
            val ir: Range = intersect.getRange(ii)!!
            dimList.add(Dim(dr, wr, ir, dataStride, wantStride)) // note reversed : fastest first
            dataStride *= dr.length
            wantStride *= wr.length
        }

        // the offset in the result Array of this piece of it
        startDestElem = wantSection.offset(intersect)
        startSrcElem = dataSection.offset(intersect)
        if (debugStartingElems) println(" startDestElem=$startDestElem startSrcElem=$startSrcElem")

        // how many elements can we do at a time?
        if (varRank == 0) nelems = 1 else {
            val innerDim = dimList[0]
            nelems = innerDim.ncontigElements
            if (innerDim.ncontigElements > 1) {
                innerDim.wantNelems = 1 // 1 wantIndex increment = nelems
                innerDim.wantStride = innerDim.ncontigElements
            }
        }

        // we will use Index objects to keep track of the chunks
        val rank = dimList.size
        val dataStrides = LongArray(rank)
        val resultStrides = LongArray(rank)
        val shape = IntArray(rank)
        for (i in dimList.indices) { // reverse to slowest first
            val dim = dimList[i]
            dataStrides[rank - i - 1] = (dim.dataStride * dim.want.stride).toLong()
            resultStrides[rank - i - 1] = dim.wantStride.toLong() // * dim.want.stride();
            shape[rank - i - 1] = dim.wantNelems
        }
        dataIndex = IndexLong(shape, dataStrides)
        resultIndex = IndexLong(shape, resultStrides)

        // sanity checks
        val nchunks: Long = computeSize(shape)
        Preconditions.checkArgument(nchunks * nelems == totalNelems)
        if (debug) {
            println(
                "RegularSectionLayout total = " + totalNelems + " nchunks= " + nchunks + " nelems= " + nelems
                        + " dataSection= " + dataSection + " wantSection= " + wantSection + " intersect= " + intersect + this
            )
        }
    }

    private class Dim constructor(
        data: Range,
        want: Range,
        intersect: Range,
        dataStride: Int,
        wantStride: Int
    ) {
        val data : Range// Range we got
        val want : Range// Range we want
        val intersect : Range //  Range we want
        val dataStride : Int // stride in the data array
        var wantStride : Int // stride in the want array
        var wantNelems: Int
        val ncontigElements: Int

        init {
            this.data = data
            this.want = want
            this.intersect = intersect
            this.dataStride = dataStride
            this.wantStride = wantStride
            ncontigElements = if (intersect.stride == 1) intersect.length else 1
            wantNelems = intersect.length
            if (debugMerge) println("Dim=$this")
        }

        override fun toString(): String {
            return ("  data = " + data + " want = " + want + " intersect = " + intersect + " ncontigElements = "
                    + ncontigElements)
        }
    } // Dim

    operator fun hasNext(): Boolean {
        return done < totalNelems
    }

    operator fun next(): IndexChunker.Chunk {
        if (chunk == null) {
            chunk = IndexChunker.Chunk(0, nelems, startDestElem.toLong())
        } else {
            dataIndex.incr()
            resultIndex.incr()
        }

        // Set the current element's index from the start of the data array
        chunk!!.srcElem = startSrcElem + dataIndex.currentElement()

        // Set the current element's index from the start of the result array
        chunk!!.destElem = startDestElem + resultIndex.currentElement()
        if (debugNext) println(" chunk: $chunk")
        if (debugDetail) {
            println(" dataIndex: $dataIndex")
            println(" wantIndex: $resultIndex")
        }
        done += nelems.toLong()
        return chunk!!
    }

    ////////////////////
    override fun toString(): String {
        val f = Formatter()
        for (elem in dimList) {
            f.format("%s%n", elem)
        }
        return f.toString()
    }

    companion object {
        private const val debug = false
        private const val debugMerge = false
        private const val debugDetail = false
        private const val debugNext = false
        private const val debugStartingElems = false
    }
}