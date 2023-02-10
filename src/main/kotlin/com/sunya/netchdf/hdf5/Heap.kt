package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*

private const val debugHeap = false

internal class H5heap(val header: H5builder) {
    val raf = header.raf
    private val heapMap = mutableMapOf<Long, GlobalHeap>()

    /**
     * Fetch a Vlen data array.
     *
     * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
     * @param dataType type of data
     * @param endian byteOrder of the data (0 = BE, 1 = LE)
     * @return the Array read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getHeapDataArray(globalHeapIdAddress: Long, dataType: DataType, endian: ByteOrder?): Array<*> {
        val heapId: HeapIdentifier = readHeapIdentifier(globalHeapIdAddress)
        return getHeapDataArray(heapId, dataType, endian)
    }

    @Throws(IOException::class)
    fun getHeapDataArray(heapId: HeapIdentifier, dataType: DataType, endian: ByteOrder?): Array<*> {
        val ho = heapId.getHeapObject()
            ?: throw IllegalStateException("Illegal Heap address, HeapObject = $heapId")

        val state = OpenFileState(ho.dataPos, endian?: ByteOrder.nativeOrder())
        if (DataType.FLOAT === dataType) {
            return raf.readArrayFloat(state, heapId.nelems)
        } else if (DataType.DOUBLE === dataType) {
            return raf.readArrayDouble(state, heapId.nelems)
        } else if (dataType.primitiveClass == Byte::class.java) {
            return raf.readArrayByte(state, heapId.nelems)
        } else if (dataType.primitiveClass == Short::class.java) {
            return raf.readArrayShort(state, heapId.nelems)
        } else if (dataType.primitiveClass== Int::class.java) {
            return raf.readArrayInt(state, heapId.nelems)
        } else if (dataType.primitiveClass== Long::class.java) {
            return raf.readArrayLong(state, heapId.nelems)
        }
        throw UnsupportedOperationException("getHeapDataAsArray dataType=$dataType")
    }

    /**
     * Fetch a String from the heap.
     *
     * @param heapIdAddress address of the heapId, used to get the String out of the heap
     * @return String the String read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun readHeapString(heapIdAddress: Long): String? {
        val heapId = this.readHeapIdentifier(heapIdAddress)
        if (heapId.isEmpty()) {
            return H5builder.NULL_STRING_VALUE
        }
        val ho: GlobalHeap.HeapObject = heapId.getHeapObject()
            ?: throw IllegalStateException("Cant find Heap Object,heapId=$heapId")
        if (ho.dataSize > 1000 * 1000) return java.lang.String.format("Bad HeapObject.dataSize=%s", ho)
        val state = OpenFileState(ho.dataPos, ByteOrder.nativeOrder())
        raf.seek(ho.dataPos)
        return raf.readString(state, ho.dataSize.toInt(), header.valueCharset)
    }

    // see "Global Heap Id" in http://www.hdfgroup.org/HDF5/doc/H5.format.html
    @Throws(IOException::class)
    fun readHeapIdentifier(globalHeapIdAddress: Long): HeapIdentifier {
        return HeapIdentifier(globalHeapIdAddress)
    }

    // the heap id is has already been read into a byte array at given pos
    fun readHeapIdentifier(bb: ByteBuffer, pos: Int): HeapIdentifier {
        return HeapIdentifier(bb, pos)
    }

    // see "Global Heap Id" in http://www.hdfgroup.org/HDF5/doc/H5.format.html
    internal inner class HeapIdentifier {
        val nelems: Int // "number of 'base type' elements in the sequence in the heap"
        private val heapAddress: Long
        private val index: Int

        // address must be absolute, getFileOffset already added
        constructor(address: Long) {
            if (address < 0 || address >= raf.size) {
                throw IllegalStateException("$address out of bounds; address ")
            }

            // header information is in le byte order
            val state = OpenFileState(address, ByteOrder.LITTLE_ENDIAN)
            nelems = raf.readInt(state)
            heapAddress = header.readOffset(state)
            index = raf.readInt(state)
        }

        // the heap id is in ByteBuffer at given pos
        constructor(bb: ByteBuffer, pos: Int) {
            bb.order(ByteOrder.LITTLE_ENDIAN) // header information is in LE byte order
            bb.position(pos) // reletive reading
            nelems = bb.int
            heapAddress = if (header.isOffsetLong) bb.long else bb.int.toLong()
            index = bb.int
        }

        fun isEmpty(): Boolean {
            return heapAddress == 0L
        }

        override fun toString(): String {
            return " nelems=$nelems heapAddress=$heapAddress index=$index"
        }

        fun getHeapObject(): GlobalHeap.HeapObject? {
            if (isEmpty()) return null
            var gheap = heapMap[heapAddress]
            if (null == gheap) {
                gheap = GlobalHeap(heapAddress)
                heapMap[heapAddress] = gheap
            }
            return gheap.getHeapObject(index.toShort()) ?: throw IllegalStateException("cant find HeapObject")
        }
    } // HeapIdentifier

    // level 1E Global Heap
    inner class GlobalHeap(address: Long) {
        private val version: Byte
        private val sizeBytes: Int
        private val hos: MutableMap<Short, HeapObject> = HashMap()

        init {
            val filePos: Long = header.getFileOffset(address)
            if (filePos < 0 || filePos >= raf.size) {
                throw IllegalStateException("$filePos out of bounds; address=$address ")
            }

            // header information is in le byte order
            val state = OpenFileState(address, ByteOrder.LITTLE_ENDIAN)

            // header
            val magic: String = raf.readString(state, 4)
            check(magic == "GCOL") { "$magic should equal GCOL" }
            version = raf.readByte(state)
            state.pos += 3
            sizeBytes = raf.readInt(state)
            state.pos += 4  // pad to 8

            var count = 0
            var countBytes = 0
            while (true) {
                val startPos: Long = state.pos
                val o = HeapObject()
                o.id = raf.readShort(state)
                if (o.id.toInt() == 0) break // ?? look
                o.refCount = raf.readShort(state)
                state.pos += 4
                o.dataSize = header.readLength(state)
                o.dataPos = state.pos
                val dsize = o.dataSize.toInt() + padding(o.dataSize.toInt(), 8)
                countBytes += dsize + 16
                if (o.dataSize < 0) break // ran off the end, must be done
                if (countBytes < 0) break // ran off the end, must be done
                if (countBytes > sizeBytes) break // ran off the end
                state.pos += dsize
                hos[o.id] = o
                count++
                if (countBytes + 16 >= sizeBytes) break // ran off the end, must be done
            }
        }

        fun getHeapObject(id: Short): HeapObject? {
            return hos[id]
        }

        internal inner class HeapObject {
            var id: Short = 0
            var refCount: Short = 0
            var dataSize: Long = 0
            var dataPos: Long = 0
            override fun toString(): String {
                return "id=$id, refCount=$refCount, dataSize=$dataSize, dataPos=$dataPos"
            }
        }
    } // GlobalHeap
}

// Level 1D - Local Heaps
internal class LocalHeap(header : H5builder, address: Long) {
    var size: Int
    var freelistOffset: Long
    var dataAddress: Long
    var heap: ByteBuffer
    var version: Byte

    init {
        // header information is in le byte order
        val state = OpenFileState(header.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

        // header
        val magic: String = header.raf.readString(state,4)
        check(magic == "HEAP") { "$magic should equal HEAP" }
        version = header.raf.readByte(state)
        state.pos += 3
        size = header.readLength(state).toInt()
        freelistOffset = header.readLength(state)
        dataAddress = header.readOffset(state)

        // data
        state.pos = header.getFileOffset(dataAddress)
        heap = header.raf.readByteBuffer(state, size)
        val hsize: Int = 8 + 2 * header.sizeLengths + header.sizeOffsets
        if (debugHeap) {
            println("LocalHeap hsize = $hsize")
        }
    }

    fun getStringAt(offset: Int): String {
        var count = 0
        while (heap[offset + count].toInt() != 0) count++
        return String(heap.array(), offset, count, StandardCharsets.UTF_8)
    }
} // LocalHeap