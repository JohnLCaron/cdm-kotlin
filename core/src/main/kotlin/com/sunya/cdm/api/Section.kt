package com.sunya.cdm.api

import java.util.*

/**
 * A section of multidimensional array indices.
 * Represented as List<Range>.
 * TODO evaluate use of null.
*/
class Section {
    val ranges : List<Range?> // unmodifiableList

    /**
     * Create Section from a shape array, with origin 0, stride 1.
     *
     * @param shape array of lengths for each Range. 0 = EMPTY, &lt; 0 = VLEN
     */
    constructor(shape: IntArray) {
        val builder = ArrayList<Range>()
        for (aShape in shape) {
            require(aShape >= 0)
            if (aShape > 0) builder.add(Range(aShape)) else if (aShape == 0) builder.add(Range.EMPTY)
        }
        ranges = Collections.unmodifiableList(builder)
    }

    /**
     * Create Section from a shape and origin arrays.
     *
     * @param origin array of start for each Range
     * @param shape array of lengths for each Range
     * @throws InvalidRangeException if origin &lt; 0, or shape &lt; 1.
     */
    constructor(origin: IntArray, shape: IntArray) {
        require(origin.isScalar() == shape.isScalar() || origin.size == shape.size)
        val builder = mutableListOf<Range>()
        for (i in shape.indices) {
            require(shape[i] >= 0)
            if (shape[i] == 0) {
                builder.add(Range.EMPTY)
            } else if (origin[i] == 0 && shape[i] == 1) {
                builder.add(Range.SCALAR)
            } else {
                builder.add(Range(origin[i], origin[i] + shape[i] - 1))
            }
        }
        ranges = Collections.unmodifiableList(builder)
    }

    /** Create Section from a List<Range>. </Range> */
    constructor(from: List<Range?>) {
        ranges = from
    }

    /** Create Section from a variable length argument list of Ranges  */
    constructor(vararg ranges: Range) {
        this.ranges = ranges.asList()
    }

    /**
     * Create Section from a List<Range>, filling in nulls with shape.
     *
     * @param from the list of Range
     * @param shape use this as default shape if any of the ranges are null.
     * @throws InvalidRangeException if shape and range list dont match
    </Range> */
    constructor(from: List<Range?>, shape: IntArray) {
        if (shape.size != from.size) throw InvalidRangeException(" shape[] must have same rank as list of ranges")
        val builder = ArrayList<Range>()

        // check that any individual Range is null
        for (i in shape.indices) {
            require(shape[i] >= 0)
            val r = from[i]
            if (r == null) {
                if (shape[i] > 0) builder.add(Range(shape[i])) else if (shape[i] == 0) builder.add(Range.EMPTY)
            } else {
                builder.add(r)
            }
        }
        ranges = Collections.unmodifiableList(builder)
    }

    /**
     * Parse an index section String specification, return equivilent Section.
     * A null Range means "all" (i.e.":") indices in that dimension.
     *
     *
     * The sectionSpec string uses fortran90 array section syntax, namely:
     *
     * <pre>
     * sectionSpec := dims
     * dims := dim | dim, dims
     * dim := ':' | slice | start ':' end | start ':' end ':' stride
     * slice := INTEGER
     * start := INTEGER
     * stride := INTEGER
     * end := INTEGER
     *
     * where nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
     *
     * Meaning of index selector :
     * ':' = all
     * slice = hold index to that value
     * start:end = all indices from start to end inclusive
     * start:end:stride = all indices from start to end inclusive with given stride
     *
    </pre> *
     *
     * @param sectionSpec the token to parse, eg "(1:20,:,3,10:20:2)", parenthesis optional
     * @throws InvalidRangeException when the Range is illegal
     * @throws IllegalArgumentException when sectionSpec is misformed
     */
    constructor(sectionSpec: String) {
        val builder = ArrayList<Range?>()
        var range: Range?
        val stoke = StringTokenizer(sectionSpec, "(),") // TODO deal with scatterRange {1,2,3}
        while (stoke.hasMoreTokens()) {
            val s = stoke.nextToken().trim { it <= ' ' }
            range = if (s == ":") {
                null // all
            } else if (s.indexOf(':') < 0) { // just a number : slice
                try {
                    val index = s.toInt()
                    Range(index, index)
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException(" illegal selector: $s part of <$sectionSpec>")
                }
            } else { // gotta be "start : end" or "start : end : stride"
                val stoke2 = StringTokenizer(s, ":")
                val s1 = stoke2.nextToken()
                val s2 = stoke2.nextToken()
                val s3 = if (stoke2.hasMoreTokens()) stoke2.nextToken() else null
                try {
                    val index1 = s1.toInt()
                    val index2 = s2.toInt()
                    val stride = s3?.toInt() ?: 1
                    Range(index1, index2, stride)
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException(" illegal selector: $s part of <$sectionSpec>")
                }
            }
            builder.add(range)
        }
        ranges = Collections.unmodifiableList(builder)
    }

