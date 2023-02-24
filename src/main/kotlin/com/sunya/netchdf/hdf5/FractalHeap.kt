package com.sunya.netchdf.hdf5

import mu.KotlinLogging
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.util.log2
import java.io.IOException
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList

/** Level 1G - Fractal Heap  */
class FractalHeap(h5: H5builder, forWho: String, address: Long, memTracker: MemTracker?) {
    // level 1E "Fractal Heap" used for both Global and Local heaps in 1.8.0+
    /*
   * 1) the root indirect block knows how many rows it has from the header, which i can divide into
   * direct and indirect using:
   * 
   * int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2;
   * 
   * in the example file i have, maxDirectBlockSize = 216, startingBlockSize = 2^10, tableWidth = 4, so
   * maxrows = 8. So I will see 8 rows, with direct sizes:
   * 2^10, 2^10, 2^11, 2^12, 2^13, 2^14, 2^15, 2^16
   * 
   * So if nrows > 8, I will see indirect rows of size
   * 2^17, 2^18, .....
   * 
   * this value is the <indirect block size>.
   * 
   * 2) now read a 1st level indirect block of size 217:
   * 
   * <iblock_nrows> = lg2(<indirect block size>) - lg2(<starting block size) - lg2(<doubling_table_width>)) + 1
   * 
   * <iblock_nrows> = 17 - 10 - 2 + 1 = 6.
   * 
   * All indirect blocks of "size" 2^17 will have: (for the parameters above)
   * row 0: (direct blocks): 4 x 2^10 = 2^12
   * row 1: (direct blocks): 4 x 2^10 = 2^12
   * row 2: (direct blocks): 4 x 2^11 = 2^13
   * row 3: (direct blocks): 4 x 2^12 = 2^14
   * row 4: (direct blocks): 4 x 2^13 = 2^15
   * row 5: (direct blocks): 4 x 2^14 = 2^16
   * ===============
   * Total size: 2^17
   * 
   * Then there are 7 rows for indirect block of size 218, 8 rows for indirect block of size 219, etc.
   * An indirect block of size 2^20 will have nine rows, the last one of which are indirect blocks that are size 2^17,
   * an indirect block of size 2^21 will have ten rows, the last two rows of which are indirect blocks that are size
   * 2^17 & 2^18, etc.
   * 
   * One still uses
   * 
   * int maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2
   * 
   * Where startingBlockSize is from the header, ie the same for all indirect blocks.
   * 
   * 
   */
    private val debugOut = System.out
    private val h5: H5builder
    private val raf: OpenFile
    var version: Int
    var heapIdLen: Short
    var flags: Byte
    var maxSizeOfObjects: Int
    var nextHugeObjectId: Long
    var freeSpace: Long
    var managedSpace: Long
    var allocatedManagedSpace: Long
    var offsetDirectBlock: Long
    var nManagedObjects: Long
    var sizeHugeObjects: Long
    var nHugeObjects: Long
    var sizeTinyObjects: Long
    var nTinyObjects: Long
    var btreeAddressHugeObjects: Long
    var freeSpaceTrackerAddress: Long
    var maxHeapSize: Short
    var startingNumRows: Short
    var currentNumRows: Short
    var maxDirectBlockSize: Long
    var tableWidth: Short
    var startingBlockSize: Long
    var rootBlockAddress: Long
    var rootBlock: IndirectBlock

    // filters
    var ioFilterLen: Short
    var sizeFilteredRootDirectBlock: Long = 0
    var ioFilterMask = 0
    var doublingTable: DoublingTable
    var btreeHugeObjects: BTree2? = null

