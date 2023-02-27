package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section

class Odometer(val section : Section) : Iterable<IntArray> {
    val rank = section.rank()
    val current = IntArray(rank) { section.origin(it) }
    val limit = section.limit
    var done = false

    fun isDone(): Boolean  = done
    fun incr(incrdigit: Int): IntArray = current.incr(incrdigit)

    override fun iterator() : Iterator<IntArray> = OdoIterator()

    private inner class OdoIterator() : AbstractIterator<IntArray>() {
        val currentOdo = IntArray(rank) { section.origin(it) }
        val nelems = section.computeSize().toInt()
        var count = 0

        override fun computeNext() {
            if (count >= nelems) {
                return done()
            }
            count++
            val result = intArrayOf(*currentOdo)
            currentOdo.incr()
            setNext(result)
        }
    }

    fun IntArray.last(): Boolean {
        for (digit in 0 until rank) {
            if (this[digit] < limit[digit] - 1) {
                return false
            }
        }
        return true
    }

    // increment starting from the fastest digit
    fun IntArray.incr(): IntArray {
        var digit: Int = rank - 1
        while (digit >= 0) {
            this[digit]++
            if (this[digit] < section.limit[digit]) break // normal exit
            this[digit] = section.origin[digit] // else, carry
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
            if (this[digit] < section.limit[digit]) break // normal exit
            this[digit] = section.origin[digit] // else, carry
            digit--
        }
        if (digit < 0) done = true
        return this
    }
}