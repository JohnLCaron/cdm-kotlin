package com.sunya.netchdf.hdf5

import com.sunya.cdm.dsl.structdsl
import com.sunya.cdm.iosp.OpenFileState
import java.nio.ByteOrder

// each GroupOld has one of these, see readGroupOld() in H5Group
// it uses a Btree1, which is not exposed
class GroupSymbolTable(val btreeAddress : Long) {

    fun symbolTableEntries(h5 : H5builder) : Iterable<SymbolTableEntry> {
        val btree = BTree1New(h5, btreeAddress, 0)
        val sentries = mutableListOf<SymbolTableEntry>()
        btree.readGroupEntries().forEach {
            sentries.add(h5.readSymbolTable(it.childAddress))
        }
        return sentries
    }

    // Level 1C - Symbol Table Entry
    internal fun H5builder.readSymbolTable(address : Long) : SymbolTableEntry {
        val state = OpenFileState(address, ByteOrder.LITTLE_ENDIAN)
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
}