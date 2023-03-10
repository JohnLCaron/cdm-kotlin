package com.sunya.netchdf.hdf5

import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.Odometer
import com.sunya.cdm.layout.Tiling

private val check = true
private val debug = false
private val debugRow = false

// replaces H5tiledLayoutBB and H5tiledLayout
/** wraps BTree1New to handle iterating through tiled data (aka chunked data) */
class TiledData(val btree1 : BTree1New) {
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

    override fun toString(): String {
        return "TiledData(chunk=${btree1.storageSize.contentToString()}, readHit=$readHit, readMiss=$readMiss)"
    }

}