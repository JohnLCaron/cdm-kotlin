package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class ArrayString(shape : IntArray, val values : List<String>) : ArrayTyped<String>(ByteBuffer.allocate(1), Datatype.STRING, shape) {

    constructor(shape : IntArray, valueArray : Array<String>) : this (shape, valueArray.toList())

    override fun iterator(): Iterator<String> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<String>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.size) done() else setNext(values[idx++])
    }

    override fun toString(): String {
        return buildString {
            append("shape=${shape.contentToString()} data= ")
            for (i in 0 until values.size) { append("'${values[i]}',")}
            append("\n")
        }
    }

    override fun section(section : SectionL) : ArrayString {
        val odo = IndexND(IndexSpace(section), this.shape.toLongArray())
        val sectionList = mutableListOf<String>()
        for (index in odo) {
            sectionList.add( values[odo.element().toInt()])
        }
        return ArrayString(section.shape.toIntArray(), sectionList)
    }
}


/**
 * Create a String out of this ArrayByte, collapsing all dimensions into one.
 * If there is a null (zero) value in the array, the String will end there.
 * The null is not returned as part of the String.
 */
fun ArrayUByte.makeStringFromBytes(): String {
    var count = 0
    for (c in this) {
        if (c.toInt() == 0) {
            break
        }
        count++
    }
    val carr = ByteArray(count)
    var idx = 0
    for (c in this) {
        if (c.toInt() == 0) {
            break
        }
        carr[idx++] = c.toByte()
    }
    return String(carr, StandardCharsets.UTF_8)
}

/**
 * Create an ArrayString out of this ArrayByte of any rank.
 * If there is a null (zero) value in the Array array, the String will end there.
 * The null is not returned as part of the String.
 *
 * @return Array of Strings of rank - 1.
 */
fun ArrayUByte.makeStringsFromBytes(): ArrayString {
    val rank = shape.size
    if (rank < 2) {
        return ArrayString(intArrayOf(), listOf(makeStringFromBytes()))
    }
    val (outerShape, innerLength) = shape.breakoutInner()
    val outerLength = outerShape.computeSize()

    val result = arrayOfNulls<String>(outerLength)
    val carr = ByteArray(innerLength)
    var cidx = 0
    var sidx = 0
    while (sidx < outerLength) {
        val idx = sidx * innerLength + cidx
        val c: Byte = values[idx]
        if (c.toInt() == 0) {
            result[sidx++] = String(carr, 0, cidx, StandardCharsets.UTF_8)
            cidx = 0
            continue
        }
        carr[cidx++] = c
        if (cidx == innerLength) {
            result[sidx++] = String(carr, StandardCharsets.UTF_8)
            cidx = 0
        }
    }
    return ArrayString(outerShape, Array(outerLength) { result[it]!!} )
}