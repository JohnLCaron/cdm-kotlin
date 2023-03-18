package com.sunya.netchdf.hdf4

import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder

/** Make a linked list of data segments look like an InputStream. */
internal class LinkedInputStream(val h4 : H4builder,
                                 val nsegs: Int,
                                 val segPosA: LongArray,
                                 val segSizeA: IntArray) : InputStream() {

    constructor(h4 : H4builder, vinfo: Vinfo) : this(h4, vinfo.segSize.size, vinfo.segPos, vinfo.segSize)

    private val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
    private var segno = -1
    private var segpos = 0
    private var segSize = 0
    private var buffer = ByteArray(0)

    @Throws(IOException::class)
    private fun readSegment(): Boolean {
        segno++
        if (segno == nsegs) {
            return false
        }
        segSize = segSizeA[segno]
        while (segSize == 0) { // for some reason may have a 0 length segment
            segno++
            if (segno == nsegs) return false
            segSize = segSizeA[segno]
        }

        state.pos = segPosA[segno]
        buffer = h4.raf.readBytes(state, segSize)
        segpos = 0
        return true
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (segpos == segSize) {
            val ok = readSegment()
            if (!ok) return -1
        }
        val b = buffer[segpos].toInt() and 0xff
        segpos++
        return b
    }
}

internal fun makeSpecialLinkedInputStream(h4: H4builder, linked: SpecialLinked): LinkedInputStream {
    val linkedBlocks = linked.getLinkedDataBlocks(h4)
    val nsegs = linkedBlocks.size
    val segPos = LongArray(nsegs) { idx -> linkedBlocks[idx].offset }
    val segSize = IntArray(nsegs) { idx -> linkedBlocks[idx].length }
    return LinkedInputStream(h4, nsegs, segPos, segSize)
}