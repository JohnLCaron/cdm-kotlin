package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize
import com.sunya.cdm.api.toIntArray
import java.nio.ByteBuffer

class ArrayUShort(shape : IntArray, datatype : Datatype<UShort>, bb : ByteBuffer) : ArrayTyped<UShort>(bb, datatype, shape) {
    val values = bb.asShortBuffer()

    constructor(shape : IntArray, bb : ByteBuffer) : this(shape, Datatype.USHORT, bb)

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