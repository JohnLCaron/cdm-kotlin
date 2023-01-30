package sunya.cdm.netcdf3

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class OpenFile(val location : String) {
    val fileChannel : FileChannel
    val size : Long
    init {
        val raf = RandomAccessFile(File(location), "r")
        fileChannel = raf.getChannel();
        size = fileChannel.size()
    }

    fun seek(pos : Long) {
        fileChannel.position(pos)
    }

    @Throws(IOException::class)
    fun readBytes(dst : ByteBuffer, state : OpenFileState) : Int {
        val nread =  fileChannel.read(dst, state.pos)
        if (nread != dst.capacity()) {
            throw EOFException("Tried to read past EOF at pos ${state.pos} location $location")
        }
        dst.flip()
        state.pos += nread
        return nread
    }

    fun readBytes(dst : ByteArray, state : OpenFileState) : Int {
        return readBytes(ByteBuffer.wrap(dst), state)
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

    fun readFloat(state : OpenFileState): Float {
        val dst = ByteBuffer.allocate(4)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getFloat(0)
    }

    fun readDouble(state : OpenFileState): Double {
        val dst = ByteBuffer.allocate(8)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getDouble(0)
    }

    fun readShort(state : OpenFileState): Short {
        val dst = ByteBuffer.allocate(2)
        readBytes(dst, state)
        dst.order(state.byteOrder)
        return dst.getShort(0)
    }

    fun readByteBuffer(state : OpenFileState, nelems : Int): ByteBuffer {
        val dst = ByteBuffer.allocate(nelems)
        require (readBytes(dst, state) == nelems)
        dst.order(state.byteOrder)
        return dst
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

data class OpenFileState(var pos : Long, var byteOrder : ByteOrder)
