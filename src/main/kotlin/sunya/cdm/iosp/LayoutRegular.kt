package sunya.cdm.iosp

import sunya.cdm.api.Section

class LayoutRegular(startPos: Long, elemSize: Int, varShape: IntArray, wantSection: Section?) : Layout {
    private val chunker: IndexChunker
    private val startPos : Long // starting position

    override val elemSize : Int // size of each element


    /**
     * Constructor.
     *
     * @param startPos starting address of the entire data array.
     * @param elemSize size of an element in bytes.
     * @param varShape shape of the entire data array.
     * @param wantSection the wanted section of data, contains a List of Range objects.
     * @throws InvalidRangeException if ranges are misformed
     */
    init {
        require(startPos >= 0)
        require(elemSize > 0)
        this.startPos = startPos
        this.elemSize = elemSize
        chunker = IndexChunker(varShape, wantSection)
    }

    override val totalNelems: Long
        get() = chunker.totalNelems

    override fun hasNext(): Boolean {
        return chunker.hasNext()
    }

    override fun next(): Layout.Chunk {
        val chunk: IndexChunker.Chunk = chunker.next()
        chunk.srcPos = startPos + chunk.srcElem * elemSize
        return chunk
    }
}