package com.sunya.cdm.iosp

import java.nio.IntBuffer

class ArrayInt(shape : IntArray, val values : IntBuffer) : ArrayTyped<Int>(shape) {

    override fun iterator(): Iterator<Int> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Int>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayInt

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