    fun contains(index : IntArray): Boolean {
        ranges.forEachIndexed { idx, r ->
            if (!(r!!.contains(index[idx]))) {
                return false
            }
        }
        return true
    }

    /**
     * Compute the element offset of an intersecting subrange of this.
     *
     * @param intersect the subrange
     * @return element offset
     */
    @Throws(InvalidRangeException::class)
    fun offset(intersect: Section): Int {
        if (!compatibleRank(intersect)) {
            throw InvalidRangeException("Incompatible Section rank")
        }
        var result = 0
        var stride = 1
        for (j in ranges.indices.reversed()) {
            val base = ranges[j]
            val r = intersect.getRange(j)
            val offset = base!!.index(r!!.first)
            result += offset * stride
            stride *= base.length
        }
        return result
    }

    /**
     * Convert List of Ranges to String sectionSpec.
     * Inverse of new Section(String sectionSpec)
     *
     * @return index section String specification
     */
    override fun toString(): String {
        val sbuff = Formatter()
        for (i in ranges.indices) {
            val r = ranges[i]
            if (i > 0) sbuff.format(",")
            if (r == null) sbuff.format(":") else {
                sbuff.format("%s", r)
            }
        }
        return sbuff.toString()
    }

    // Get shape array using the Range.length() values.
    val shape: IntArray
        get() {
            return IntArray(ranges.size) { ranges[it]!!.length }
        }
    // Get origin array using the Range.first() values.
    val origin: IntArray
        get() {
            return IntArray(ranges.size) { ranges[it]!!.first }
        }
    // Get stride array using the Range.stride() values
    val stride: IntArray
        get() {
            return IntArray(ranges.size) { ranges[it]!!.stride }
        }
    // Get limit array using the Range.first + length - 1, thus inclusive
    val limit: IntArray
        get() {
            return IntArray(ranges.size) { ranges[it]!!.first + ranges[it]!!.length - 1 }
        }

    /** Get origin of the ith Range  */
    fun origin(i: Int): Int {
        return ranges[i]!!.first
    }

    /** Get length of the ith Range  */
    fun shape(i: Int): Int {
        return ranges[i]!!.length
    }

    /** Get stride of the ith Range  */
    fun stride(i: Int): Int {
        return ranges[i]!!.stride
    }

    /** Get limit (exclusive) of the ith Range  */
    fun limit(i: Int): Int {
        return ranges[i]!!.first + ranges[i]!!.length
    }

    /** Get rank = number of Ranges.  */
    fun rank(): Int {
        return ranges.size
    }

    private fun compatibleRank(other: Section): Boolean {
        return rank() == other.rank()
    }

    /**
     * Compute total number of elements represented by the section.
     * Any null or VLEN Ranges are skipped.
     *
     * @return total number of elements
     */
    fun computeSize(): Long {
        var product: Long = 1
        for (r in ranges) {
            if (r == null || r.length < 0) {
                continue
            }
            product *= r.length
        }
        return product
    }

    fun size(): Long = size.value
    private val size = lazy {
        computeSize()
    }

    /**
     * Get the ith Range
     *
     * @param i index into the list of Ranges
     * @return ith Range
     */
    fun getRange(i: Int): Range? {
        return ranges[i]
    }

    /** Fil any null sections with the ccorresponding value from shape.  */
    @Throws(InvalidRangeException::class)
    fun fill(shape: IntArray): Section {
        return fill(this, shape)
    }

