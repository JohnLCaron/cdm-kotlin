package com.sunya.cdm.layout

/**
 * A Tiling divides a multidimensional index space into tiles.
 * Indices are points in the original multidimensional index space.
 * Tiles are points in the tiled space.
 * Each tile has the same index size, given by chunk.
 *
 *  Allows to efficiently find the data chunks that cover an arbitrary section of the variable's index space.
 *
 * @param varshape the variable's shape
 * @param chunk  actual data storage has this shape. May be larger than the shape.
 */
class Tiling(varshape: IntArray, val chunk: IntArray) {
    val rank: Int
    val tileShape : IntArray // overall shape of the dataset's tile space
    private val indexShape : IntArray // overall shape of the dataset's index space - may be larger than actual variable shape
    private val strider : IntArray // for computing tile index

    init {
        // convenient to allow tileSize to have (an) extra dimension at the end
        // to accommodate hdf5 storage, which has the element size
        require(varshape.size <= chunk.size)
        rank = varshape.size
        this.indexShape = IntArray(rank)
        for (i in 0 until rank) {
            this.indexShape[i] = Math.max(varshape[i], chunk[i])
        }
        this.tileShape = IntArray(rank)
        for (i in 0 until rank) {
            tileShape[i] = (this.indexShape[i] + chunk[i] - 1) / chunk[i]
        }
        strider = IntArray(rank)
        var accumStride = 1
        for (k in rank - 1 downTo 0) {
            strider[k] = accumStride
            accumStride *= tileShape[k]
        }
    }

    /** Compute the tile from an index, ie which tile does this point belong to? */
    fun tile(index: IntArray): IntArray {
        val useRank = Math.min(rank, index.size) // eg varlen (datatype 9) has mismatch
        val tile = IntArray(useRank)
        for (i in 0 until useRank) {
            // 7/30/2016 jcaron. Apparently in some cases, at the end of the array, the index can be greater than the shape.
            // eg cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc
            // Presumably to have even chunks. Could try to calculate the last even chunk.
            // For now im removing this consistency check.
            // assert shape[i] >= pt[i] : String.format("shape[%s]=(%s) should not be less than pt[%s]=(%s)", i, shape[i], i,
            // pt[i]);
            tile[i] = index[i] / chunk[i] // LOOK seems wrong, rounding down ??
        }
        return tile
    }

    /** Compute the minimum index of a tile, inverse of tile().
     * This will match a key in the datachunk btree, up to rank dimensions */
    fun index(tile: IntArray): IntArray {
        return IntArray(rank) { idx-> tile[idx] * chunk[idx] }
    }

    /**
     * Get order based on which tile the pt belongs to
     * LOOK you could do this without using the tile
     *
     * @param pt index point
     * @return order number based on which tile the pt belongs to
     */
    fun order(pt: IntArray): Int {
        val tile = tile(pt)
        var order = 0
        val useRank = Math.min(rank, pt.size) // eg varlen (datatype 9) has mismatch
        for (i in 0 until useRank) order += strider[i] * tile[i]
        return order
    }

    /**
     * Create an ordering of index points based on which tile the point is in.
     *
     * @param p1 index point 1
     * @param p2 index point 2
     * @return order(p1) - order(p2) : negative if p1 < p2, positive if p1 > p2 , 0 if equal
     */
    fun compare(p1: IntArray, p2: IntArray): Int {
        return order(p1) - order(p2)
    }

    /** create an IndexChunk in tile space from a IndexChunk in index space */
    fun section(indexSection : IndexSpace) : IndexSpace {
        require(indexSection.rank == rank)
        indexSection.limit.forEachIndexed { idx, it ->
            require(it < indexShape[idx])
        }

        val start = tile(indexSection.start)
        val limit = tile(indexSection.limit)
        val length = IntArray(rank) { idx -> limit[idx] - start[idx] + 1 }
        return IndexSpace(start, length)
    }
}