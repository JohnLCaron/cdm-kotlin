package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import java.nio.ByteBuffer

class ArrayLong(shape : IntArray, bb : ByteBuffer) : ArrayTyped<Long>(bb, Datatype.LONG, shape) {
    val values = bb.asLongBuffer()

    override fun iterator(): Iterator<Long> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Long>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun section(section : Section) : ArrayLong {
        return ArrayLong(section.shape, sectionFrom(section))
    }
}