    init {
        this.h5 = h5
        raf = h5.raf

        val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
        if (debugDetail) debugOut.println("-- readFractalHeap position=" + state.pos)

        // header
        val magic: String = raf.readString(state,4)
        if (magic != "FRHP") throw IllegalStateException("$magic should equal FRHP")
        version = raf.readByte(state).toInt()
        heapIdLen = raf.readShort(state) // bytes
        ioFilterLen = raf.readShort(state) // bytes
        flags = raf.readByte(state)
        maxSizeOfObjects = raf.readInt(state) // greater than this are huge objects
        nextHugeObjectId = h5.readLength(state) // next id to use for a huge object
        btreeAddressHugeObjects = h5.readOffset(state) // v2 btee to track huge objects
        freeSpace = h5.readLength(state) // total free space in managed direct blocks
        freeSpaceTrackerAddress = h5.readOffset(state)
        managedSpace = h5.readLength(state) // total amount of managed space in the heap
        allocatedManagedSpace = h5.readLength(state) // total amount of managed space in the heap actually allocated
        offsetDirectBlock = h5.readLength(state) // linear heap offset where next direct block should be allocated
        nManagedObjects = h5.readLength(state) // number of managed objects in the heap
        sizeHugeObjects = h5.readLength(state) // total size of huge objects in the heap (in bytes)
        nHugeObjects = h5.readLength(state) // number huge objects in the heap
        sizeTinyObjects = h5.readLength(state) // total size of tiny objects packed in heap Ids (in bytes)
        nTinyObjects = h5.readLength(state) // number of tiny objects packed in heap Ids
        tableWidth = raf.readShort(state) // number of columns in the doubling table for managed blocks, must be power of 2
        startingBlockSize = h5.readLength(state) // starting direct block size in bytes, must be power of 2
        maxDirectBlockSize = h5.readLength(state) // maximum direct block size in bytes, must be power of 2
        maxHeapSize = raf.readShort(state) // log2 of the maximum size of heap's linear address space, in bytes
        startingNumRows = raf.readShort(state) // starting number of rows of the root indirect block, 0 = maximum needed
        rootBlockAddress = h5.readOffset(state) // This is the address of the root block for the heap.
        // It can be the undefined address if there is no data in the heap.
        // It either points to a direct block (if the Current # of Rows in the Root
        // Indirect Block value is 0), or an indirect block.
        currentNumRows = raf.readShort(state) // current number of rows of the root indirect block, 0 = direct block
        val hasFilters = ioFilterLen > 0
        if (hasFilters) {
            sizeFilteredRootDirectBlock = h5.readLength(state)
            ioFilterMask = raf.readInt(state)
            val ioFilterInfo = raf.readByteBuffer(state, ioFilterLen.toInt()).array()
        }
        val checksum: Int = raf.readInt(state)
        if (debugDetail || debugFractalHeap) {
            debugOut.println(
                "FractalHeap for " + forWho + " version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen="
                        + ioFilterLen + " flags= " + flags
            )
            debugOut.println(
                (" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId
                        + " btreeAddress=" + btreeAddressHugeObjects + " managedSpace=" + managedSpace + " allocatedManagedSpace="
                        + allocatedManagedSpace + " freeSpace=" + freeSpace)
            )
            debugOut.println(
                (" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects="
                        + nTinyObjects + " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize)
            )
            debugOut.println(" DoublingTable: tableWidth=$tableWidth startingBlockSize=$startingBlockSize")
            debugOut.println(
                (" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows
                        + " currentNumRows=" + currentNumRows)
            )
        }
        if (debugPos) debugOut.println("    *now at position=" + state.pos)
        val pos: Long = state.pos
        if (debugDetail) debugOut.println("-- end FractalHeap position=" + state.pos)
        val hsize: Int = 8 + (2 * h5.sizeLengths) + h5.sizeOffsets
        memTracker?.add("Group FractalHeap ($forWho)", address, pos)
        doublingTable = DoublingTable(tableWidth.toInt(), startingBlockSize, allocatedManagedSpace, maxDirectBlockSize)

        // data
        rootBlock = IndirectBlock(currentNumRows.toInt(), startingBlockSize)
        val cstate = state.copy(pos = h5.getFileOffset(rootBlockAddress))
        if (currentNumRows.toInt() == 0) {
            val dblock = DataBlock()
            doublingTable.blockList.add(dblock)
            readDirectBlock(cstate, address, dblock)
            dblock.size = startingBlockSize // - dblock.extraBytes; // removed 10/1/2013
            rootBlock.add(dblock)
        } else {
            readIndirectBlock(rootBlock, cstate, address, hasFilters)

            // read in the direct blocks
            for (dblock: DataBlock in doublingTable.blockList) {
                if (dblock.address > 0) {
                    val cstate2 = state.copy(pos = h5.getFileOffset(dblock.address))
                    readDirectBlock(cstate2, address, dblock)
                    // dblock.size -= dblock.extraBytes; // removed 10/1/2013
                }
            }
        }
    }

    fun showDetails(f: Formatter) {
        f.format(
            ("FractalHeap version=" + version + " heapIdLen=" + heapIdLen + " ioFilterLen=" + ioFilterLen + " flags= "
                    + flags + "%n")
        )
        f.format(
            (" maxSizeOfObjects=" + maxSizeOfObjects + " nextHugeObjectId=" + nextHugeObjectId + " btreeAddress="
                    + btreeAddressHugeObjects + " managedSpace=" + managedSpace + " allocatedManagedSpace=" + allocatedManagedSpace
                    + " freeSpace=" + freeSpace + "%n")
        )
        f.format(
            (" nManagedObjects=" + nManagedObjects + " nHugeObjects= " + nHugeObjects + " nTinyObjects=" + nTinyObjects
                    + " maxDirectBlockSize=" + maxDirectBlockSize + " maxHeapSize= 2^" + maxHeapSize + "%n")
        )
        f.format(
            (" rootBlockAddress=" + rootBlockAddress + " startingNumRows=" + startingNumRows + " currentNumRows="
                    + currentNumRows + "%n%n")
        )
        rootBlock.showDetails(f)
        // doublingTable.showDetails(f);
    }

    fun getFractalHeapId(heapId: ByteArray): DHeapId {
        return DHeapId(heapId)
    }

    inner class DHeapId internal constructor(heapId: ByteArray) {
        val type: Int
        var subtype = 0 // 1 = indirect no filter, 2 = indirect, filter 3 = direct, no filter, 4 = direct, filter
        var n = 0 // the offset field size
        var m = 0
        var offset = 0 // This field is the offset of the object in the heap.
        var size = 0 // This field is the length of the object in the heap

        init {
            type = (heapId[0].toInt() and 0x30) shr 4
            if (type == 0) {
                n = maxHeapSize / 8
                // The minimum number of bytes necessary to encode the Maximum Heap Size value
                m = h5.getNumBytesFromMax(maxDirectBlockSize - 1)
                // The length of the object in the heap, determined by taking the minimum value of
                // Maximum Direct Block Size and Maximum Size of Managed Objects in the Fractal Heap Header.
                // Again, the minimum number of bytes needed to encode that value is used for the size of this field.
                offset = h5.makeIntFromBytes(heapId, 1, n)
                size = h5.makeIntFromBytes(heapId, 1 + n, m)
            } else if (type == 1) {
                // how fun to guess the subtype
                val hasBtree = (btreeAddressHugeObjects > 0)
                val hasFilters = (ioFilterLen > 0)
                if (hasBtree) subtype = if (hasFilters) 2 else 1 else subtype = if (hasFilters) 4 else 3
                when (subtype) {
                    1, 2 -> offset = h5.makeIntFromBytes(heapId, 1, (heapId.size - 1))
                }
            } else if (type == 2) {
                // The sub-type for tiny heap IDs depends on whether the heap ID is large enough to store objects greater
                // than T16 bytes or not. If the heap ID length is 18 bytes or smaller, the "normal" tiny heap ID form
                // is used. If the heap ID length is greater than 18 bytes in length, the "extented" form is used.
                subtype = if ((heapId.size <= 18)) 1 else 2 // 0 == normal, 1 = extended
            } else {
                throw UnsupportedOperationException() // "DHeapId subtype ="+subtype);
            }
        }

        fun getPos(): Long {
                when (type) {
                    0 -> return doublingTable.getPos(offset.toLong())
                    1 -> {
                        run {
                            when (subtype) {
                                1, 2 -> {
                                    val btree = if (btreeHugeObjects == null) {
                                        val local = BTree2(h5, "FractalHeap btreeHugeObjects", btreeAddressHugeObjects)
                                        require(local.btreeType.toInt() == subtype)
                                        local
                                    } else btreeHugeObjects!!

                                    val record1: BTree2.Record1? = btree.getEntry1(offset)
                                    if (record1 == null) {
                                        btree.getEntry1(offset) // debug
                                        throw RuntimeException("Cant find DHeapId=" + offset)
                                    }
                                    return record1.hugeObjectAddress
                                }

                                3, 4 -> return offset.toLong() // guess
                                else -> throw RuntimeException("Unknown DHeapId subtype =$subtype")
                            }
                        }
                    }
                    else -> throw RuntimeException("Unknown DHeapId type =$type")
                }
            }

        override fun toString(): String {
            return "$type,$n,$m,$offset,$size"
        }

        @Throws(IOException::class)
        fun show(f: Formatter) {
            f.format("   %2d %2d %2d %6d %4d %8d", type, n, m, offset, size, getPos())
        }
    }

    inner class DoublingTable internal constructor(
        val tableWidth: Int,
        val startingBlockSize: Long,
        val managedSpace: Long,
        val maxDirectBlockSize: Long
    ) {
        val blockList = mutableListOf<DataBlock>()

        private fun calcNrows(max: Long): Int {
            var n = 0
            var sizeInBytes: Long = 0
            var blockSize = startingBlockSize
            while (sizeInBytes < max) {
                sizeInBytes += blockSize * tableWidth
                n++
                if (n > 1) blockSize *= 2
            }
            return n
        }

        private fun assignSizes() {
            var block = 0
            var blockSize = startingBlockSize
            for (db: DataBlock in blockList) {
                db.size = blockSize
                block++
                if ((block % tableWidth == 0) && (block / tableWidth > 1)) blockSize *= 2
            }
        }

        fun getPos(offset: Long): Long {
            var block = 0
            for (db: DataBlock in blockList) {
                if (db.address < 0) continue
                if ((offset >= db.offset) && (offset <= db.offset + db.size)) {
                    val localOffset = offset - db.offset
                    return db.dataPos + localOffset
                }
                block++
            }
            logger.error("DoublingTable: illegal offset=$offset")
            throw IllegalStateException("offset=$offset")
        }

        fun showDetails(f: Formatter) {
            f.format(
                " DoublingTable: tableWidth= %d startingBlockSize = %d managedSpace=%d maxDirectBlockSize=%d%n",
                tableWidth, startingBlockSize, managedSpace, maxDirectBlockSize
            )
            f.format(" DataBlocks:%n")
            f.format("  address            dataPos            offset size%n")
            for (dblock: DataBlock in blockList) {
                f.format("  %#-18x %#-18x %5d  %4d%n", dblock.address, dblock.dataPos, dblock.offset, dblock.size)
            }
        }
    }

    inner class IndirectBlock internal constructor(var nrows: Int, val size: Long) {
        var directRows = 0
        var indirectRows = 0
        val directBlocks = mutableListOf<DataBlock>()
        var indirectBlocks = mutableListOf<IndirectBlock>()

        init {
            if (nrows < 0) {
                nrows = log2(size) - log2(startingBlockSize * tableWidth) + 1 // LOOK
            }
            val maxrows_directBlocks = (log2(maxDirectBlockSize) - log2(startingBlockSize)) + 2 // LOOK
            if (nrows < maxrows_directBlocks) {
                directRows = nrows
                indirectRows = 0
            } else {
                directRows = maxrows_directBlocks
                indirectRows = (nrows - maxrows_directBlocks)
            }
            if (debugFractalHeap) debugOut.println("  readIndirectBlock directChildren$directRows indirectChildren= $indirectRows")
        }

        fun add(dblock: DataBlock) {
            directBlocks.add(dblock)
        }

        fun add(iblock: IndirectBlock) {
            indirectBlocks.add(iblock)
        }

        fun showDetails(f: Formatter) {
            f.format(
                "%n IndirectBlock: nrows= %d directRows = %d indirectRows=%d startingSize=%d%n", nrows, directRows,
                indirectRows, size
            )
            f.format(" DataBlocks:%n")
            f.format("  address            dataPos            offset size end%n")
            for (dblock: DataBlock in directBlocks) f.format(
                "  %#-18x %#-18x %5d  %4d %5d %n", dblock.address, dblock.dataPos, dblock.offset, dblock.size,
                (dblock.offset + dblock.size)
            )
            for (iblock: IndirectBlock in indirectBlocks) iblock.showDetails(f)
        }
    }

    class DataBlock() {
        var address: Long = 0
        var sizeFilteredDirectBlock: Long = 0
        var filterMask = 0
        var dataPos: Long = 0
        var offset: Long = 0
        var size: Long = 0
        var extraBytes = 0
        var wasRead = false // when empty, object exists, but fields are not init. not yet sure where to use.
        override fun toString(): String {
            return "DataBlock{offset=$offset, size=$size, dataPos=$dataPos}"
        }
    }

    @Throws(IOException::class)
    fun readIndirectBlock(iblock: IndirectBlock, state: OpenFileState, heapAddress: Long, hasFilter: Boolean) {
        // header
        val magic: String = raf.readString(state,4)
        if (magic != "FHIB") throw IllegalStateException("$magic should equal FHIB")
        val version: Byte = raf.readByte(state)
        val heapHeaderAddress: Long = h5.readOffset(state)
        if (heapAddress != heapHeaderAddress) throw IllegalStateException()
        var nbytes = maxHeapSize / 8
        if (maxHeapSize % 8 != 0) nbytes++
        val blockOffset: Long = h5.readVariableSizeUnsigned(state, nbytes)
        if (debugDetail || debugFractalHeap) {
            debugOut.println(" -- FH IndirectBlock version=$version blockOffset= $blockOffset")
        }
        val npos: Long = state.pos
        if (debugPos) debugOut.println("    *now at position=$npos")

        // child direct blocks
        var blockSize = startingBlockSize
        for (row in 0 until iblock.directRows) {
            if (row > 1) blockSize *= 2
            for (i in 0 until doublingTable.tableWidth) {
                val directBlock = DataBlock()
                iblock.add(directBlock)
                directBlock.address = h5.readOffset(state) // This field is the address of the child direct block. The size of the
                // [uncompressed] direct block can be computed by its offset in the
                // heap's linear address space.
                if (hasFilter) {
                    directBlock.sizeFilteredDirectBlock = h5.readLength(state)
                    directBlock.filterMask = raf.readInt(state)
                }
                if (debugDetail || debugFractalHeap) debugOut.println("  DirectChild " + i + " address= " + directBlock.address)
                directBlock.size = blockSize

                // if (directChild.address >= 0)
                doublingTable.blockList.add(directBlock)
            }
        }

        // child indirect blocks
        for (row in 0 until iblock.indirectRows) {
            blockSize *= 2
            for (i in 0 until doublingTable.tableWidth) {
                val iblock2 = IndirectBlock(-1, blockSize)
                iblock.add(iblock2)
                val childIndirectAddress: Long = h5.readOffset(state)
                val cstate = state.copy(pos = childIndirectAddress)
                if (debugDetail || debugFractalHeap) debugOut.println("  InDirectChild $row address= $childIndirectAddress")
                if (childIndirectAddress >= 0) readIndirectBlock(iblock2, cstate, heapAddress, hasFilter)
            }
        }
    }

    @Throws(IOException::class)
    fun readDirectBlock(state: OpenFileState, heapAddress: Long, dblock: DataBlock) {
        if (state.pos < 0) return  // means its empty
        val startPos = state.pos

        // header
        val magic: String = raf.readString(state,4)
        if (magic != "FHDB") throw IllegalStateException("$magic should equal FHDB")
        val version = raf.readByte(state)
        val heapHeaderAddress = h5.readOffset(state) // This is the address for the fractal heap header that this block belongs
        // to. This field is principally used for file integrity checking.
        if (heapAddress != heapHeaderAddress) throw IllegalStateException()
        // // keep track of how much room is taken out of block size, that is, how much is left for the object
        dblock.extraBytes = 5
        dblock.extraBytes += if (h5.isOffsetLong) 8 else 4
        var nbytes = maxHeapSize / 8
        if (maxHeapSize % 8 != 0) nbytes++
        // This is the offset of the block within the fractal heap's address space (in bytes).
        dblock.offset = h5.readVariableSizeUnsigned(state, nbytes)
        dblock.dataPos = startPos  // offsets are from the start of the block

        dblock.extraBytes += nbytes
        if ((flags.toInt() and 2) != 0) dblock.extraBytes += 4 // ?? size of checksum
        dblock.wasRead = true
        if (debugDetail || debugFractalHeap) {
            debugOut.println("  DirectBlock offset= " + dblock.offset + " dataPos = " + dblock.dataPos)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger("H5builder")
        var debugDetail = false
        var debugFractalHeap = false
        var debugPos = false
    }
} // FractalHeap
