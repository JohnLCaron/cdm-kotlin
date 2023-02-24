package com.sunya.cdm.array

import java.nio.ShortBuffer

class ArrayUShort(shape : IntArray, val values : ShortBuffer) : ArrayTyped<UShort>(shape) {
    override fun iterator(): Iterator<UShort> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UShort>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUShort())
    }
}