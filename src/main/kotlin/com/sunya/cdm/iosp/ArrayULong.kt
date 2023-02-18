package com.sunya.cdm.iosp

import java.nio.LongBuffer

class ArrayULong(shape : IntArray, val values : LongBuffer) : ArrayTyped<ULong>(shape) {
    override fun iterator(): Iterator<ULong> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<ULong>() {
        private var idx = 0
        override fun computeNext() {
            if (idx >= values.limit()) done() else setNext(values.get(idx++).toULong())
        }
    }
}