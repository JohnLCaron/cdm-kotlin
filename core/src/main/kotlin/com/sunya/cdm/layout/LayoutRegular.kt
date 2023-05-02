package com.sunya.cdm.layout

import com.sunya.cdm.api.SectionL

/**
 * LayoutRegular has data stored in row-major order, like netcdf non-record variables.
 *
 * @param startPos starting address (bytes) of the complete source data array.
 * @param elemSize size of an element in bytes.
 * @param wantSection the wanted section of data, along wit the variable's shape
 */
class LayoutRegular(startPos: Long, elemSize: Int, wantSection: SectionL) : Layout {
    private val chunker: Chunker
    private val startPos : Long // starting position
    override val elemSize : Int // size of each element

    init {
        require(startPos >= 0)
        require(elemSize > 0)
        this.startPos = startPos
        this.elemSize = elemSize
        chunker = Chunker(wantSection) // one big chunk
    }

    override val totalNelems: Long
        get() = chunker.totalNelems

    override fun hasNext(): Boolean {
        return chunker.hasNext()
    }

    override fun next(): Layout.Chunk {
        val chunk = chunker.next()
        val srcPos = startPos + chunk.srcElem * elemSize
        return LayoutChunk(srcPos, chunk)
    }
}