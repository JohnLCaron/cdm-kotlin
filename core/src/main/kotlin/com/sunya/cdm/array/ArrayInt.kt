package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.SectionL
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayInt(shape : IntArray, bb : ByteBuffer) : ArrayTyped<Int>(bb, Datatype.INT, shape) {
    val values = bb.asIntBuffer()

    override fun iterator(): Iterator<Int> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Int>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun section(section : SectionL) : ArrayInt {
        return ArrayInt(section.shape.toIntArray(), sectionFrom(section))
    }
}