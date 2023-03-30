package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.DoubleBuffer

class ArrayDouble(shape : IntArray, val values : DoubleBuffer) : ArrayTyped<Double>(Datatype.DOUBLE, shape) {

    override fun iterator(): Iterator<Double> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Double>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }
}