package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.iosp.IndexChunker

/**
 * LayoutRegular has data stored in row-major order, like netcdf non-record variables.
 *
 * @param startPos starting address (bytes) of the complete source data array.
 * @param elemSize size of an element in bytes.
 * @param varShape shape of the entire data array. must have rank &gt; 0
 * @param wantSection the wanted section of data; if null, use varShape
 */
class LayoutRegular(startPos: Long, elemSize: Int, varShape: IntArray, wantSection: Section?) : Layout {
    private val chunker: IndexChunker
    private val startPos : Long // starting position
    override val elemSize : Int // size of each element

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