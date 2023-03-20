package com.sunya.netchdf.hdf5

import com.sunya.cdm.dsl.structdsl
import com.sunya.cdm.iosp.OpenFileState
import java.nio.ByteOrder

/** Wraps a BTree1New, when its used to store symbol table nodes for GroupOld. */
internal class GroupSymbolTable(val btreeAddress : Long) {

    fun symbolTableEntries(h5: H5builder): Iterable<SymbolTableEntry> {
        val btree = BTree1(h5, btreeAddress, 0)
        val symbols = mutableListOf<SymbolTableEntry>()
        btree.readGroupEntries().forEach {
            readSymbolTableNode(h5, it.childAddress, symbols)
        }
        return symbols
    }

    // level 1B Group Symbol Table Nodes
    internal fun readSymbolTableNode(h5: H5builder, address: Long, symbols: MutableList<SymbolTableEntry>) {
        val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
        val magic: String = h5.raf.readString(state, 4)
        check(magic == "SNOD") { "$magic should equal SNOD" }
        state.pos += 2
        val nentries = h5.raf.readShort(state)

        var posEntry = state.pos
        for (i in 0 until nentries) {
            val entry = h5.readSymbolTable(state)
            posEntry += entry.dataSize
            if (entry.objectHeaderAddress != 0L) { // skip zeroes, probably a bug in HDF5 file format or docs, or me
                symbols.add(entry)
            } else {
                println("   BAD objectHeaderAddress==0 !! $entry")
            }
        }
    }
}

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
    when (val cacheType = rootEntry.getInt("cacheType")) {
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
            throw IllegalArgumentException("SymbolTableEntry has unknown cacheType=$cacheType")
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
data class SymbolTableEntry(
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