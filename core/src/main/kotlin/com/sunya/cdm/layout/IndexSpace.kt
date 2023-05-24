package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize
import com.sunya.cdm.api.toLongArray
import kotlin.math.max
import kotlin.math.min

/** A rectangular subsection of indices, going from start to start + shape */
data class IndexSpace(val start : LongArray, val shape : LongArray) {
    val rank = start.size
    val totalElements = shape.computeSize()
    val last by lazy { LongArray(rank) { idx -> start[idx] + shape[idx] - 1 } } // inclusive
    val ranges : List<LongProgression> by lazy { start.mapIndexed {
            idx, start -> LongProgression.fromClosedRange(start, start + shape[idx] - 1, 1) } } // inclusive

    constructor(shape : IntArray) : this( shape.toLongArray()) // starts at 0
    constructor(shape : LongArray) : this( LongArray(shape.size), shape) // starts at 0
    constructor(section : Section) : this(section.ranges.map { it.first() }.toLongArray(), section.shape)
    constructor(rank : Int, start : LongArray, shape : LongArray) : this( LongArray(rank) { start[it] }, LongArray(rank) { shape[it] })

    fun section(varShape : LongArray) : Section {
        return Section(ranges, varShape)
    }

    fun contains(pt : LongArray): Boolean {
        if (rank != pt.size) {
            return false
        }
        ranges.forEachIndexed { idx, range ->
            if (!range.contains(pt[idx])) {
                return false
            }
        }
        return true
    }

    fun contains(other : IndexSpace): Boolean {
        if (rank != other.rank) {
            return false
        }
        ranges.forEachIndexed { idx, range : LongProgression ->
            val o : LongProgression = other.ranges[idx]
            if (!range.contains(o)) {
                return false
            }
        }
        return true
    }

    fun shift(origin : LongArray): IndexSpace {
        val newOrigin = LongArray(rank) { idx -> start[idx] - origin[idx] }
        return IndexSpace(newOrigin, shape)
    }

    fun intersect(other: IndexSpace): IndexSpace {
        val firstList = mutableListOf<Long>()
        val lengthList = mutableListOf<Long>()
        ranges.mapIndexed  { idx, range ->
            val orange = other.ranges[idx]
            val first = max(range.first, orange.first)
            val last = min(range.last, orange.last)
            firstList.add(first)
            lengthList.add(last - first + 1)
        }
        return IndexSpace(LongArray(rank) { firstList[it] }, LongArray(rank) { lengthList[it] })
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

private fun LongProgression.contains(other : LongProgression) : Boolean {
    return (other.first >= this.first && other.last <= this.last)
}