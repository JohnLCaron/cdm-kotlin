package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder
import java.util.*

/**
 * Level 1A2
 * Used in readGroupNew( type 5 and 6), readAttributesFromInfoMessage(), FractalHeap.
 * DHeapId(type 1,2,3,4)
 */
class BTree2(h5: H5builder, owner: String, address: Long) {
    val btreeType: Int
    private val nodeSize: Int // size in bytes of btree nodes
    private val recordSize: Short// size in bytes of btree records
    private val owner: String
    private val h5: H5builder
    private val raf: OpenFile
    val entryList: MutableList<Entry2> = ArrayList()

    init {
        this.h5 = h5
        raf = h5.raf
        this.owner = owner
        val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

        // header
        val magic = raf.readString(state, 4)
        check(magic == "BTHD") { "$magic should equal BTHD" }
        val version: Byte = raf.readByte(state)
        btreeType = raf.readByte(state).toInt()
        nodeSize = raf.readInt(state)
        recordSize = raf.readShort(state)
        val treeDepth: Short = raf.readShort(state)
        state.pos += 2
        val rootNodeAddress: Long = h5.readOffset(state)
        val numRecordsRootNode: Short = raf.readShort(state)
        val totalRecords: Long = h5.readLength(state) // total in entire btree
        val checksum: Int = raf.readInt(state)

        // eager reading of all nodes
        if (treeDepth > 0) {
            val node = InternalNode(rootNodeAddress, numRecordsRootNode, recordSize, treeDepth.toInt())
            node.recurse()
        } else {
            val leaf = LeafNode(rootNodeAddress, numRecordsRootNode)
            leaf.addEntries(entryList)
        }
    }

    internal fun getEntry1(hugeObjectID: Int): Record1? {
        for (entry in entryList) {
            val record1 = entry.record as Record1?
            if (record1!!.hugeObjectID == hugeObjectID.toLong()) return record1
        }
        return null
    }

    // these are part of the level 1A data structure, type = 0
    class Entry2 {
        var childAddress: Long = 0
        var nrecords: Long = 0
        var totNrecords: Long = 0
        var record: Any? = null
    }

    internal inner class InternalNode(address: Long, nrecords: Short, recordSize: Short, val depth: Int) {
        var entries: Array<Entry2?>

        init {
            val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

            // header
            val magic = raf.readString(state, 4)
            check(magic == "BTIN") { "$magic should equal BTIN" }
            val version: Byte = raf.readByte(state)
            val nodeType = raf.readByte(state).toInt()
            check(nodeType == btreeType)
            entries = arrayOfNulls(nrecords + 1) // did i mention theres actually n+1 children?
            for (i in 0 until nrecords) {
                entries[i] = Entry2()
                entries[i]!!.record = readRecord(state, btreeType.toInt())
            }
            entries[nrecords.toInt()] = Entry2()

            val maxNumRecords = nodeSize / recordSize // guessing
            val maxNumRecordsPlusDesc = nodeSize / recordSize // guessing
            for (i in 0 until nrecords + 1) {
                val e = entries[i]
                e!!.childAddress = h5.readOffset(state)
                e.nrecords = h5.readVariableSizeUnsigned(state, 1) // readVariableSizeMax(maxNumRecords);
                if (depth > 1) {
                    e.totNrecords = h5.readVariableSizeUnsigned(state, 2)
                } // readVariableSizeMax(maxNumRecordsPlusDesc);
            }

            // skip
            raf.readInt(state)
        }

        @Throws(IOException::class)
        fun recurse() {
            for (e in entries) {
                if (depth > 1) {
                    val node = InternalNode(e!!.childAddress, e.nrecords.toShort(), recordSize, depth - 1)
                    node.recurse()
                } else {
                    val nrecs = e!!.nrecords
                    val leaf = LeafNode(e.childAddress, nrecs.toShort())
                    leaf.addEntries(entryList)
                }
                if (e.record != null) { // last one is null
                    entryList.add(e)
                }
            }
        }
    }

