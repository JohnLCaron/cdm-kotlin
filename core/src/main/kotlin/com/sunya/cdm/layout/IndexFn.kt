package com.sunya.cdm.layout

import com.sunya.cdm.api.computeSize
import java.nio.ByteBuffer

class IndexFn(val shape : IntArray) {
    val nelems = shape.computeSize()
    init {
        require(shape.size == 2) // ??
    }

    fun flip(orgBuffer : ByteBuffer, elemSize : Int) : ByteBuffer {
        val flipBuffer = ByteBuffer.allocate(orgBuffer.capacity())
        flipBuffer.order(orgBuffer.order())

        repeat (nelems) { elem ->
            val row = elem / shape[1]
            val col = elem % shape[1]
            val dstElem = col * shape[0] + row
            repeat (elemSize) { flipBuffer.put(dstElem * elemSize + it, orgBuffer.get(elem * elemSize + it)) }
        }

        return flipBuffer
    }

    fun flippedShape() = shape.flip()
}

fun IntArray.flip() : IntArray {
    val rank = this.size
    return IntArray(rank) { this[rank - it - 1] }
}

