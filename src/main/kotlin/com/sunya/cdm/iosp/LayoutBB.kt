package com.sunya.cdm.iosp

import java.nio.*


/**
 * A Layout that supplies the "source" ByteBuffer.
 * This is used when the data must be massaged after being read, eg uncompresed or filtered.
 * The modified data is placed in a ByteBuffer, which may change for different chunks, and
 * so is supplied by each chunk.
 *
 *
 *
 * Example for Integers:
 *
 * <pre>
 * int[] read(LayoutBB index, int[] pa) {
 * while (index.hasNext()) {
 * LayoutBB.Chunk chunk = index.next();
 * IntBuffer buff = chunk.getIntBuffer();
 * buff.position(chunk.getSrcElem());
 * int pos = (int) chunk.getDestElem();
 * for (int i = 0; i &lt; chunk.getNelems(); i++)
 * pa[pos++] = buff.get();
 * }
 * return pa;
 * }
</pre> *
 */
interface LayoutBB : Layout {

    override fun next(): LayoutBB.Chunk // covariant return.

    /**
     * A contiguous chunk of data as a ByteBuffer.
     * Read nelems from ByteBuffer at filePos, store in destination at startElem.
     */
    interface Chunk : Layout.Chunk {
        val byteBuffer: ByteBuffer?

        /** Get the position in source where to read  */
        fun srcElem() : Int
    }
}