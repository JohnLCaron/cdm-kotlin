package com.sunya.cdm.iosp

import java.nio.ShortBuffer

class ArrayUShort(val values : ShortBuffer, val shape : IntArray) : ArrayTyped<UShort>() {
    override fun iterator(): Iterator<UShort> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<UShort>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++].toUShort())
    }
}