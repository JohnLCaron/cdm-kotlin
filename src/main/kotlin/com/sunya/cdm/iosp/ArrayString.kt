package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import java.nio.charset.StandardCharsets

class ArrayString(val values : Array<String>, val shape : IntArray) : ArrayTyped<String>() {

    override fun iterator(): Iterator<String> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<String>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.size) done() else setNext(values[idx++])
    }

}


/**
 * Create a String out of this ArrayByte, collapsing all dimensions into one.
 * If there is a null (zero) value in the array, the String will end there.
 * The null is not returned as part of the String.
 */
fun ArrayByte.makeStringFromBytes(): String {
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
        carr[idx++] = c
    }
    return String(carr, StandardCharsets.UTF_8)
}

/**
 * Create an Array of Strings out of this ArrayByte of any rank.
 * If there is a null (zero) value in the Array array, the String will end there.
 * The null is not returned as part of the String.
 *
 * @return Array of Strings of rank - 1.
 */
fun ArrayByte.makeStringsFromBytes(): ArrayString {
    val rank = shape.size
    if (rank < 2) {
        return ArrayString(arrayOf(makeStringFromBytes()), intArrayOf(1))
    }
    val innerLength: Int = shape[rank - 1]
    val outerLength = (Section(shape).computeSize() / innerLength).toInt()
    val outerShape = IntArray(rank - 1)
    System.arraycopy(shape, 0, outerShape, 0, rank - 1)
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
    return ArrayString(Array(outerLength) {result[it]!!}, outerShape)
}