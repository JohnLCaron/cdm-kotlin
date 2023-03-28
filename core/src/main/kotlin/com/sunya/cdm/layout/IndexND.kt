package com.sunya.cdm.layout

/**
 * Translate between 1D "element" and nD "index".
 * Keeps "current index" state.
 * Seperately, it provides iterators over the nD indices, aka an "odometer",
 * @param section : a section of the entire datashape.
 * @param datashape : The datashape. May have an extra dimension, which is ignored.
 */
class IndexND(val section : IndexSpace, datashape : IntArray) : Iterable<IntArray> {
    val rank = section.rank
    val current = section.start.copyOf()
    val totalElements = section.totalElements
    val strider : LongArray

    init {
        repeat(rank) {  idx ->
            require( section.start[idx] + section.shape[idx] <= datashape[idx])
        }

        strider = LongArray(rank)
        var accumStride = 1L
        for (k in rank - 1 downTo 0) {
            strider[k] = accumStride
            accumStride *= datashape[k]
        }
    }

    fun incr(incrdigit: Int): IntArray = current.incr(incrdigit)

    /** Get the 1D element from the current nD index. */
    fun element() : Long {
        var total = 0L
        for (idx in 0 until rank) { total += strider[idx] * current[idx] }
        return total
    }

    /** Set the nD index from the 1D element. Return current index. */
    fun set(element: Long) : IntArray {
        var total = element
        for (dim in 0 until rank) {
            current[dim] = (total / strider[dim]).toInt()
            total -= (current[dim] * strider[dim])
        }
        return current
    }

    // increment the fastest digit
    private fun IntArray.incr(): IntArray {
        var digit: Int = rank - 1
        while (digit >= 0) {
            this[digit]++
            if (this[digit] <= section.last[digit]) break // normal exit
            this[digit] = section.start[digit] // else, carry
            digit--
        }
        return this
    }

    // increment the given digit
    private fun IntArray.incr(incrdigit : Int): IntArray {
        require(incrdigit in 0 until rank)
        var digit = incrdigit
        while (digit >= 0) {
            this[digit]++
            if (this[digit] <= section.last[digit]) break // normal exit
            this[digit] = section.start[digit] // else, carry
            digit--
        }
        // LOOK we dont know when this is done. maybe this is an Index, not an odometer
        return this
    }

    override fun toString(): String {
        return "Odometer(section=$section, limit=${section.last.contentToString()}, strider=${strider.contentToString()})"
    }

    /** An iterator over the full nD index space. */
    override fun iterator() : Iterator<IntArray> = Odometer()
    private inner class Odometer() : AbstractIterator<IntArray>() {
        var count = 0
        var first = true

        init {
            section.start.forEachIndexed { idx, start -> current[idx] = start }
        }

        override fun computeNext() {
            if (count >= totalElements) {
                return done()
            }
            if (!first) {
                current.incr()
            }
            count++
            setNext(intArrayOf(*current))
            first = false
        }
    }
}