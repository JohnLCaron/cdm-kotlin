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
     * @param datatype type of data
     * @param endian byteOrder of the data (0 = BE, 1 = LE)
     * @return the Array read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getHeapDataArray(globalHeapIdAddress: Long, datatype: Datatype, endian: ByteOrder?): Array<*> {
        val heapId: HeapIdentifier = readHeapIdentifier(globalHeapIdAddress)
        return getHeapDataArray(heapId, datatype, endian)
    }

    @Throws(IOException::class)
    fun getHeapDataArray(heapId: HeapIdentifier, datatype: Datatype, endian: ByteOrder?): Array<*> {
        val ho = heapId.getHeapObject()
            ?: throw IllegalStateException("Illegal Heap address, HeapObject = $heapId")

        val typedef = datatype.typedef
        val valueDatatype = if (typedef != null) typedef.baseType else datatype

        val state = OpenFileState(ho.dataPos, endian ?: ByteOrder.nativeOrder())
        val result = when (valueDatatype) {
            Datatype.FLOAT -> raf.readArrayFloat(state, heapId.nelems)
            Datatype.DOUBLE -> raf.readArrayDouble(state, heapId.nelems)
            Datatype.BYTE -> raf.readArrayByte(state, heapId.nelems)
            Datatype.SHORT -> raf.readArrayShort(state, heapId.nelems)
            Datatype.INT -> raf.readArrayInt(state, heapId.nelems)
            Datatype.LONG -> raf.readArrayLong(state, heapId.nelems)
            else -> throw UnsupportedOperationException("getHeapDataAsArray datatype=$datatype")
        }
        return result
    }

    /**
     * Fetch a String from the heap, when the heap identifier has already been put into a ByteBuffer at given pos
     *
     * @param bb heap id is here
     * @param pos at this position
     * @return String the String read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun readHeapString(bb: ByteBuffer, pos: Int): String? {
        val heapId: HeapIdentifier = HeapIdentifier(bb, pos)
        if (heapId.isEmpty()) {
            return null
        }
        val ho = heapId.getHeapObject() ?: throw IllegalStateException("Cant find Heap Object,heapId=$heapId")
        val state = OpenFileState(ho.dataPos, ByteOrder.LITTLE_ENDIAN)
        return raf.readString(state, ho.dataSize.toInt())
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
            return null // H5builder.NULL_STRING_VALUE
        }
        val ho: GlobalHeap.HeapObject = heapId.getHeapObject()
            ?: throw IllegalStateException("Cant find Heap Object,heapId=$heapId")
        if (ho.dataSize == 0L) return null
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
        constructor(bb: ByteBuffer, start: Int) {
            var pos = start
            bb.order(ByteOrder.LITTLE_ENDIAN) // header information is in LE byte order
            nelems = bb.getInt(pos)
            pos += 4
            heapAddress = if (header.isOffsetLong) bb.getLong(pos) else bb.getInt(pos).toLong()
            pos += header.sizeOffsets
            index = bb.getInt(pos)
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