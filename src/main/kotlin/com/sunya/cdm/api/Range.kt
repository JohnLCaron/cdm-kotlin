package com.sunya.cdm.api

import java.util.*

/**
 * A strided subset of the interval of positive integers [first(), last()] inclusive.
 * For example Range(1:11:3) represents the set of integers {1,4,7,10}
 * <p>
 * Elements must be nonnegative and unique.
 * EMPTY is the empty Range.
 * SCALAR is the set {0}.
 * VLEN is for variable length dimensions.
 * <p>
 * Standard iteration is
 *
 * <pre>
 *  for (val idx in range) {
 *    ...
 *  }
 * </pre>
 */

data class Range(
        val length : Int, // number of elements
        val first : Int, // first value in range
        val last : Int, // last value in range, inclusive
        val stride : Int, // stride, must be >= 1
        val name : String? = null, // optional name
    ) : Iterable<Int> {

    init {
        require(first >= 0) {"first ($first) must be >= 0" }
        require(stride >= 1) { "stride ($stride) must be >= 1" }
        if (name != "VLEN" && name != "EMPTY") {
            require(last >= first) { "last ($last) must be >= first ($first)" }
        }
    }

    /**
     * Create a range starting at zero, with unit stride.
     *
     * @param length number of elements in the Range
     */
    constructor(length: Int) : this(length, 0, length - 1, 1)

    /**
     * Create a range with unit stride.
     *
     * @param first first value in range
     * @param last last value in range, inclusive
     * @throws InvalidRangeException elements must be nonnegative, 0  first  last
     */
    constructor(first: Int, last: Int) : this(null, first, last, 1)

    /**
     * Create a range with a specified first, last, stride.
     *
     * @param first first value in range
     * @param last last value in range, inclusive
     * @param stride stride between consecutive elements, must be &gt; 0
     * @throws InvalidRangeException elements must be nonnegative: 0  first  last, stride &gt; 0
     */
    constructor(first: Int, last: Int, stride: Int) : this(null, first, last, stride)

    /**
     * Create a named range with a specified name, first, last, stride.
     *
     * @param name name of Range
     * @param first first value in range
     * @param last last value in range, inclusive
     * @param stride stride between consecutive elements, must be &gt; 0
     * @throws InvalidRangeException elements must be nonnegative: 0  first  last, stride &gt; 0
     */
    @JvmOverloads
    constructor(name: String?, first: Int, last: Int, stride: Int = 1) :
            this(1 + (last - first) / stride, first, (last / stride) * stride, stride, name)

    /** Make a copy with a different stride.  */
    fun copyWithStride(stride: Int): Range {
        return if (stride == this.stride) this
        else Range(this.first, this.last, stride)
    }

    /** Make a copy with a different name.  */
    fun copyWithName(name: String): Range {
        return if (name == this.name) this
        else Range(this.length, this.first, this.last, this.stride, name)
    }

    /////////////////////////////////////////////
    /**
     * Is want contained in this Range?
     *
     * @param want index in the original Range
     * @return true if the ith element would be returned by the Range iterator
     */
    fun contains(want: Int): Boolean {
        if (want < first) return false
        if (want > last) return false
        return if (stride == 1) true else (want - first) % stride == 0
    }

    /**
     * Create a new Range by composing a Range that is reletive to this Range.
     *
     * @param r range elements are reletive to this
     * @return combined Range, may be EMPTY
     * @throws InvalidRangeException elements must be nonnegative, 0  first  last
     */
    @Throws(InvalidRangeException::class)
    fun compose(r: Range): Range {
        if (this.length == 0 || r.length == 0) {
            return EMPTY
        }
        if (this === VLEN || r === VLEN) {
            return VLEN
        }
        /*
     * if(false) {// Original version
     * // Note that this version assumes that range r is
     * // correct with respect to this.
     * int first = element(r.first());
     * int stride = stride() * r.stride();
     * int last = element(r.last());
     * return new Range(name, first, last, stride);
     * } else {//new version: handles versions all values of r.
     */
        val sr_stride = stride * r.stride
        val sr_first = element(r.first) // MAP(this,i) == element(i)
        val lastx = element(r.last)
        val sr_last = Math.min(last, lastx)
        return Range(name, sr_first, sr_last, sr_stride)
    }

    /**
     * Create a new Range by compacting this Range by removing the stride.
     * first = first/stride, last=last/stride, stride=1.
     *
     * @return compacted Range
     * @throws InvalidRangeException elements must be nonnegative, 0  first  last
     */
    @Throws(InvalidRangeException::class)
    fun compact(): Range {
        if (stride == 1) return this
        val first = first / stride
        val last = first + length - 1
        return Range(name, first, last, 1)
    }

    /**
     * Get ith element
     *
     * @param i index of the element
     * @return the i-th element of a range.
     * @throws InvalidRangeException i must be: 0  i &lt; length
     */
    @Throws(InvalidRangeException::class)
    fun element(i: Int): Int {
        if (i < 0) {
            throw InvalidRangeException("element idx ($i) must be >= 0")
        }
        if (i >= length) {
            throw InvalidRangeException("element idx ($i) must be < length")
        }
        return first + i * stride
    }

    /**
     * Given an element in the Range, find its index in the array of elements.
     *
     * @param want the element of the range
     * @return index
     * @throws InvalidRangeException if illegal elem
     */
    @Throws(InvalidRangeException::class)
    fun index(want: Int): Int {
        if (want < first) throw InvalidRangeException("elem must be >= first")
        val result = (want - first) / stride
        if (result > length) throw InvalidRangeException("elem must be &le; first = n * stride")
        return result
    }

    /**
     * Create a new Range by intersecting with another Range. One of them must have stride 1.
     *
     * @param other range to intersect..
     * @return intersected Range, may be EMPTY
     */
    @Throws(InvalidRangeException::class)
    fun intersect(other: Range): Range {
        if (this.length == 0 || other.length == 0) {
            return EMPTY
        }
        if (this === VLEN || other === VLEN) {
            return VLEN
        }
        val first = Math.max(this.first, other.first)
        val last = Math.min(this.last, other.last)
        if (first > last) {
            return EMPTY
        }
        if (stride == 1) {
            return intersect(name, other, this)
        } else if (other.stride == 1) {
            return intersect(name, this, other)
        }
        throw UnsupportedOperationException("Intersection when both ranges have a stride")
    }

    /**
     * Determine if a given Range interval intersects this one.
     *
     * @param other range to intersect
     * @return true if their intervals intersect, ignoring stride.
     */
    fun intersects(other: Range): Boolean {
        return try {
            val result = this.intersect(other)
            result.length > 0
        } catch (e: InvalidRangeException) {
            false
        }
    }

    /**
     * Find the first element in a strided array after some index start.
     * Return the smallest element k in the Range, such that
     *
     *  * k  first
     *  * k  start
     *  * k  last
     *  * k = element of this Range
     *
     * @param start starting index
     * @return first in interval, else -1 if there is no such element.
     */
    fun getFirstInInterval(start: Int): Int {
        if (start > this.last) {
            return -1
        }
        if (start <= this.first) {
            return this.first
        }
        if (this.stride == 1) {
            return start
        }
        val offset = start - this.first
        var i = offset / this.stride
        i = if (offset % this.stride == 0) i else i + 1 // round up
        return this.first + i * this.stride
    }

    override fun toString(): String {
        return if (length == 0) ":" // EMPTY
        else if (length < 0) ":" // VLEN
        else first.toString() + ":" + last + if (stride > 1) ":$stride" else ""
    }

    /** Does not include the name.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val integers = other as Range
        return length == integers.length && first == integers.first && last == integers.last && stride == integers.stride
    }

    override fun hashCode(): Int {
        return Objects.hash(length, first, last, stride)
    }

    /////////////////////////////////////////////////////////
    override fun iterator(): Iterator<Int> {
        return InternalIterator()
    }

    private inner class InternalIterator : Iterator<Int> {
        private var current = 0
        override fun hasNext(): Boolean {
            return current < length
        }

        override fun next(): Int {
            return elementNC(current++)
        }
    }

    /** Get ith element; skip checking, for speed.  */
    private fun elementNC(i: Int): Int {
        return first + i * stride
    }

    companion object {
        val EMPTY = Range(0, 0, -1, 1, "EMPTY") // used for unlimited dimension = 0
        val SCALAR = Range(1, 0, 0, 1, "SCALAR")
        val VLEN = Range(-1, 0, -1, 1, "VLEN")

        /** Make a named Range from 0 to len-1. RuntimeException on error.  */
        fun make(name: String?, len: Int): Range {
            return try {
                Range(name, 0, len - 1, 1)
            } catch (e: InvalidRangeException) {
                throw RuntimeException(e) // cant happen if len > 0
            }
        }

        /** Make an unnamed Range from first to last. RuntimeException on error.  */
        fun make(first: Int, last: Int): Range {
            return try {
                Range(first, last)
            } catch (e: InvalidRangeException) {
                throw RuntimeException(e) // cant happen if last >= first
            }
        }

        /** Make an unnamed Range from first to last with stride. RuntimeException on error.  */
        fun make(first: Int, last: Int, stride: Int): Range {
            return try {
                Range(first, last, stride)
            } catch (e: InvalidRangeException) {
                throw RuntimeException(e) // cant happen if last >= first
            }
        }

        fun make(name: String?, first: Int, last: Int, stride: Int): Range {
            return try {
                Range(name, first, last, stride)
            } catch (e: InvalidRangeException) {
                throw RuntimeException(e) // cant happen if last >= first
            }
        }

        // r1 must have stride 1, rok may have a different stride
        @Throws(InvalidRangeException::class)
        private fun intersect(name: String?, rok: Range, r1: Range): Range {
            var first = Math.max(rok.first, r1.first)
            val last = Math.min(rok.last, r1.last)
            if (first > last) {
                return EMPTY
            }
            if (rok.first < r1.first) {
                val incr = (r1.first - rok.first) / rok.stride
                first = rok.first + incr * rok.stride
                if (first < r1.first) {
                    first += rok.stride
                }
            }
            return if (first > last) {
                EMPTY
            } else Range(name, first, last, rok.stride)
        }
    }
}