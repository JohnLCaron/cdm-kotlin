package com.sunya.cdm.iosp

import com.sunya.cdm.api.computeSize
import java.nio.ByteBuffer

// LOOK not dealing with n > 1
class ArrayOpaque(shape : IntArray, val values : ByteBuffer, val size : Int) : ArrayTyped<ByteBuffer>(shape) {
    private val nelems = shape.computeSize()

    init {
        require(nelems * size <= values.capacity())
    }

    override fun iterator(): Iterator<ByteBuffer> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<ByteBuffer>() {
        private var idx = 0
        override fun computeNext() = if (idx >= nelems) done() else {
            values.position(idx * size)
            values.limit((idx + 1) * size)
            idx++
            setNext(values)
        }
    }
}