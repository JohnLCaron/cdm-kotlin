package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.IntBuffer

class ArrayInt(shape : IntArray, val values : IntBuffer) : ArrayTyped<Int>(Datatype.INT, shape) {

    override fun iterator(): Iterator<Int> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Int>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }
}