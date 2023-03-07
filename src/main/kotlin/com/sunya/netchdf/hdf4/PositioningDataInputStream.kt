package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.Layout
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Similar to a DataInputStream that keeps track of position.
 * The position must always increase, no going backwards.
 * Note cant handle byte order yet - assume big endian(?).
 */
class PositioningDataInputStream(input: InputStream) {
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

    fun readIntoByteArray(pos : Long, dest : ByteArray, destPos : Int, nbytes : Int) : Int {
        seek(pos)
        delegate.readFully(dest, destPos, nbytes)
        cpos += nbytes.toLong()
        return nbytes
     }
}

fun readDataFromPositioningStream(input: PositioningDataInputStream, layout: Layout, v2: Variable, fillValue : Any?, wantSection : Section)
        : ArrayTyped<*> {
    require(wantSection.size() == layout.totalNelems)
    val vinfo = v2.spObject as Vinfo
    val totalNbytes = (vinfo.elemSize * layout.totalNelems)
    require(totalNbytes < Int.MAX_VALUE)
    val values = ByteBuffer.allocate(totalNbytes.toInt())

    var bytesRead = 0
    while (layout.hasNext()) {
        val chunk = layout.next()
        val dstPos = (vinfo.elemSize * chunk.destElem()).toInt()
        val chunkBytes = vinfo.elemSize * chunk.nelems()
        bytesRead += input.readIntoByteArray(chunk.srcPos(), values.array(), dstPos, chunkBytes)
    }
    require(bytesRead == totalNbytes.toInt())
    values.position(0)

    return when (v2.datatype) {
        Datatype.BYTE -> ArrayByte(wantSection.shape, values)
        Datatype.CHAR, Datatype.STRING -> ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
        Datatype.DOUBLE -> ArrayDouble(wantSection.shape, values.asDoubleBuffer())
        Datatype.FLOAT -> ArrayFloat(wantSection.shape, values.asFloatBuffer())
        Datatype.INT -> ArrayInt(wantSection.shape, values.asIntBuffer())
        Datatype.LONG -> ArrayLong(wantSection.shape, values.asLongBuffer())
        Datatype.SHORT -> ArrayShort(wantSection.shape, values.asShortBuffer())
        else -> throw IllegalArgumentException("datatype ${v2.datatype}")
    }
}