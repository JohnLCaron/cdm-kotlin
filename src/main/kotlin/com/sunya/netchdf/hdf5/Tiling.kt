package com.sunya.netchdf.hdf5

import com.google.common.base.Preconditions
import java.util.*

/**
 * Abstraction of HDF5 chunking.
 * A Tiling divides a multidimensional index into tiles.
 *
 * Index are points in the original multidimensional index.
 * Tiles are points in the tiled space.
 * Each tile has the same size, given by tileSize.
 * LOOK maybe move to IOSP ??
 */
class Tiling(shape: IntArray, chunk: IntArray) {
    private val rank: Int
    private val shape : IntArray // overall shape of the dataset's index space - may be larger than actual variable shape
    private val chunk : IntArray // actual storage is in this shape. may be larger than the shape.
    private val stride : IntArray // for computing tile index

    init {
        // convenient to allow tileSize to have (an) extra dimension at the end
        // to accommodate hdf5 storage, which has the element size
        Preconditions.checkArgument(shape.size <= chunk.size)
        rank = shape.size
        this.chunk = chunk
        this.shape = IntArray(rank)
        for (i in 0 until rank) {
            this.shape[i] = Math.max(shape[i], chunk[i])
        }
        val tileSize = IntArray(rank)
        for (i in 0 until rank) {
            tileSize[i] = (this.shape[i] + chunk[i] - 1) / chunk[i]
        }
        stride = IntArray(rank)
        var strider = 1
        for (k in rank - 1 downTo 0) {
            stride[k] = strider
            strider *= tileSize[k]
        }
    }

    fun show(a: IntArray): String {
        val f = Formatter()
        for (`val` in a) f.format("%3d,", `val`)
        return f.toString()
    }

    /**
     * Compute the tile
     *
     * @param pt index point
     * @return corresponding tile
     */
    fun tile(pt: IntArray): IntArray {
        val useRank = Math.min(rank, pt.size) // eg varlen (datatype 9) has mismatch
        val tile = IntArray(useRank)
        for (i in 0 until useRank) {
            // 7/30/2016 jcaron. Apparently in some cases, at the end of the array, the index can be greater than the shape.
            // eg cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc
            // Presumably to have even chunks. Could try to calculate the last even chunk.
            // For now im removing this consistency check.
            // assert shape[i] >= pt[i] : String.format("shape[%s]=(%s) should not be less than pt[%s]=(%s)", i, shape[i], i,
            // pt[i]);
            tile[i] = pt[i] / chunk[i] // seems wrong, rounding down ??
        }
        return tile
    }

    /**
     * Get order based on which tile the pt belongs to
     *
     * @param pt index point
     * @return order number based on which tile the pt belongs to
     */
    fun order(pt: IntArray): Int {
        val tile = tile(pt)
        var order = 0
        val useRank = Math.min(rank, pt.size) // eg varlen (datatype 9) has mismatch
        for (i in 0 until useRank) order += stride[i] * tile[i]
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
}