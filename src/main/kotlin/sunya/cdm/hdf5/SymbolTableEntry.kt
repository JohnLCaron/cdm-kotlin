package sunya.cdm.hdf5

import sunya.cdm.dsl.structdsl
import sunya.cdm.iosp.OpenFileState

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

// aka Group Entry "level 1C"
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