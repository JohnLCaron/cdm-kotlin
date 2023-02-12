package com.sunya.cdm.iosp

import java.nio.ByteBuffer

class ArrayUByte(val values : ByteBuffer, val shape : IntArray) : ArrayTyped<UByte>() {
    override fun iterator(): Iterator<UByte> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UByte>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUByte())
    }
}