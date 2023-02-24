package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder
import java.util.*

/**
 * // Level 1A2
 *
 * Version 2 B-trees are "traditional" B-trees, with one major difference. Instead of just using a simple pointer
 * (or address in the file) to a child of an internal node, the pointer to the child node contains two additional
 * pieces of information: the number of records in the child node itself, and the total number of records in the child
 * node and all its descendents. Storing this additional information allows fast array-like indexing to locate the n'th
 * record in the B-tree.
 *
 * The entry into a version 2 B-tree is a header which contains global information about the structure of the B-tree.
 * The root node address field in the header points to the B-tree root node, which is either an internal or leaf node,
 * depending on the value in the header's depth field. An internal node consists of records plus pointers to further
 * leaf or internal nodes in the tree. A leaf node consists of solely of records. The format of the records depends on
 * the B-tree type.
 *
 * Used in readGroupNew(), readAttributesFromInfoMessage(), FractalHeap.
 */
class BTree2(h5: H5builder, owner: String, address: Long) {
    private val debugBtree2 = false
    private val debugPos = false
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
        val split: Byte = raf.readByte(state)
        val merge: Byte = raf.readByte(state)
        val rootNodeAddress: Long = h5.readOffset(state)
        val numRecordsRootNode: Short = raf.readShort(state)
        val totalRecords: Long = h5.readLength(state) // total in entire btree
        val checksum: Int = raf.readInt(state)
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
                entry.record = readRecord(state, btreeType.toInt())
                entries.add(entry)
            }

            // skip
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
            7 -> {
                Record70(state) // TODO wrong
            }

            8 -> Record8(state)
            9 -> Record9(state)
            else -> throw IllegalStateException()
        }
    }

    internal inner class Record1(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val hugeObjectID = h5.readLength(state)
    }

    internal inner class Record2(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val filterMask = raf.readInt(state)
        val hugeObjectSize = h5.readLength(state)
        val hugeObjectID = h5.readLength(state)
    }

    internal inner class Record3(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
    }

    internal inner class Record4(state: OpenFileState) {
        val hugeObjectAddress = h5.readOffset(state)
        val hugeObjectLength = h5.readLength(state)
        val filterMask = raf.readInt(state)
        val hugeObjectSize = h5.readLength(state)
    }

    inner class Record5(state: OpenFileState) {
        val nameHash = raf.readInt(state)
        val heapId = raf.readByteBuffer(state, 7).array()
    }

    inner class Record6(state: OpenFileState) {
        val creationOrder = raf.readLong(state)
        val heapId = raf.readByteBuffer(state, 7).array()
    }

    internal inner class Record70(state: OpenFileState) {
        val location = raf.readByte(state)
        val refCount = raf.readInt(state)
        val id = raf.readByteBuffer(state, 8).array()
    }

    internal inner class Record71(state: OpenFileState) {
        val location = raf.readByte(state)
        val skip = raf.readByte(state)
        val messtype = raf.readByte(state)
        val index = raf.readShort(state)
        val address = h5.readOffset(state)
    }

    inner class Record8(state: OpenFileState) {
        val heapId = raf.readByteBuffer(state, 8).array()
        val flags = raf.readByte(state)
        val creationOrder = raf.readInt(state)
        val nameHash = raf.readInt(state)
    }

    inner class Record9(state: OpenFileState) {
        val heapId = raf.readByteBuffer(state, 8).array()
        val flags = raf.readByte(state)
        val creationOrder = raf.readInt(state)
    }
} // BTree2
