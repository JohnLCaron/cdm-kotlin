package com.sunya.cdm.layout

/** Iterator through indices of multidimensional space. */
class Odometer(val section : IndexSpace, shape : IntArray) : Iterable<IntArray> {
    val rank = section.rank
    val current = section.start.copyOf()
    val limit = section.limit
    val nelems = section.totalElements
    val strider : IntArray
    var done = false

    init {
        strider = IntArray(rank)
        var accumStride = 1
        for (k in rank - 1 downTo 0) {
            strider[k] = accumStride
            accumStride *= shape[k]
        }
    }

    fun isDone(): Boolean  = done
    fun incr(): IntArray = current.incr()
    fun incr(incrdigit: Int): IntArray = current.incr(incrdigit)

    // increment starting from the fastest digit
    fun IntArray.incr(): IntArray {
        var digit: Int = rank - 1
        while (digit >= 0) {
            this[digit]++
            if (this[digit] <= section.limit[digit]) break // normal exit
            this[digit] = section.start[digit] // else, carry
            digit--
        }
        if (digit < 0) done = true
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
        if (digit < 0) done = true
        return this
    }

    fun element() : Long = element(current, strider)

    fun element(pt : IntArray, strider : IntArray) : Long {
        var element = 0L
        for (k in rank - 1 downTo 0) {
            element += pt[k] * strider[k]
        }
        return element
    }



    override fun iterator() = OdoIterator()
    override fun toString(): String {
        return "Odometer(section=$section, limit=${limit.contentToString()}, strider=${strider.contentToString()})"
    }

    inner class OdoIterator() : AbstractIterator<IntArray>() {
        var count = 0
        var first = true

        override fun computeNext() {
            if (count >= nelems) {
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