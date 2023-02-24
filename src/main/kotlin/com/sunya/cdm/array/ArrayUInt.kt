package com.sunya.cdm.array

import java.nio.IntBuffer

class ArrayUInt(shape : IntArray, val values : IntBuffer) : ArrayTyped<UInt>(shape) {
    override fun iterator(): Iterator<UInt> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UInt>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUInt())
    }
}