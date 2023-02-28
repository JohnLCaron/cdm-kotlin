package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Section
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

    fun findDataChunkContainingKey(parent : BTree1New.Node, key : IntArray) : BTree1New.DataChunkEntry? {
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
                throw RuntimeException("wtf")
            }
            // is this true ?
            require(tiling.compare(key, foundEntry.key.offsets) == 0)
            return foundEntry
        }
        val node= readNode(foundEntry.childAddress, parent)
        return findDataChunkContainingKey(node, key)
    }

    // find the next chunk in the btree
    fun nextDataChunk(chunk : BTree1New.DataChunkEntry) : BTree1New.DataChunkEntry? {
        val node = chunk.parent
        if (chunk.idx < node.nentries - 1) {
            return node.dataChunkEntries[chunk.idx + 1]
        }
        if (node.rightAddress == -1L) {
            return null
        }
        val nextSibling =  readNode(node.rightAddress, node.parent)
        return nextSibling.dataChunkEntries[0]
    }

    fun findDataChunks(section : Section) : Iterable<BTree1New.DataChunkEntry> {
        val chunks = mutableListOf<BTree1New.DataChunkEntry>()

        val tileSection = tiling.section( IndexSpace(section)) // section in tiles that we want
        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        val incrDigit = tiling.rank - 2 // increment the second fastest digit
        while (!tileOdometer.isDone()) {
            val firstTile = tileOdometer.current
            val firstKey = tiling.index(firstTile) // convert to index "keys"
            val firstChunk = findDataChunkContainingKey(rootNode, firstKey)!!
            if (check) require(section.contains(firstChunk.key.offsets))
            if (debugRow) print("tilesInRow = ${firstTile.contentToString()}")
            addDataChunkRow(section, firstChunk, chunks) // can efficiently find the chunks along the first dimension
            if (debugRow) println()

            if (incrDigit >= 0) {
                tileOdometer.incr(incrDigit)
            } else {
                break
            }
        }

        return chunks
    }

    // add all the chunks along this row until section no longer contains them
    fun addDataChunkRow(section : Section, starting : BTree1New.DataChunkEntry, chunks : MutableList<BTree1New.DataChunkEntry>) {
        var useChunk = starting
        while (true) {
            chunks.add(useChunk)
            val nextChunk = nextDataChunk(useChunk)
            if (nextChunk == null) {
                break
            } else {
                val needed = section.contains(nextChunk.key.offsets)
                if (!needed) {
                    break
                } else if (debugRow) {
                    print(", ${tiling.tile(nextChunk.key.offsets).contentToString()}")
                }

            }
            useChunk = nextChunk
        }
    }

}