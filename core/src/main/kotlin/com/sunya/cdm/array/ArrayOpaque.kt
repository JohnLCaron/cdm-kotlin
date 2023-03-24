package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.ByteBuffer

// LOOK not dealing with n > 1
class ArrayOpaque(shape : IntArray, val values : ByteBuffer, val size : Int) : ArrayTyped<ByteBuffer>(Datatype.OPAQUE, shape) {
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

    override fun toString(): String {
        return "ArrayOpaque(size=$size, nelems=$nelems, \n values=${showValues()})"
    }

    private fun showValues(): String {
        return buildString {
            for (idx in 0 until values.limit()) append("${values.get(idx)},")
        }
    }

}