package com.sunya.netchdf.hdf5

import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.Odometer
import com.sunya.cdm.layout.Tiling

private val check = true
private val debug = false
private val debugRow = false

// replaces H5tiledLayoutBB and H5tiledLayout
class ChunkedData(val btree1 : BTree1New) {
    val tiling = Tiling(btree1.varShape, btree1.storageSize)
    val rootNode : BTree1New.Node

    // keep track of nodes so we only read once
    private val nodeCache = mutableMapOf<Long, BTree1New.Node>()
    var readHit = 0;
    var readMiss = 0;

    init {
        rootNode = readNode(btree1.rootNodeAddress, null)
    }

    // node reading goes through here for caching
    fun readNode(address : Long, parent : BTree1New.Node?) : BTree1New.Node {
        if (nodeCache[address] != null) {
            readHit++
            return nodeCache[address]!!
        }
        readMiss++
        val node = btree1.Node(address, parent)
        nodeCache[address] = node

        if (check) {
            if (debug) println("node = $address, level = ${node.level} nentries = ${node.nentries}")
            for (idx in 0 until node.nentries) {
                val thisEntry = node.dataChunkEntries[idx]
                val key = thisEntry.key.offsets
                if (debug) println(" $idx = ${key.contentToString()} tile = ${tiling.tile(key).contentToString()}")
                if (idx < node.nentries - 1) {
                    val nextEntry = node.dataChunkEntries[idx + 1]
                    require(tiling.compare(key, nextEntry.key.offsets) < 0)
                }
            }
        }
        return node
    }

    // optimize later
    fun findEntryContainingKey(parent : BTree1New.Node, key : IntArray) : BTree1New.DataChunkEntry? {
        var foundEntry : BTree1New.DataChunkEntry? = null
        for (idx in 0 until parent.nentries) {
            foundEntry = parent.dataChunkEntries[idx]
            if (idx < parent.nentries - 1) {
                val nextEntry = parent.dataChunkEntries[idx + 1] // look at the next one
                if (tiling.compare(key, nextEntry.key.offsets) < 0) {
                    break
                }
            }
        }
        if (foundEntry == null) {
            throw RuntimeException("wtf")
        }
        if (parent.level == 0) {
            return if (tiling.compare(key, foundEntry.key.offsets) == 0) foundEntry else null
        }
        val node= readNode(foundEntry.childAddress, parent)
        return findEntryContainingKey(node, key)
    }

    /*
    fun findEntryContainingKey(lastEntry : BTree1New.DataChunkEntry, key : IntArray) : BTree1New.DataChunkEntry? {
        if (tiling.compare(key, lastEntry.key.offsets) == 0) {
            return lastEntry
        }
        if (tiling.compare(key, lastEntry.key.offsets) < 0) {
            return null // missing
        }
        var useEntry : BTree1New.DataChunkEntry = lastEntry
        while (tiling.compare(key, useEntry.key.offsets) > 0) {
            useEntry = nextEntry(useEntry)?: return null
       }

        val parent = lastEntry?.parent ?: rootNode
        var foundEntry : BTree1New.DataChunkEntry? = null
        for (idx in 0 until parent.nentries) {
            foundEntry = parent.dataChunkEntries[idx]
            if (idx < parent.nentries - 1) {
                val nextEntry = parent.dataChunkEntries[idx + 1] // look at the next one
                if (tiling.compare(key, nextEntry.key.offsets) < 0) {
                    break
                }
            }
        }
        if (foundEntry == null) {
            throw RuntimeException("wtf")
        }
        if (parent.level == 0) {
            if (tiling.compare(key, foundEntry.key.offsets) != 0) {
                println(" *** tile missing")
                // (val level : Int, val parent : Node, val idx : Int, val key : DataChunkKey, val childAddress : Long)
                // (val chunkSize: Int, val filterMask : Int, val offsets: IntArray)
                // a "missing data chunk"
                return BTree1New.DataChunkEntry(0, parent, -1, BTree1New.DataChunkKey(-1, 0, key), -1L)
                // throw RuntimeException("tile missing") // missing tile, TODO return fillvalue
            }
            return foundEntry
        }
        val node= readNode(foundEntry.childAddress, parent)
        return findEntryContainingKey(node, key)
    } */

    // find the next entry in the btree
    fun nextEntry(entry : BTree1New.DataChunkEntry) : BTree1New.DataChunkEntry? {
        if (entry.isMissing()) {
            return null
        }
        val node = entry.parent
        if (entry.idx < node.nentries - 1) {
            return node.dataChunkEntries[entry.idx + 1]
        }
        if (node.rightAddress == -1L) {
            return null
        }
        val nextSibling =  readNode(node.rightAddress, node.parent)
        return nextSibling.dataChunkEntries[0]
    }

    fun findDataChunks(wantSpace : IndexSpace) : Iterable<BTree1New.DataChunkEntry> {
        val chunks = mutableListOf<BTree1New.DataChunkEntry>()

        val tileSection = tiling.section( wantSpace) // section in tiles that we want
        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        // println("tileSection = ${tileSection}")

        // var lastEntry : BTree1New.DataChunkEntry? = null
        while (!tileOdometer.isDone()) {
            val wantTile = tileOdometer.current
            val wantKey = tiling.index(wantTile) // convert to index "keys"
            val haveEntry = findEntryContainingKey(rootNode, wantKey)
            val useEntry = haveEntry?: BTree1New.DataChunkEntry(0, rootNode, -1, BTree1New.DataChunkKey(-1, 0, wantKey), -1L)
            chunks.add(useEntry)
            tileOdometer.incr()
            // if (haveEntry != null) lastEntry = haveEntry
        }
        // println("nchunks = ${chunks.size}")
        return chunks
    }

    /* add all the chunks along this row until section no longer contains them
    fun addDataChunkRow(wantSpace : IndexSpace, starting : BTree1New.DataChunkEntry,
                        chunks : MutableList<BTree1New.DataChunkEntry>,
                        chunkMap : MutableMap<BTree1New.DataChunkKey, BTree1New.DataChunkEntry>,
                        ) : Int {
        var count = 0
        var useChunk = starting
        while (true) {
            val dup = chunkMap[useChunk.key]
            if (dup != null) {
                println("duplicate")
            }
            chunks.add(useChunk)
            chunkMap[useChunk.key] = useChunk

            count++
            val nextChunk = nextEntry(useChunk)
            if (nextChunk == null) {
                break
            } else {
                val nextIndexShape = IndexSpace(nextChunk.key.offsets, tiling.chunk)
                val needed = wantSpace.intersects(nextIndexShape)
                if (!needed) {
                    break
                } else if (debugRow) {
                    print(", ${tiling.tile(nextChunk.key.offsets).contentToString()}")
                }
            }
            useChunk = nextChunk
        }
        return count
    } */

    override fun toString(): String {
        return "ChunkedData(chunk=${btree1.storageSize.contentToString()}, readHit=$readHit, readMiss=$readMiss)"
    }


}