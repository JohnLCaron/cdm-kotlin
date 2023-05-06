package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayShort(shape : IntArray, bb : ByteBuffer) : ArrayTyped<Short>(bb, Datatype.SHORT, shape) {
    val values = bb.asShortBuffer()

    override fun iterator(): Iterator<Short> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Short>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun section(section : Section) : ArrayShort {
        return ArrayShort(section.shape.toIntArray(), sectionFrom(section))
    }
}