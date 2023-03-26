package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import kotlin.math.max
import kotlin.math.min

/**
 * A rectangular subsection of indices
 * Replaces Section for data chunk layout. no stride, negative indices are allowed.
 */
data class IndexSpace(val start : IntArray, val shape : IntArray) {
    val rank = start.size
    val totalElements = computeSize(shape)
    val last by lazy { IntArray(rank) { idx -> start[idx] + shape[idx] - 1 } } // inclusive
    val ranges : List<IntProgression> by lazy { start.mapIndexed {
            idx, start -> IntProgression.fromClosedRange(start, start + shape[idx] - 1, 1) } } // inclusive

    constructor(shape : IntArray) : this( IntArray(shape.size), shape) // starts at 0
    constructor(section : Section) : this(section.origin, section.shape)
    constructor(rank : Int, start : IntArray, shape : IntArray) : this( IntArray(rank) { start[it] }, IntArray(rank) { shape[it] })

    fun section() : Section {
        val useShape = if (shape.size == start.size) shape else IntArray(start.size) { shape[it] }
        return Section(start, useShape)
    }

    fun contains(pt : IntArray): Boolean {
        require(rank == pt.size)
        ranges.forEachIndexed { idx, range ->
            if (!range.contains(pt[idx])) {
                return false
            }
        }
        return true
    }

    fun contains(other : IndexSpace): Boolean {
        require(rank == other.rank)
        ranges.forEachIndexed { idx, range ->
            if (!range.contains(other.ranges[idx])) {
                return false
            }
        }
        return true
    }

    fun shift(origin : IntArray): IndexSpace {
        val newOrigin = IntArray(rank) { idx -> start[idx] - origin[idx] }
        return IndexSpace(newOrigin, shape)
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

    fun intersects(other: IndexSpace): Boolean {
        ranges.mapIndexed  { idx, range ->
            val orange = other.ranges[idx]
            val first = Math.max(range.first, orange.first)
            val last = Math.min(range.last, orange.last)
            if (first > last) {
                return false
            }
        }
        return true
    }

    fun makeSection() : Section {
        return Section(start, shape)
    }

    override fun toString(): String {
        // return "${makeSection()} total=${totalElements}"
        return "${start.contentToString()} ${shape.contentToString()} total=${totalElements}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndexSpace) return false

        if (!start.contentEquals(other.start)) return false
        if (!shape.contentEquals(other.shape)) return false
        if (rank != other.rank) return false
        if (totalElements != other.totalElements) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.contentHashCode()
        result = 31 * result + shape.contentHashCode()
        result = 31 * result + rank
        result = 31 * result + totalElements.hashCode()
        return result
    }
}

private fun IntProgression.contains(pt : Int) : Boolean {
    return (pt >= this.first && pt <= this.last)
}

private fun IntProgression.contains(other : IntProgression) : Boolean {
    return (other.first >= this.first && other.last <= this.last)
}