package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.toIntArray
import com.sunya.cdm.api.toLongArray
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

class ArrayOpaque(shape : IntArray, val values : ByteBuffer, val size : Int)
        : ArrayTyped<ByteBuffer>(values, Datatype.OPAQUE, shape) {
    init {
        require(nelems * size <= values.capacity())
    }

    // src element is the 1D index
    fun getElement(srcElem : Int) : ByteBuffer {
        val elem = ByteBuffer.allocate(size)
        copyElem(srcElem, elem, 0)
        return elem
    }

    override fun iterator(): Iterator<ByteBuffer> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<ByteBuffer>() {
        private var idx = 0
        override fun computeNext() = if (idx >= nelems) done() else {
            val elem = ByteBuffer.allocate(size)
            copyElem(idx, elem, 0)
            idx++
            setNext(elem)
        }
    }

    // copy the src[srcIdx] element the dstIdx element in dest[dstIdx]
    private fun copyElem(srcIdx : Int, dest : ByteBuffer, dstIdx : Int) {
        repeat(size) { dest.put(dstIdx * size + it, values.get(srcIdx * size + it)) }
    }

    override fun showValues(): String {
        return buildString {
            val iter = this@ArrayOpaque.iterator()
            for (bb in iter) {
                append("'${bb.array().contentToString()}',")
            }
        }
    }

    override fun section(section : Section) : ArrayOpaque {
        val sectionNelems = section.totalElements.toInt()
        val sectionBB = ByteBuffer.allocate(size * sectionNelems)

        val odo = IndexND(IndexSpace(section), this.shape.toLongArray())
        var dstIdx = 0
        for (index in odo) {
            copyElem(odo.element().toInt(), sectionBB, dstIdx)
            dstIdx++
        }
        return ArrayOpaque(section.shape.toIntArray(), sectionBB, size)
    }

}