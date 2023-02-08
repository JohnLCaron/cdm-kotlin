package sunya.cdm.hdf5

import sunya.cdm.iosp.OpenFile
import sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder
import java.util.*

/**
 * // Level 1A2
 *
 * These are used for symbols, not data i think. Version 1 is H5builder.GroupBTree.
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
 * leaf
 * or internal nodes in the tree. A leaf node consists of solely of records. The format of the records depends on the
 * B-tree type (stored in the header).
 */
class BTree2(h5: H5builder, owner: String, address: Long) {
    private val debugBtree2 = false
    private val debugPos = false
    private val debugOut = System.out
    val btreeType: Int
    private val nodeSize : Int // size in bytes of btree nodes
    private val recordSize : Short// size in bytes of btree records
    private val owner: String
    private val h5: H5builder
    private val raf : OpenFile
    val entryList: MutableList<Entry2> = ArrayList()

    init {
        this.h5 = h5
        raf = h5.raf
        this.owner = owner
        val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

        // header
        val magic = raf.readString(state,4)
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
        if (debugBtree2) {
            debugOut.printf(
                "BTree2 (%s) version=%d type=%d treeDepth=%d nodeSize=%d recordSize=%d numRecordsRootNode=%d totalRecords=%d rootNodeAddress=%d%n",
                owner, version, btreeType, treeDepth, nodeSize, recordSize, numRecordsRootNode, totalRecords,
                rootNodeAddress
            )
        }
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
            val record1 = entry!!.record as Record1?
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
            if (debugPos) debugOut.println("--Btree2 InternalNode position=" + state.pos)

            // header
            val magic = raf.readString(state,4)
            check(magic == "BTIN") { "$magic should equal BTIN" }
            val version: Byte = raf.readByte(state)
            val nodeType = raf.readByte(state).toInt()
            check(nodeType == btreeType)
            if (debugBtree2) debugOut.println("   BTree2 InternalNode version=$version type=$nodeType nrecords=$nrecords")
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
                e.nrecords = h5.readVariableSizeUnsigned(state,1) // readVariableSizeMax(maxNumRecords);
                if (depth > 1) e.totNrecords =
                    h5.readVariableSizeUnsigned(state,2) // readVariableSizeMax(maxNumRecordsPlusDesc);
                if (debugBtree2) debugOut.println(
                    " BTree2 entry childAddress=" + e.childAddress + " nrecords=" + e.nrecords + " totNrecords="
                            + e.totNrecords
                )
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
                if (e.record != null) // last one is null
                    entryList.add(e)
            }
        }
    }

    internal inner class LeafNode(address: Long, nrecords: Short) {
        val entries = mutableListOf<Entry2>()

        init {
            val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
            if (debugPos) debugOut.println("--Btree2 InternalNode position=" + state.pos)

            // header
            val magic = raf.readString(state,4)
            check(magic == "BTLF") { "$magic should equal BTLF" }
            val version: Byte = raf.readByte(state)
            val nodeType = raf.readByte(state).toInt()
            check(nodeType == btreeType)
            if (debugBtree2) debugOut.println("   BTree2 LeafNode version=$version type=$nodeType nrecords=$nrecords")
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
    fun readRecord(state : OpenFileState, type: Int): Any {
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

    internal inner class Record1(state : OpenFileState) {
        var hugeObjectAddress: Long
        var hugeObjectLength: Long
        var hugeObjectID: Long

        init {
            hugeObjectAddress = h5.readOffset(state)
            hugeObjectLength = h5.readLength(state)
            hugeObjectID = h5.readLength(state)
        }
    }

    internal inner class Record2(state : OpenFileState) {
        var hugeObjectAddress: Long
        var hugeObjectLength: Long
        var hugeObjectID: Long
        var hugeObjectSize: Long
        var filterMask: Int

        init {
            hugeObjectAddress = h5.readOffset(state)
            hugeObjectLength = h5.readLength(state)
            filterMask = raf.readInt(state)
            hugeObjectSize = h5.readLength(state)
            hugeObjectID = h5.readLength(state)
        }
    }

    internal inner class Record3(state : OpenFileState) {
        var hugeObjectAddress: Long
        var hugeObjectLength: Long

        init {
            hugeObjectAddress = h5.readOffset(state)
            hugeObjectLength = h5.readLength(state)
        }
    }

    internal inner class Record4(state : OpenFileState) {
        var hugeObjectAddress: Long
        var hugeObjectLength: Long
        var hugeObjectID: Long = 0
        var hugeObjectSize: Long
        var filterMask: Int

        init {
            hugeObjectAddress = h5.readOffset(state)
            hugeObjectLength = h5.readLength(state)
            filterMask = raf.readInt(state)
            hugeObjectSize = h5.readLength(state)
        }
    }

    inner class Record5(state : OpenFileState) {
        var nameHash: Int
        val heapId : ByteArray

        init {
            nameHash = raf.readInt(state)
            heapId = raf.readByteBuffer(state, 7).array()
            if (debugBtree2) debugOut.println("  record5 nameHash=" + nameHash + " heapId=" + Arrays.toString(heapId))
        }
    }

    inner class Record6(state : OpenFileState) {
        var creationOrder: Long
        val heapId : ByteArray

        init {
            creationOrder = raf.readLong(state)
            heapId = raf.readByteBuffer(state, 7).array()
            if (debugBtree2) debugOut.println(
                "  record6 creationOrder=" + creationOrder + " heapId=" + Arrays.toString(
                    heapId
                )
            )
        }
    }

    internal inner class Record70(state : OpenFileState) {
        var location: Byte
        var refCount: Int
        val id : ByteArray

        init {
            location = raf.readByte(state)
            refCount = raf.readInt(state)
            id = raf.readByteBuffer(state, 8).array()
        }
    }

    internal inner class Record71(state : OpenFileState) {
        var location: Byte
        var messtype: Byte
        var index: Short
        var address: Long

        init {
            location = raf.readByte(state)
            raf.readByte(state) // skip a byte
            messtype = raf.readByte(state)
            index = raf.readShort(state)
            address = h5.readOffset(state)
        }
    }

    inner class Record8(state : OpenFileState) {
        var flags: Byte
        var creationOrder: Int
        var nameHash: Int
        val heapId : ByteArray

        init {
            heapId = raf.readByteBuffer(state, 8).array()
            flags = raf.readByte(state)
            creationOrder = raf.readInt(state)
            nameHash = raf.readInt(state)
            if (debugBtree2) debugOut.println(
                "  record8 creationOrder=" + creationOrder + " heapId=" + Arrays.toString(
                    heapId
                )
            )
        }
    }

    inner class Record9(state : OpenFileState) {
        var flags: Byte
        var creationOrder: Int
        val heapId : ByteArray

        init {
            heapId = raf.readByteBuffer(state, 8).array()
            flags = raf.readByte(state)
            creationOrder = raf.readInt(state)
        }
    }
} // BTree2
