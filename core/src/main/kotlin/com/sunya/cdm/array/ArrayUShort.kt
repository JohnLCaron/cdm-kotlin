package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayUShort(shape : IntArray, bb : ByteBuffer) : ArrayTyped<UShort>(bb, Datatype.USHORT, shape) {
    val values = bb.asShortBuffer()

    override fun iterator(): Iterator<UShort> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UShort>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUShort())
    }

    override fun section(section : Section) : ArrayUShort {
        return ArrayUShort(section.shape.toIntArray(), sectionFrom(section))
    }

    companion object {
        fun fromArray(shape : IntArray, sa : ShortArray) : ArrayUShort {
            val bb = ByteBuffer.allocate(2 * shape.computeSize())
            val sbb = bb.asShortBuffer()
            sa.forEach { sbb.put(it) }
            return ArrayUShort(shape, bb)
        }
    }
}