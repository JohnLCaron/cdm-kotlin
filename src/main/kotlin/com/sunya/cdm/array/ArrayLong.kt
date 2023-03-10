package com.sunya.cdm.array

import java.nio.LongBuffer

class ArrayLong(shape : IntArray, val values : LongBuffer) : ArrayTyped<Long>(shape) {

    override fun iterator(): Iterator<Long> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Long>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayLong

        if (values != other.values) return false
        if (!shape.contentEquals(other.shape)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + shape.contentHashCode()
        return result
    }
}