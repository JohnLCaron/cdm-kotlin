package com.sunya.netchdf.hdf5

import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.Odometer
import com.sunya.cdm.layout.Tiling

private const val check = true
private const val debug = false

/** wraps BTree1New to handle iterating through tiled data (aka chunked data) */
internal class TiledH5Data(val btree1 : BTree1) {
    val tiling = Tiling(btree1.varShape, btree1.storageSize)
    val rootNode : BTree1.Node

    // keep track of nodes so we only read once
    private val nodeCache = mutableMapOf<Long, BTree1.Node>()
    var readHit = 0
    var readMiss = 0

    init {
        rootNode = readNode(btree1.rootNodeAddress, null)
    }

    // node reading goes through here for caching
    private fun readNode(address : Long, parent : BTree1.Node?) : BTree1.Node {
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
    private fun findEntryContainingKey(parent : BTree1.Node, key : IntArray) : BTree1.DataChunkEntry? {
        var foundEntry : BTree1.DataChunkEntry? = null
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

    fun findDataChunks(wantSpace : IndexSpace) : Iterable<BTree1.DataChunkEntry> {
        val chunks = mutableListOf<BTree1.DataChunkEntry>()

        val tileSection = tiling.section( wantSpace) // section in tiles that we want
        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        // println("tileSection = ${tileSection}")

        // var lastEntry : BTree1New.DataChunkEntry? = null
        while (!tileOdometer.isDone()) {
            val wantTile = tileOdometer.current
            val wantKey = tiling.index(wantTile) // convert to index "keys"
            val haveEntry = findEntryContainingKey(rootNode, wantKey)
            val useEntry = haveEntry?: BTree1.DataChunkEntry(0, rootNode, -1, BTree1.DataChunkKey(-1, 0, wantKey), -1L)
            chunks.add(useEntry)
            tileOdometer.incr()
            // if (haveEntry != null) lastEntry = haveEntry
        }
        // println("nchunks = ${chunks.size}")
        return chunks
    }

    override fun toString(): String {
        return "TiledData(chunk=${btree1.storageSize.contentToString()}, readHit=$readHit, readMiss=$readMiss)"
    }

}