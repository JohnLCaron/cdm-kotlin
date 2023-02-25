package com.sunya.cdm.iosp

import com.sunya.cdm.api.Range
import com.sunya.cdm.api.Section
import java.util.*

/**
 * Finds contiguous chunks of data, used by Layout implementations, not exposed in the Layout API.
 * The iteration is monotonic in both src and dest positions.

 * @param srcShape the shape of the source, eg Variable.getShape()
 * @param want the wanted section in srcShape, must be subset of srcShape; if null, use varShape
 */
class IndexChunker(srcShape: IntArray, want: Section?) : Iterator<Layout.Chunk> {
    private val dimList = mutableListOf<Dim>()
    private var chunkIndex : IndexLong // each element is one chunk; strides track position in source
    private var chunk: Chunk? = null // gets returned on next().
    private var nelems = 0 // number of elements to read at one time
    private var start: Long

    val totalNelems: Long // total number of elements in wantSection
    private var done: Long // done so far

    init {
        // will throw InvalidRangeException if illegal section
        val wantSection= Section.fill(want, srcShape)
        totalNelems = wantSection.computeSize()
        done = 0
        start = 0

        // see if this is a "want all of it", so its a single chunk
        if (wantSection.equivalent(srcShape)) {
            nelems = totalNelems.toInt()
            chunkIndex = IndexLong()

        } else {
            // create the List<Dim> tracking each dimension
            val varRank = srcShape.size
            var stride: Long = 1
            for (ii in varRank - 1 downTo 0) {
                dimList.add(Dim(stride, srcShape[ii].toLong(), wantSection.getRange(ii)!!)) // note reversed : fastest first
                stride *= srcShape[ii].toLong()
            }

            // merge contiguous inner dimensions for efficiency
            if (debugMerge) println("merge= $this")

            // count how many merge dimensions
            var merge = 0
            for (i in 0 until dimList.size - 1) {
                val elem = dimList[i]
                val elem2 = dimList[i + 1]
                if (elem.maxSize == elem.wantSize.toLong() && elem2.want.stride == 1) {
                    merge++
                } else {
                    break
                }
            }

            // merge the dimensions
            for (i in 0 until merge) {
                val elem = dimList[i]
                val elem2 = dimList[i + 1]
                elem2.maxSize *= elem.maxSize
                elem2.wantSize *= elem.wantSize
                require(elem2.wantSize >= 0) { "array size may not exceed 2^31" }
                if (debugMerge) println(" ----$this")
            }

            // delete merged
            if (merge > 0) {
                dimList.subList(0, merge).clear()
            }
            if (debug) println(" final= $this")

            // how many elements can we do at a time?
            if (varRank == 0 || dimList[0].want.stride > 1) nelems = 1 else {
                val innerDim = dimList[0]
                nelems = innerDim.wantSize
                innerDim.wantSize =
                    1 // inner dimension has one element of length nelems (we dont actually need this here)
            }
            start = 0 // first wanted value
            for (dim in dimList) {
                start += dim.stride * dim.want.first() // watch for overflow on large files
            }

            // we will use an IndexLong to keep track of the chunks, each index represents nelems
            val rank = dimList.size
            val wstride = LongArray(rank)
            val shape = IntArray(rank)
            for (i in 0 until rank) {
                val dim = dimList[i]
                wstride[rank - i - 1] = dim.stride * dim.want.stride // reverse to slowest first
                shape[rank - i - 1] = dim.wantSize
            }
            if (debug) {
                System.out.printf("  elemsPerChunk=%d  nchunks=%d ", nelems, IndexLong.computeSize(shape))
                System.out.printf("  indexShape=%s%n", shape.contentToString())
                System.out.printf("  indexStride=%s%n", wstride.contentToString())
            }
            chunkIndex = IndexLong(shape, wstride)

            // sanity check
            require(IndexLong.computeSize(shape) * nelems == totalNelems)
            if (debug) {
                println("Index2= $this")
                println(" start= " + start + " varShape= " + Arrays.toString(srcShape) + " wantSection= " + wantSection)
            }
        }
    }

    private class Dim constructor( // number of elements
        val stride: Long,
        var maxSize: Long,  // number of elements
        val want: Range
    ) {
        var wantSize : Int // keep separate from want so we can modify when merging

        init {
            wantSize = want.length
        }
    }

    /** If there are more chunks to process  */
    override operator fun hasNext(): Boolean {
        return done < totalNelems
    }

    /** Get the next chunk  */
    override operator fun next(): IndexChunker.Chunk {
        if (chunk == null) {
            chunk = Chunk(start, nelems, 0)
        } else {
            chunkIndex.incr() // increment one element, which represents one chunk = nelems * sizeElem
            chunk!!.incrDestElem(nelems) // always read nelems at a time
        }

        // Get the current element's index from the start of the file
        chunk!!.srcElem = start + chunkIndex.currentElement()
        if (debugNext) println(" next chunk: $chunk")
        done += nelems.toLong()
        return chunk!!
    }

    /**
     * A chunk of data that is contiguous in both the source and destination.
     * Everything is done in elements, not bytes.
     * Read nelems from src at srcPos, store in destination at destPos.
     */
    class Chunk(
        var srcElem : Long, // start reading/writing here in the file
        var nelems: Int,  // read these many contiguous elements
        var destElem: Long // start writing/reading here in array
    ) : Layout.Chunk {
        // must be set by controlling Layout class - not used here
        var srcPos: Long = 0

        override fun srcElem() = srcElem
        override fun srcPos() = srcPos
        override fun nelems() = nelems
        override fun destElem() = destElem

        fun incrDestElem(incr: Int) {
            destElem += incr.toLong()
        }

        fun set(from: Chunk) : Chunk { // LOOK should create a new Index from this. probably
            this.srcElem = from.srcElem
            this.nelems = from.nelems
            this.destElem = from.destElem
            this.srcPos = from.srcPos
            return this
        }

        override fun toString(): String {
            return " srcPos=$srcPos srcElem=$srcElem nelems=$nelems destElem=$destElem"
        }
    }

    override fun toString(): String {
        return buildString {
            append("wantSize=${dimList.map {it.wantSize}}")
            append(" maxSize=${dimList.map {it.maxSize}}")
            append(" wantStride=${dimList.map {it.want.stride}}")
            append(" stride=${dimList.map {it.stride}}")
        }
    }

    companion object {
        private const val debug = false
        private const val debugMerge = false
        private const val debugNext = false
    }
}