package com.sunya.cdm.api

import java.util.*

/** A partially filled section of multidimensional array indices. */
data class SectionP(val ranges : List<LongProgression?>) {

    /**
     * Check if this Section is legal for the given shape.*
     * @param shape range must fit within this shape, rank must match.
     * @return error message if illegal, null if all ok
     */
    fun checkInRange(shape: LongArray): String? {
        if (ranges.size != shape.size) {
            return "Number of ranges in section (${ranges.size}) must be = ${shape.size}"
        }
        for (i in ranges.indices) {
            val r = ranges[i] ?: continue
            if (r.isEmpty()) {
                return if (shape[i] != 0L) "Illegal Range for dimension $i: empty range only allowed for unlimited dimension len = 0" else continue
            }
            if (r.last >= shape[i]) return "Illegal Range for dimension $i: last requested ${r.last} > max ${shape[i]-1}"
        }
        return null
    }

    companion object {
        val SCALAR = SectionL(longArrayOf(1))

        /**
         * Return a Section guaranteed to be non null, with no null Ranges, and within the bounds set by shape.
         * A section with no nulls is called "filled".
         *
         * @param s the original Section, may be null or not filled
         * @param varShape use this as default shape if any of the ranges are null.
         * @return a filled SectionL
         * @throws InvalidRangeException if shape and s and shape rank dont match, or if s has invalid range compared to shape
         */
        @Throws(InvalidRangeException::class)
        fun fill(s: SectionP?, varShape: LongArray): SectionL {
            // want the entire variable's data
            if (s == null) {
                return SectionL(varShape)
            }
            if (varShape.isEmpty() && s.ranges.isEmpty()) {
                return SCALAR
            }
            val errs = s.checkInRange(varShape)
            if (errs != null) {
                throw InvalidRangeException(errs)
            }
            // where the range is missing, we want the entire variable's dimension length
            val filledRanges = s.ranges.mapIndexed { idx, range ->
                range ?: LongProgression.fromClosedRange(0L, varShape[idx] - 1L, 1L)
            }
            return SectionL(filledRanges, varShape)
        }

        /**
         * Parse an index section String specification, return equivalent Section.
         * A null Range means "all" (i.e.":") indices in that dimension.
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
        fun fromSpec(sectionSpec: String) : SectionP {
            val ranges = mutableListOf<LongProgression?>()
            var range: LongProgression?
            val stoke = StringTokenizer(sectionSpec, "(),") // TODO deal with scatterRange {1,2,3}
            while (stoke.hasMoreTokens()) {
                val s = stoke.nextToken().trim { it <= ' ' }
                range = if (s == ":") {
                    null // all
                } else if (s.indexOf(':') < 0) { // just a number : slice
                    try {
                        val index = s.toLong()
                        LongProgression.fromClosedRange(index, index, 1L)
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException(" illegal selector: $s part of <$sectionSpec>")
                    }
                } else { // gotta be "start : end" or "start : end : stride"
                    val stoke2 = StringTokenizer(s, ":")
                    val s1 = stoke2.nextToken()
                    val s2 = stoke2.nextToken()
                    val s3 = if (stoke2.hasMoreTokens()) stoke2.nextToken() else null
                    try {
                        val index1 = s1.toLong()
                        val index2 = s2.toLong()
                        val stride = s3?.toLong() ?: 1L
                        LongProgression.fromClosedRange(index1, index2, stride)
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException(" illegal selector: $s part of <$sectionSpec>")
                    }
                }
                ranges.add(range)
            }
            return SectionP(ranges)
        }

    }
}