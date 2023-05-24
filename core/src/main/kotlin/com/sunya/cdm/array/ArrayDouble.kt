package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayDouble(shape : IntArray, bb : ByteBuffer) : ArrayTyped<Double>(bb, Datatype.DOUBLE, shape) {
    val values = bb.asDoubleBuffer()

    override fun iterator(): Iterator<Double> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Double>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

    override fun section(section : Section) : ArrayDouble {
        return ArrayDouble(section.shape.toIntArray(), sectionFrom(section))
    }
}