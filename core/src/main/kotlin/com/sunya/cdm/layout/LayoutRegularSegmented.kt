package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize

/**
 * LayoutRegularSegmented has data stored in segments that are regularly spaced.
 * This is how Netcdf-3 "record variables" are laid out.
 *
 * @param startPos starting address of the entire data array.
 * @param elemSize size of an element in bytes.
 * @param recSize size of outer stride in bytes
 * @param wantSection the wanted section of data
*/
class LayoutRegularSegmented(val startPos: Long, override val elemSize: Int, val recSize: Long, wantSection: Section)
    : Layout {

    override val totalNelems: Long
    private val innerNelems: Long

    // outer chunk
    private val chunker: Chunker

    init {
        require(startPos > 0)
        require(elemSize > 0)
        require(recSize > 0)
        require(wantSection.varShape.size > 0) // no scalars
        chunker = Chunker(wantSection, Merge.notFirst) // each record becomes a chunk
        totalNelems = chunker.totalNelems
        innerNelems = if (wantSection.varShape[0] == 0L) 0 else wantSection.varShape.computeSize() / wantSection.varShape[0]
    }

    override fun hasNext(): Boolean {
        return chunker.hasNext()
    }

    private fun getFilePos(elem: Long): Long {
        val segno = elem / innerNelems
        val offset = elem % innerNelems
        return startPos + segno * recSize + offset * elemSize
    }

    override fun next(): Layout.Chunk {
        val chunk = chunker.next()
        val srcPos = getFilePos(chunk.srcElem)
        return LayoutChunk(srcPos, chunk)
    }
}