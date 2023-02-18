package com.sunya.cdm.iosp

import java.nio.ByteBuffer

class ArrayUByte(shape : IntArray, val values : ByteBuffer) : ArrayTyped<UByte>(shape) {
    override fun iterator(): Iterator<UByte> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UByte>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUByte())
    }
}