package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.Layout
import com.sunya.cdm.iosp.LayoutTiled

internal class H5tiledLayout(
    h5: H5builder,
    v2: Variable,
    want: Section,
    dtype: Datatype,
) : Layout {

    private val delegate: LayoutTiled
    private val wantSection: Section // must be filled
    private val chunkSize : IntArray // from the StorageLayout message (exclude the elemSize)
    override val elemSize : Int  // last dimension of the StorageLayout message

    /**
     * Constructor.
     * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
     *
     * @param vinfo the vinfo object for this variable
     * @param dtype type of data. may be different from v2.
     * @param wantSection the wanted section of data, contains a List of Range objects, must be complete
     * @throws IOException on io error
     */
    init {
        val vinfo = v2.spObject as DataContainerVariable
        requireNotNull(vinfo)
        require(vinfo.isChunked)

        // we have to translate the want section into the same rank as the storageSize, in order to be able to call
        // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, reletive
        // to the Variable.
        if (dtype === Datatype.CHAR && want.rank() < vinfo.storageDims.size) {
            wantSection = Section.builder().appendRanges(want.ranges).appendRange(1).build()
        } else {
            wantSection = want
        }

        // one less chunk dimension, except in the case of char
        val nChunkDims: Int = if (dtype === Datatype.CHAR) vinfo.storageDims.size else vinfo.storageDims.size - 1
        chunkSize = IntArray(nChunkDims)
        System.arraycopy(vinfo.storageDims, 0, chunkSize, 0, nChunkDims)
        elemSize = vinfo.elementSize
        if (debug) println(" H5tiledLayout: $this")

        // create the data chunk iterator LOOK maybe vinfo.btree, to cache it ?
        val btree = DataBTree(h5, vinfo.dataPos, v2.shape, vinfo.storageDims, null)
        val iter: LayoutTiled.DataChunkIterator = btree.getDataChunkIteratorNoFilter(want, nChunkDims)
        delegate = LayoutTiled(iter, chunkSize, elemSize, wantSection)
    }

    override val totalNelems: Long
        get() = delegate.totalNelems

    override operator fun hasNext(): Boolean {
        return delegate.hasNext()
    }

    override operator fun next(): Layout.Chunk {
        return delegate.next()
    }

    override fun toString(): String {
        val sbuff = StringBuilder()
        sbuff.append("want=").append(wantSection).append("; ")
        sbuff.append("chunkSize=[")
        for (i in chunkSize.indices) {
            if (i > 0) sbuff.append(",")
            sbuff.append(chunkSize[i])
        }
        sbuff.append("] totalNelems=").append(totalNelems)
        sbuff.append(" elemSize=").append(elemSize)
        return sbuff.toString()
    }

    companion object {
        private const val debug = false
    }
}