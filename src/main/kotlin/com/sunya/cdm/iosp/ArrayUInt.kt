package com.sunya.cdm.iosp

import java.nio.IntBuffer

class ArrayUInt(val values : IntBuffer, val shape : IntArray) : ArrayTyped<UInt>() {
    override fun iterator(): Iterator<UInt> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UInt>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUInt())
    }
}