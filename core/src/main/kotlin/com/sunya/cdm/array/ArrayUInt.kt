package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.SectionL
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayUInt(shape : IntArray, bb : ByteBuffer) : ArrayTyped<UInt>(bb, Datatype.UINT, shape) {
    val values = bb.asIntBuffer()

    override fun iterator(): Iterator<UInt> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UInt>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUInt())
    }

    override fun section(section : SectionL) : ArrayUInt {
        return ArrayUInt(section.shape.toIntArray(), sectionFrom(section))
    }
}