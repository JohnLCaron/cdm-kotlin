package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.ShortBuffer

class ArrayShort(shape : IntArray, val values : ShortBuffer) : ArrayTyped<Short>(Datatype.SHORT, shape) {

    override fun iterator(): Iterator<Short> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Short>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }
}