    /**
     * Find a Range by its name.
     *
     * @param rangeName find this Range
     * @return named Range or null
     */
    fun find(rangeName: String): Range? {
        for (r in ranges) {
            if (rangeName == r!!.name) {
                return r
            }
        }
        return null
    }

    /**
     * Check if this Section is legal for the given shape.
     * [Note: modified by dmh to address the case of unlimited
     * where the size is zero]
     *
     * @param shape range must fit within this shape, rank must match.
     * @return error message if illegal, null if all ok
     */
    fun checkInRange(shape: IntArray): String? {
        if (ranges.size != shape.size) {
            return "Number of ranges in section (" + ranges.size + ") must be = " + shape.size
        }
        for (i in ranges.indices) {
            val r = ranges[i] ?: continue
            if (r === Range.EMPTY) {
                return if (shape[i] != 0) "Illegal Range for dimension $i: empty range only for unlimited dimension len = 0" else continue
            }
            if (r.last >= shape[i]) return "Illegal Range for dimension " + i + ": last requested " + r.last + " > max " + (shape[i] - 1)
        }
        return null
    }

    /**
     * Is this section equivilent to the given shape.
     * All non-null ranges must have origin 0 and length = shape\[i]
     */
    @Throws(InvalidRangeException::class)
    fun equivalent(other: IntArray): Boolean {
        if (other.isScalar() && this.shape.isScalar()) {
            return true
        }
        if (rank() != other.size) {
            return false
        }
        for (i in ranges.indices) {
            val r = ranges[i] ?: continue
            if (r.first != 0) return false
            if (r.length != other[i]) return false
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val section = other as Section
        return ranges == section.ranges
    }

    override fun hashCode(): Int {
        return Objects.hash(ranges)
    }

    companion object {
        val SCALAR = Section(Range.SCALAR)

        /**
         * Return a Section guaranteed to be non null, with no null Ranges, and within the bounds set by shape.
         * A section with no nulls is called "filled".
         * If it is already filled, return it, otherwise return a new Section, filled from the shape.
         *
         * @param s the original Section, may be null or not filled
         * @param shape use this as default shape if any of the ranges are null.
         * @return a filled Section
         * @throws InvalidRangeException if shape and s and shape rank dont match, or if s has invalid range compared to shape
         */
        @Throws(InvalidRangeException::class)
        fun fill(s: Section?, shape: IntArray): Section {
            // want all
            if (s == null) {
                return Section(shape)
            }
            // scalar
            if (shape.size == 0 && s == SCALAR) {
                return s
            }
            val errs = s.checkInRange(shape)
            if (errs != null) {
                throw InvalidRangeException(errs)
            }

            // if s is already filled, use it
            var ok = true
            for (i in shape.indices) {
                ok = ok and (s.getRange(i) != null)
            }
            return if (ok) s else Section(s.ranges, shape)

            // fill in any nulls
        }

        /////////////////////////////////////////////////////////////////////////////////////////
        fun computeSize(shape: IntArray): Long {
            var product: Long = 1
            for (ii in shape.indices) product *= shape[ii].toLong()
            return product
        }

        /** Is this a scalar Section? Allows int[], int[1] {0}, int[1] {1}  */
        fun IntArray.isScalar(): Boolean {
            return this.size == 0 || this.size == 1 && this[0] < 2
        }

        fun IntArray.equivalent(other: IntArray): Boolean {
            if (this.isScalar() and other.isScalar()) {
                return true
            }
            return this.contentEquals(other)
        }

        // return outerShape, innerLength
        fun IntArray.breakoutInner() : Pair<IntArray, Int> {
            val rank = this.size
            val innerLength: Int = this[rank - 1]
            val outerShape = IntArray(rank - 1)
            System.arraycopy(this, 0, outerShape, 0, rank - 1)
            return Pair(outerShape, innerLength)
        }

        // return outerLength, innerShape
        fun IntArray.breakoutOuter() : Pair<Int, IntArray> {
            val rank = this.size // LOOK is scalar rank 1?
            if (rank < 1) {
                return Pair(0, intArrayOf())
            }
            val outerLength = this[0]
            val innerShape = IntArray(rank - 1)
            System.arraycopy(this, 1, innerShape, 0, rank - 1)
            return Pair(outerLength, innerShape)
        }

    }
}