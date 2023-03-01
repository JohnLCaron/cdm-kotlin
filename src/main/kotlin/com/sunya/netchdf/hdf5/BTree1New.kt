package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.Tiling
import java.nio.ByteOrder

/**
 * This holds the chunked data storage.
 * level 1A
 * A B-tree, version 1, used for data (node type 1)
 *
 * Version 1 B-trees in HDF5 files are a B-link tree, in which the sibling nodes at a particular level
 * in the tree are stored in a doubly-linked list.
 * The B-link trees implemented here contain one more key than the number of children.
 * In other words, each child pointer out of a B-tree node has a left key and a right key.
 *
 * The pointers in internal nodes point to sub-trees while the pointers in leaf nodes point to symbol nodes and
 * raw data chunks. Aside from that difference, internal nodes and leaf nodes are identical.
 */
class BTree1New(
    val h5: H5builder,
    val rootNodeAddress: Long,
    val nodeType : Int,  // 0 = group/symbol table, 1 = raw data chunks
    val varShape: IntArray = intArrayOf(), // not needed for group
    val storageSize: IntArray = intArrayOf(), // not needed for group
) {
    private val ndimStorage: Int = storageSize.size

    fun readGroupEntries() : Iterator<GroupEntry> {
        require(nodeType == 0)
        val root = Node(rootNodeAddress, null)
        if (root.level == 0) {
            return root.groupEntries.iterator()
        }
        // TODO recursion in case not contained in single node
        return emptyList<GroupEntry>().iterator()
    }

    // Btree nodes Level 1A1 - Version 1 B-trees
    inner class Node(val address: Long, val parent: BTree1New.Node?) {
        val level: Int
        val nentries: Int
        private val leftAddress: Long
        val rightAddress: Long

        // type 0
        val groupEntries = mutableListOf<BTree1New.GroupEntry>()

        // type 1
        val dataChunkEntries = mutableListOf<BTree1New.DataChunkEntry>()

        init {
            val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
            val magic: String = h5.raf.readString(state, 4)
            check(magic == "TREE") { "DataBTree doesnt start with TREE" }

            val type: Int = h5.raf.readByte(state).toInt()
            check(type == nodeType) { "DataBTree must be type $nodeType" }

            level = h5.raf.readByte(state).toInt() // leaf nodes are level 0
            nentries = h5.raf.readShort(state).toInt() // number of children to which this node points
            leftAddress = h5.readOffset(state)
            rightAddress = h5.readOffset(state)

            for (idx in 0 until nentries) {
                if (type == 0) {
                    val key = h5.readLength(state) // 4 or 8 bytes
                    val address = h5.readOffset(state) // 4 or 8 bytes
                    if (address > 0) groupEntries.add(GroupEntry(key, address))
                } else {
                    val chunkSize = h5.raf.readInt(state)
                    val filterMask = h5.raf.readInt(state)
                    val inner = IntArray(ndimStorage) { j ->
                        val loffset: Long = h5.raf.readLong(state)
                        require(loffset < Int.MAX_VALUE) // why?
                        loffset.toInt()
                    }
                    val key = DataChunkKey(chunkSize, filterMask, inner)
                    val childPointer = h5.readOffset(state) // 4 or 8 bytes
                    dataChunkEntries.add(DataChunkEntry(level, this, idx, key, childPointer))
                }
            }

            // note there may be unused entries, "All nodes of a particular type of tree have the same maximum degree,
            // but most nodes will point to less than that number of children""
        }
    }

    /** @param key the byte offset into the local heap for the first object name in the subtree which that key describes. */
    data class GroupEntry(val key : Long, val childAddress : Long)

    data class DataChunkKey(val chunkSize: Int, val filterMask : Int, val offsets: IntArray)

    // childAddress = data chunk (level 1) else a child node
    data class DataChunkEntry(val level : Int, val parent : Node, val idx : Int, val key : DataChunkKey, val childAddress : Long) {
        fun show(tiling : Tiling) : String = "chunkSize=${key.chunkSize}, chunk=${key.offsets.contentToString()}" +
                ", tile= ${tiling.tile(key.offsets).contentToString()}"
    }
}
