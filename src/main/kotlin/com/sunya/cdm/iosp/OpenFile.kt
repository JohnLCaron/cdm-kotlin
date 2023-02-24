package com.sunya.cdm.iosp

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class OpenFile(val location : String) : Closeable {
    val fileChannel : FileChannel
    val size : Long
    init {
        val raf = RandomAccessFile(File(location), "r")
        fileChannel = raf.getChannel();
        size = fileChannel.size()
    }

    override fun close() {
        fileChannel.close()
    }

    fun seek(pos : Long) {
        fileChannel.position(pos)
    }

    @Throws(IOException::class)
    fun readBytes(dst : ByteBuffer, state : OpenFileState) : Int {
        if (state.pos > fileChannel.size()) {
            throw EOFException("Tried to read past EOF ${fileChannel.size()} at pos ${state.pos} location $location")
        }
        val nread = fileChannel.read(dst, state.pos)
        if (nread != dst.capacity()) {
            throw EOFException("Only read $nread bytes of wanted ${dst.capacity()} bytes; starting at pos ${state.pos} EOF=${fileChannel.size()}")
        }
        dst.flip()
        state.pos += nread
        return nread
    }

    @Throws(IOException::class)
    fun readIntoByteBuffer(state : OpenFileState, dst : ByteBuffer, dstPos : Int, nbytes : Int) : Int {
        if (state.pos > fileChannel.size()) {
            throw EOFException("Tried to read past EOF ${fileChannel.size()} at pos ${state.pos} location $location")
        }
        // this is what fileChannel.read uses to read into dst; so limit and pos are getting munged
        dst.limit(dstPos + nbytes)
        dst.position(dstPos)
        val nread =  fileChannel.read(dst, state.pos)
        if (nread != nbytes) {
            throw EOFException("Tried to read past EOF at pos ${state.pos} location $location EOF=${fileChannel.size()}")
        }
        // println("read at ${state.pos} $nbytes bytes to $dstPos")
        state.pos += nread
        return nread
    }

    fun readByteBuffer(state : OpenFileState, nelems : Int): ByteBuffer {
        val dst = ByteBuffer.allocate(nelems)
        require (readBytes(dst, state) == nelems)
        dst.order(state.byteOrder)
        return dst
    }

    fun readBytes(dst : ByteArray, state : OpenFileState) : Int {
        return readBytes(ByteBuffer.wrap(dst), state)
    }

    fun readByte(state : OpenFileState): Byte {
        if (state.pos > fileChannel.size()) {
            throw EOFException("Tried to read past EOF ${fileChannel.size()} at pos ${state.pos} location $location")
        }
        val dst = ByteBuffer.allocate(1)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.get(0)
    }

    fun readDouble(state : OpenFileState): Double {
        val dst = ByteBuffer.allocate(8)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getDouble(0)
    }

    fun readFloat(state : OpenFileState): Float {
        val dst = ByteBuffer.allocate(4)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getFloat(0)
    }

    fun readInt(state : OpenFileState): Int {
        val dst = ByteBuffer.allocate(4)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getInt(0)
    }

    fun readLong(state : OpenFileState): Long {
        val dst = ByteBuffer.allocate(8)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getLong(0)
    }

    fun readShort(state : OpenFileState): Short {
        val dst = ByteBuffer.allocate(2)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getShort(0)
    }

    fun readString(state : OpenFileState, nbytes : Int): String {
        return readString(state, nbytes, StandardCharsets.UTF_8)
    }

    fun readString(state : OpenFileState, nbytes : Int, charset : Charset): String {
        val dst = ByteBuffer.allocate(nbytes)
        readBytes(dst, state)
        return makeStringZ(dst.array(), charset)
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
