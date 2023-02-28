package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Section
import com.sunya.cdm.iosp.LayoutTiled
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.iosp.TilingOld
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
 * NOT USED
 */
class BTreeDataNew(
    val h5: H5builder,
    val rootNodeAddress: Long,
    varShape: IntArray,
    storageSize: IntArray,
) {
    private val raf: OpenFile = h5.raf
    private val tiling: TilingOld = TilingOld(varShape, storageSize)
    private val ndimStorage: Int = storageSize.size
    private val wantType: Int = 1 // 0 = group nodes, 1 = raw data chunks

    private val nodes = mutableMapOf<Long, Node>()
    private var owner: Any? = null

    fun setOwner(owner: Any?) {
        this.owner = owner
    }

    // used by H5tiledLayoutBB
    fun getDataChunkIteratorFilter(want: Section): BTreeDataNew.DataChunkIterator {
        return DataChunkIterator(want)
    }

    // used by H5tiledLayout
    fun getDataChunkIteratorNoFilter(want: Section, nChunkDim: Int): LayoutTiled.DataChunkIterator {
        return DataChunkIteratorNoFilter(want, nChunkDim)
    }

    // An Iterator over the DataChunks in the btree.
    // returns the actual data from the btree leaf (level 0) nodes.
    // used by H5tiledLayout, when there are no filters
    // no caching i think
    /**
     * @param want skip any nodes that are before this section
     * @param nChunkDim number of chunk dimensions - may be less than the offset[] length
     */
    internal inner class DataChunkIteratorNoFilter(want: Section, private val nChunkDim: Int) :
        LayoutTiled.DataChunkIterator {

        private val root: BTreeDataNew.Node

        init {
            root = Node(rootNodeAddress, -1) // should we cache the nodes ???
            if (nodes[rootNodeAddress] != null) {
                println("HEY already read node at $rootNodeAddress")
            }
            nodes[rootNodeAddress] = root
            root.first(want.origin)
        }

        override operator fun hasNext(): Boolean {
            return root.hasNext() // && !node.greaterThan(wantOrigin);
        }

        override operator fun next(): LayoutTiled.DataChunk {
            val dataChunk: BTreeDataNew.DataChunk = root.next()
            var offset: IntArray = dataChunk.key.offsets
            if (offset.size > nChunkDim) { // may have to eliminate last offset
                offset = IntArray(nChunkDim)
                System.arraycopy(dataChunk.key.offsets, 0, offset, 0, nChunkDim)
            }
            return LayoutTiled.DataChunk(offset, dataChunk.filePos)
        }
    }

    // An Iterator over the DataChunks in the btree.
    // returns the DataChunks contained in the leaf (level 0) nodes
    /** @param want skip any nodes that are before this section */
    inner class DataChunkIterator internal constructor(want: Section) {
        private val root: BTreeDataNew.Node
        private val wantOrigin: IntArray

        init {
            root = Node(rootNodeAddress, -1) // should we cache the nodes ???
            if (nodes[rootNodeAddress] != null) {
                println("HEY already read node at $rootNodeAddress")
            }
            nodes[rootNodeAddress] = root
            wantOrigin = want.origin
            root.first(wantOrigin)
        }

        operator fun hasNext(): Boolean {
            return root.hasNext() // && !node.greaterThan(wantOrigin);
        }

        operator fun next(): BTreeDataNew.DataChunk {
            return root.next()
        }
    }

    // Btree nodes
    private inner class Node(val address: Long, parent: Long) {
        private val level: Int
        private val nentries: Int
        private var currentNode: BTreeDataNew.Node? = null

        // level 0 only
        private val myDataChunks = mutableListOf<BTreeDataNew.DataChunk>()

        // level > 0 only
        private val keys = mutableListOf<Key>() // int[nentries][ndim]; // other levels
        private val childPointers = mutableListOf<Long>() // long[nentries];

        private var currentEntry = 0 // track iteration; TODO why not an iterator ??

        init {
            val state = OpenFileState(h5.getFileOffset(address), ByteOrder.LITTLE_ENDIAN)
            val magic: String = raf.readString(state, 4)
            check(magic == "TREE") { "DataBTree doesnt start with TREE" }

            val type: Int = raf.readByte(state).toInt()
            check(type == wantType) { "DataBTree must be type $wantType" }

            level = raf.readByte(state).toInt() // leaf nodes are level 0
            nentries = raf.readShort(state).toInt() // number of children to which this node points
            // val size: Long = 8 + 2 * h5.sizeOffsets + nentries.toLong() * (8 + h5.sizeOffsets + 8 + ndimStorage)
            val leftAddress: Long = h5.readOffset(state)
            val rightAddress: Long = h5.readOffset(state)

            // could we gulp this in one read ?
            for (i in 0..nentries) {
                val chunkSize = raf.readInt(state)
                val filterMask = raf.readInt(state)
                val inner = IntArray(ndimStorage) { j ->
                    val loffset: Long = raf.readLong(state)
                    require(loffset < Int.MAX_VALUE) // why?
                    loffset.toInt()
                }
                val key = Key(chunkSize, filterMask, inner)
                val childPointer = h5.readOffset(state) // 4 or 8 bytes
                if (level == 0) {
                    myDataChunks.add( DataChunk(key, childPointer))
                } else {
                    keys.add(key)
                    childPointers.add(if (i == nentries) -1 else childPointer)
                }
            }
        }

        // this finds the first entry we dont want to skip.
        // entry i goes from [offset(i),offset(i+1))
        // we want to skip any entries we dont need, namely those where want >= offset(i+1)
        // so keep skipping until want < offset(i+1)
        fun first(wantOrigin: IntArray?) {
            if (level == 0) {
                // note nentries-1 - assume dont skip the last one
                currentEntry = 0
                while (currentEntry < nentries - 1) {
                    val entry: BTreeDataNew.DataChunk = myDataChunks[currentEntry + 1] // look at the next one
                    if (wantOrigin == null || tiling.compare(wantOrigin, entry.key.offsets) < 0) break
                    currentEntry++
                }
            } else {
                currentNode = null
                currentEntry = 0
                while (currentEntry < nentries) {
                    if (wantOrigin == null || tiling.compare(wantOrigin, keys[currentEntry + 1].offsets) < 0) {
                        val childAddress = childPointers[currentEntry]
                        currentNode = Node(childAddress, address)
                        currentNode!!.first(wantOrigin)
                        if (nodes[childAddress] != null) {
                            println("HEY already read node at $rootNodeAddress")
                        }
                        nodes[childAddress] = currentNode!!
                        break
                    }
                    currentEntry++
                }

                // heres the case where its the last entry we want; the tiling.compare() above may fail
                if (currentNode == null) {
                    currentEntry = nentries - 1
                    val childAddress = childPointers[currentEntry]
                    currentNode = Node(childAddress, address)
                    currentNode!!.first(wantOrigin)
                    if (nodes[childAddress] != null) {
                        println("HEY already read node at $rootNodeAddress")
                    }
                    nodes[childAddress] = currentNode!!
                }
            }
            require(nentries == 0 || currentEntry < nentries) { "$currentEntry >= $nentries" }
        }

        // TODO - wouldnt be a bad idea to terminate if possible instead of running through all subsequent entries
        operator fun hasNext(): Boolean {
            return if (level == 0) {
                currentEntry < nentries
            } else {
                if (currentNode!!.hasNext()) true else currentEntry < nentries - 1
            }
        }

        operator fun next(): BTreeDataNew.DataChunk {
            return if (level == 0) {
                myDataChunks[currentEntry++]
            } else {
                if (currentNode!!.hasNext()) return currentNode!!.next() // LOOK recursion instead of loop ?
                currentEntry++
                val childAddress = childPointers[currentEntry]
                currentNode = Node(childAddress, address)
                currentNode!!.first(null)
                if (nodes[childAddress] != null) {
                    println("HEY already read node at $rootNodeAddress")
                }
                nodes[childAddress] = currentNode!!
                currentNode!!.next()
            }
        }
    }

    data class Key(val chunkSize: Int, val filterMask : Int, val offsets: IntArray)

    data class DataChunk(val key : Key, val filePos : Long)
}