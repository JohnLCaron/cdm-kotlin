package com.sunya.cdm.iosp

import java.nio.ByteBuffer

// LOOK not dealing with n > 1
class ArrayOpaque(val values : ByteBuffer, val shape : IntArray) : ArrayTyped<ByteBuffer>() {

    override fun iterator(): Iterator<ByteBuffer> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<ByteBuffer>() {
        private var idx = 0
        override fun computeNext() = if (idx++ >= 1) done() else setNext(values)
    }
}