    internal inner class LeafNode(address: Long, nrecords: Short) {
        val entries = mutableListOf<Entry2>()

        init {
            val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

            // header
            val magic = raf.readString(state, 4)
            check(magic == "BTLF") { "$magic should equal BTLF" }
            val version: Byte = raf.readByte(state)
            val nodeType = raf.readByte(state).toInt()
            check(nodeType == btreeType)

            for (i in 0 until nrecords) {
                val entry = Entry2()
                entry.record = readRecord(state, btreeType)
                entries.add(entry)
            }

            // skip checksum i guess
            raf.readInt(state)
        }

        fun addEntries(list: MutableList<Entry2>) {
            list.addAll(entries)
        }
    }

    @Throws(IOException::class)
    fun readRecord(state: OpenFileState, type: Int): Any {
        return when (type) {
            1 -> Record1(state)
            2 -> Record2(state)
            3 -> Record3(state)
            4 -> Record4(state)
            5 -> Record5(state)
            6 -> Record6(state)
            7 -> Record70(state) // TODO wrong
            8 -> Record8(state)
            9 -> Record9(state)
            10 -> Record10(state, 0) // TODO wrong, whats ndims?
            11 -> Record11(state, 0) // TODO wrong, whats ndims?
            else -> throw IllegalStateException()
        }
    }

    // Type 1 Record Layout - Indirectly Accessed, Non-filtered, ‘Huge’ Fractal Heap Objects
    internal inner class Record1(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val hugeObjectID = h5.readLength(state)
    }

    // Type 2 Record Layout - Indirectly Accessed, Filtered, ‘Huge’ Fractal Heap Objects
    internal inner class Record2(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val filterMask = raf.readInt(state)
        val hugeObjectSize = h5.readLength(state)
        val hugeObjectID = h5.readLength(state)
    }

    // Type 3 Record Layout - Directly Accessed, Non-filtered, ‘Huge’ Fractal Heap Objects
    internal inner class Record3(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
    }

    // Type 4 Record Layout - Directly Accessed, Filtered, ‘Huge’ Fractal Heap Objects
    internal inner class Record4(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val filterMask = raf.readInt(state)
        val hugeObjectSize = h5.readLength(state)
    }

    // Type 5 Record Layout - Link Name for Indexed Group
    inner class Record5(state: OpenFileState) {
        val nameHash = raf.readInt(state)
        val heapId = raf.readByteBuffer(state, 7).array()
    }

    // Type 6 Record Layout - Creation Order for Indexed Group
    inner class Record6(state: OpenFileState) {
        val creationOrder = raf.readLong(state)
        val heapId = raf.readByteBuffer(state, 7).array()
    }

    // Type 7 Record Layout - Shared Object Header Messages (Sub-type 0 - Message in Heap)
    internal inner class Record70(state: OpenFileState) {
        val location = raf.readByte(state)
        val hash = raf.readInt(state)
        val refCount = raf.readInt(state)
        val id = raf.readByteBuffer(state, 8).array()
    }

    // Type 7 Record Layout - Shared Object Header Messages (Sub-type 1 - Message in Object Header)
    internal inner class Record71(state: OpenFileState) {
        val location = raf.readByte(state)
        val hash = raf.readInt(state)
        val skip = raf.readByte(state)
        val messtype = raf.readByte(state)
        val index = raf.readShort(state)
        val address = h5.readOffset(state)
    }

    // Type 8 Record Layout - Attribute Name for Indexed Attributes
    inner class Record8(state: OpenFileState) {
        val heapId = raf.readByteBuffer(state, 8).array()
        val flags = raf.readByte(state)
        val creationOrder = raf.readInt(state)
        val nameHash = raf.readInt(state)
    }

    // Type 9 Record Layout - Creation Order for Indexed Attributes
    inner class Record9(state: OpenFileState) {
        val heapId = raf.readByteBuffer(state, 8).array()
        val flags = raf.readByte(state)
        val creationOrder = raf.readInt(state)
    }

    // Type 10 Record Layout - Non-filtered Dataset Chunks
    inner class Record10(state: OpenFileState, ndims : Int) {
        val address = h5.readOffset(state)
        val dims = LongArray(ndims) { raf.readLong(state) }
    }

    // Type 11 Record Layout - Filtered Dataset Chunks
    inner class Record11(state: OpenFileState, ndims : Int) {
        val address = h5.readOffset(state)
        val chunkSize = raf.readLong(state) // LOOK variable size based on what ?
        val filterMask = raf.readInt(state)
        val dims = LongArray(ndims) { raf.readLong(state) }
    }
} // BTree2
