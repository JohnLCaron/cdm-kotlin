package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import java.nio.FloatBuffer

class ArrayFloat(shape : IntArray, val values : FloatBuffer) : ArrayTyped<Float>(Datatype.FLOAT, shape) {

    override fun iterator(): Iterator<Float> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<Float>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.limit()) done() else setNext(values[idx++])
    }

}