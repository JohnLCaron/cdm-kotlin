package com.sunya.netchdf.hdf5

import com.sunya.cdm.dsl.structdsl
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder

// Level 1A1 - Version 1 B-trees.
// The nodes are links to SymbolTableNode's I think
// aka GroupBTree from H5objects in netcdf-java library
// called from readGroupOld()

/*
Version 1 B-trees in HDF5 files are an implementation of the B-link tree. The sibling nodes at a particular level in the
tree are stored in a doubly-linked list. See the “Efficient Locking for Concurrent Operations on B-trees” paper by
Phillip Lehman and S. Bing Yao as published in the ACM Transactions on Database Systems, Vol. 6, No. 4, December 1981.

The B-trees implemented by the file format contain one more key than the number of children. In other words, each child
pointer out of a B-tree node has a left key and a right key. The pointers out of internal nodes point to sub-trees
while the pointers out of leaf nodes point to symbol nodes and raw data chunks. Aside from that difference, internal
nodes and leaf nodes are identical.

The version 1 B-trees are being phased out in favor of the version 2 B-trees
 */
internal class Btree1(val header : H5builder, val owner: String, address: Long) {
    val raf = header.raf
    var wantType = 0
    private val sentries: MutableList<SymbolTableEntry> = ArrayList() // list of type SymbolTableEntry

    init {
        val entryList = mutableListOf<Btree1.Entry>()
        readAllEntries(address, entryList)

        // now convert the entries to SymbolTableEntry
        for (e in entryList) {
            val node = SymbolTableNode(e.address)
            sentries.addAll(node.symbols)
        }
    }

    val symbolTableEntries: List<SymbolTableEntry>
        get() = sentries

    // recursively read all entries, place them in order in list
    @Throws(IOException::class)
    fun readAllEntries(address: Long, entryList: MutableList<Btree1.Entry>) {
        val state = OpenFileState(header.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
        val magic: String = raf.readString(state, 4)
        check(magic == "TREE") { "BtreeGroup doesnt start with TREE" }

        val type = raf.readByte(state).toInt()
        val level = raf.readByte(state).toInt()
        val nentries = raf.readShort(state)
        check(type == wantType) { "BtreeGroup must be type $wantType" }

        val size = 8 + 2 * header.sizeOffsets + nentries * (header.sizeOffsets + header.sizeLengths)
        val leftAddress: Long = header.readOffset(state)
        val rightAddress: Long = header.readOffset(state)

        // read all entries in this Btree "Node"
        val myEntries = mutableListOf<Btree1.Entry>()
        for (i in 0 until nentries) {
            myEntries.add(Entry(state))
        }
        if (level == 0) entryList.addAll(myEntries) else {
            for (entry in myEntries) {
                readAllEntries(entry.address, entryList)
            }
        }
    }

    // these are part of the level 1A data structure, type = 0
    internal inner class Entry(state : OpenFileState) {
        var key: Long
        var address: Long

        init {
            key = header.readLength(state)
            address = header.readOffset(state)
        }
    }

    // level 1B Group Symbol Table Nodes
    internal inner class SymbolTableNode(val address: Long) {
        var version: Byte
        var nentries: Short
        val symbols: MutableList<SymbolTableEntry> = ArrayList() // SymbolTableEntry

        init {
            val state = OpenFileState(header.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)

            // header
            val magic: String = raf.readString(state,4)
            check(magic == "SNOD") { "$magic should equal SNOD" }
            version = raf.readByte(state)
            raf.readByte(state) // skip byte
            nentries = raf.readShort(state)
            var posEntry = state.pos

            for (i in 0 until nentries) {
                val entry = header.readSymbolTable(state)
                posEntry += entry.dataSize
                if (entry.objectHeaderAddress != 0L) { // skip zeroes, probably a bug in HDF5 file format or docs, or me
                    symbols.add(entry)
                } else {
                    println("   BAD objectHeaderAddress==0 !! $entry")
                }
            }
            val size = (8 + nentries * 40).toLong()
        }
    }
} // Btree1

// Level 1C - Symbol Table Entry
internal fun H5builder.readSymbolTable(state : OpenFileState) : SymbolTableEntry {
    val rootEntry =
        structdsl("SymbolTableEntry", raf, state) {
            fld("linkNameOffset", sizeOffsets)
            fld("objectHeaderAddress", sizeOffsets)
            fld("cacheType", 4)
            skip(4)
            fld("scratchPad", 16)
            overlay("scratchPad", 0, "btreeAddress")
            overlay("scratchPad", sizeOffsets, "nameHeapAddress")
        }
    if (debugGroup) rootEntry.show()

    // may be btree or symbolic link
    var btreeAddress : Long? = null
    var nameHeapAddress : Long? = null
    var linkOffset : Int? = null
    var isSymbolicLink = false
    when (rootEntry.getInt("cacheType")) {
        0 -> {
            // no-op
        }
        1 -> {
            btreeAddress = rootEntry.getLong("btreeAddress")
            nameHeapAddress = rootEntry.getLong("nameHeapAddress")
        }
        2 -> {
            linkOffset = rootEntry.getInt("scratchPad")
            isSymbolicLink = true
        }
        else -> {
            throw IllegalArgumentException("SymbolTableEntry has unknown cacheType '${rootEntry.getInt("cacheType")}")
        }
    }

    return SymbolTableEntry(
        rootEntry.getLong("linkNameOffset"), // LOOK what about rootEntry.linkNameOffset.getLong()) sizeOffsets = Int ???
        rootEntry.getLong("objectHeaderAddress"),
        rootEntry.getInt("cacheType"),
        btreeAddress,
        nameHeapAddress,
        linkOffset,
        isSymbolicLink,
        rootEntry.dataSize(),
    )
}

// III.C. Disk Format: Level 1C - Symbol Table Entry (aka Group Entry)
internal data class SymbolTableEntry(
    val nameOffset: Long,
    val objectHeaderAddress: Long,
    val cacheType : Int,
    val btreeAddress: Long?,
    val nameHeapAddress: Long?,
    val linkOffset: Int?,
    val isSymbolicLink: Boolean,
    val dataSize : Int, // nbytes on disk
) {
    init {
        require(dataSize == 32 || dataSize == 40) // sanity check
    }
}

