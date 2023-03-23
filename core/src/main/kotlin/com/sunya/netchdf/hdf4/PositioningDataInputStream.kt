package com.sunya.netchdf.hdf4

import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.iosp.ReaderIntoByteArray
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Similar to a DataInputStream that keeps track of position.
 * The position must always increase, no going backwards.
 * Note cant handle byte order yet - assume big endian(?).
 */
class PositioningDataInputStream(input: InputStream) : ReaderIntoByteArray {
    private val delegate: DataInputStream
    private var cpos: Long = 0

    init {
        delegate = if (input is DataInputStream) input else DataInputStream(input)
    }

    @Throws(IOException::class)
    private fun seek(pos: Long) {
        require(pos >= cpos) { "Cannot go backwards; current=$cpos request=$pos" }
        var want = pos - cpos
        while (want > 0) want -= delegate.skip(want)
        cpos = pos
    }

    override fun readIntoByteArray(state : OpenFileState, dest : ByteArray, destPos : Int, nbytes : Int) : Int {
        seek(state.pos)
        delegate.readFully(dest, destPos, nbytes)
        cpos += nbytes.toLong()
        return nbytes
     }
}