package com.sunya.cdm.iosp

import java.nio.LongBuffer

class ArrayULong(val values : LongBuffer, val shape : IntArray) : ArrayTyped<ULong>() {
    override fun iterator(): Iterator<ULong> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<ULong>() {
        private var idx = 0
        override fun computeNext() {
            if (idx >= values.limit()) done() else setNext(values.get(idx++).toULong())
        }
    }
}