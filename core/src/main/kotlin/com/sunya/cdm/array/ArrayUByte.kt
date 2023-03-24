package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.ByteBuffer

class ArrayUByte(shape : IntArray, val values : ByteBuffer) : ArrayTyped<UByte>(Datatype.UBYTE, shape) {
    override fun iterator(): Iterator<UByte> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UByte>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUByte())
    }
}