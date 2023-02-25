package com.sunya.cdm.iosp

import java.nio.*

/**
 * A Layout that supplies the "source" ByteBuffer.
 * This is used when the data must be massaged after being read, eg uncompresed or filtered.
 * The modified data is placed in a ByteBuffer, which may change for different chunks, and
 * so is supplied by each chunk.
 */
interface LayoutBB : Layout {

    override fun next(): LayoutBB.Chunk // covariant return.

    /**
     * A contiguous chunk of data stored in a ByteBuffer source.
     * srcPos() and srcElem() references this ByteBuffer
     */
    interface Chunk : Layout.Chunk {
        val byteBuffer: ByteBuffer
    }
}