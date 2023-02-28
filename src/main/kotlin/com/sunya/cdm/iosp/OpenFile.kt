package com.sunya.cdm.iosp

import java.io.Closeable
import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** An abstraction over a Java FileChannel. */
data class OpenFile(val location : String) : Closeable {
    val raf : com.sunya.io.RandomAccessFile
    val size : Long
    init {
        raf = com.sunya.io.RandomAccessFile(location, "r")
        raf.order(ByteOrder.LITTLE_ENDIAN)
        size = raf.length()
    }

    override fun close() {
        raf.close()
    }

    fun readIntoByteBuffer(state : OpenFileState, dst : ByteBuffer, dstPos : Int, nbytes : Int) : Int {
        if (state.pos > size) {
            throw EOFException("Tried to read past EOF ${size} at pos ${state.pos} location $location")
        }
        val bb = readBytes(state, nbytes)
        var pos = dstPos
        for (idx in 0 until bb.size) {
            dst.put(pos++, bb[idx])
        }
        return bb.size
    }

    fun readByteBuffer(state : OpenFileState, nbytes : Int): ByteBuffer {
        val dst = readBytes(state, nbytes)
        val bb = ByteBuffer.wrap(dst)
        bb.order(state.byteOrder)
        return bb
    }

    // doesnt check the number of bytes read
    fun readBytesUnchecked(state : OpenFileState, dst : ByteArray) : Int {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        val nread = raf.read(dst)
        state.pos += nread
        return nread
    }

    fun readBytes(state : OpenFileState, dst : ByteArray) : Int {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        val nread = raf.read(dst)
        if (nread != dst.size) {
            throw EOFException("Only read $nread bytes of wanted ${dst.size} bytes; starting at pos ${state.pos} EOF=${size}")
        }
        state.pos += nread
        return nread
    }

    fun readBytes(state : OpenFileState, nbytes : Int) : ByteArray {
        val dst = ByteArray(nbytes)
        readBytes(state, dst)
        return dst
    }

    fun readByte(state : OpenFileState): Byte {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos++
        return raf.readByte()
    }

    fun readDouble(state : OpenFileState): Double {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos += 8
        return raf.readDouble()
    }

    fun readFloat(state : OpenFileState): Float {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos += 4
        return raf.readFloat()
    }

    fun readInt(state : OpenFileState): Int {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos += 4
        return raf.readInt()
    }

    fun readLong(state : OpenFileState): Long {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos += 8
        return raf.readLong()
    }

    fun readShort(state : OpenFileState): Short {
        raf.seek(state.pos)
        raf.order(state.byteOrder)
        state.pos += 2
        return raf.readShort()
    }

    fun readString(state : OpenFileState, nbytes : Int): String {
        return readString(state, nbytes, StandardCharsets.UTF_8)
    }

    fun readString(state : OpenFileState, nbytes : Int, charset : Charset): String {
        val dst = ByteArray(nbytes)
        readBytes(state, dst)
        return makeStringZ(dst, charset)
    }

    fun readArrayByte(state : OpenFileState, nelems : Int): Array<Byte> {
        val dst = readByteBuffer(state, nelems)
        return Array(nelems) { dst[it] }
    }

    fun readArrayShort(state : OpenFileState, nelems : Int): Array<Short> {
        val dst = readByteBuffer(state, 2 * nelems).asShortBuffer()
        return Array(nelems) { dst[it] }
    }

    fun readArrayInt(state : OpenFileState, nelems : Int): Array<Int> {
        val dst = readByteBuffer(state, 4 * nelems).asIntBuffer()
        return Array(nelems) { dst[it] }
    }

    fun readArrayLong(state : OpenFileState, nelems : Int): Array<Long> {
        val dst = readByteBuffer(state, 8 * nelems).asLongBuffer()
        return Array(nelems) { dst[it] }
    }

    fun readArrayFloat(state : OpenFileState, nelems : Int): Array<Float> {
        val dst = readByteBuffer(state, 4 * nelems).asFloatBuffer()
        return Array(nelems) { dst[it] }
    }

    fun readArrayDouble(state : OpenFileState, nelems : Int): Array<Double> {
        val dst = readByteBuffer(state, 8 * nelems).asDoubleBuffer()
        return Array(nelems) { dst[it] }
    }
}

data class OpenFileState(var pos : Long, var byteOrder : ByteOrder = ByteOrder.LITTLE_ENDIAN) {
    fun incr(addit : Long) : OpenFileState {
        this.pos += addit
        return this
    }
}

// terminate at a zero
fun makeStringZ(bb : ByteArray, charset : Charset): String {
    var count = 0
    while (count < bb.size && bb[count].toInt() != 0) count++
    return String(bb, 0, count, charset)
}
