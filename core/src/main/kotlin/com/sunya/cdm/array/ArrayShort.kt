package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.ShortBuffer

class ArrayShort(shape : IntArray, val values : ShortBuffer) : ArrayTyped<Short>(Datatype.SHORT, shape) {

    override fun iterator(): Iterator<Short> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Short>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayShort

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