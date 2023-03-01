package com.sunya.cdm.layout

/** Iterator through indices of multidimensional space. */
class Odometer(val section : IndexSpace, shape : IntArray) : Iterable<IntArray> {
    val rank = section.rank
    val current = section.start.copyOf()
    val limit = section.limit
    val totalElements = section.totalElements
    val strider : IntArray
    var done = 0

    init {
        strider = IntArray(rank)
        var accumStride = 1
        for (k in rank - 1 downTo 0) {
            strider[k] = accumStride
            accumStride *= shape[k]
        }
    }

    fun isDone(): Boolean  = (done >= totalElements)
    fun incr(): IntArray = current.incr()
    fun incr(incrdigit: Int): IntArray = current.incr(incrdigit)

    fun add(count: Int) {
        repeat(count) {incr() } // LOOK do something smarter
    }

    private fun setFromElement(element: Long) {
        var total = element
        for (dim in 0 until rank) {
            current[dim] = (total / strider[dim]).toInt()
            total -= (current[dim] * strider[dim]).toLong()
        }
    }

    // increment starting from the fastest digit
    fun IntArray.incr(): IntArray {
        var digit: Int = rank - 1
        while (digit >= 0) {
            this[digit]++
            if (this[digit] <= section.limit[digit]) break // normal exit
            this[digit] = section.start[digit] // else, carry
            digit--
        }
        done++
        return this
    }

    // increment starting from the given digit
    fun IntArray.incr(incrdigit : Int): IntArray {
        require(incrdigit in 0 until rank)
        var digit = incrdigit
        while (digit >= 0) {
            this[digit]++
            if (this[digit] <= section.limit[digit]) break // normal exit
            this[digit] = section.start[digit] // else, carry
            digit--
        }
        // LOOK we dont know when this is done. maybe this is an Index, not an odometer
        return this
    }

    fun element() : Long {
        return current.zip(strider).map { it.first.toLong() * it.second }.sum()
    }

    override fun iterator() = OdoIterator()
    override fun toString(): String {
        return "Odometer(section=$section, limit=${limit.contentToString()}, strider=${strider.contentToString()})"
    }

    inner class OdoIterator() : AbstractIterator<IntArray>() {
        var count = 0
        var first = true

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