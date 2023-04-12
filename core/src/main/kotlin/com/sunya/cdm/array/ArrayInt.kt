package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import java.nio.ByteBuffer

class ArrayInt(shape : IntArray, bb : ByteBuffer) : ArrayTyped<Int>(bb, Datatype.INT, shape) {
    val values = bb.asIntBuffer()

    override fun iterator(): Iterator<Int> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Int>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun section(section : Section) : ArrayInt {
        return ArrayInt(section.shape, sectionFrom(section))
    }
}