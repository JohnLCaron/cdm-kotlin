package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.LongBuffer

class ArrayLong(shape : IntArray, val values : LongBuffer) : ArrayTyped<Long>(Datatype.LONG, shape) {

    override fun iterator(): Iterator<Long> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Long>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }
}