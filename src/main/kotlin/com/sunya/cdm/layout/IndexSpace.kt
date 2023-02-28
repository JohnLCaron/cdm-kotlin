package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import kotlin.math.max
import kotlin.math.min

/**
 * A rectangular subsection of indices
 * Replaces Section for data chunk layout. no stride, negative indices are allowed.
 */
data class IndexSpace(val start : IntArray, val nelems : IntArray) {
    val rank = start.size
    val totalElements = computeSize(nelems)
    val limit by lazy { IntArray(rank) { idx -> start[idx]+ nelems[idx] - 1 } } // inclusive
    val ranges by lazy { start.mapIndexed { idx, it -> it until it + nelems[idx] } } // inclusive

    constructor(shape : IntArray) : this( IntArray(shape.size), shape) // starts at 0
    constructor(section : Section) : this( section.origin, section.shape)

    fun contains(pt : IntArray): Boolean {
        ranges.forEachIndexed { idx, range ->
            if (!range.contains(pt[idx])) {
                return false
            }
        }
        return true
    }

    fun shift(origin : IntArray): IndexSpace {
        val newOrigin = IntArray(rank) { idx -> start[idx] - origin[idx] }
        return IndexSpace(newOrigin, nelems)
    }

    fun intersect(other: IndexSpace): IndexSpace {
        val firstList = mutableListOf<Int>()
        val lengthList = mutableListOf<Int>()
        ranges.mapIndexed  { idx, range ->
            val orange = other.ranges[idx]
            val first = max(range.first, orange.first)
            val last = min(range.last, orange.last)
            firstList.add(first)
            lengthList.add(last - first + 1)
        }
        return IndexSpace(IntArray(rank) { firstList[it] }, IntArray(rank) { lengthList[it] })
    }

    fun makeSection() : Section {
        return Section(start, nelems)
    }

    override fun toString(): String {
        return "${makeSection()}"
    }


}