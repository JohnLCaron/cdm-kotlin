package com.sunya.netchdf.hdf5

import com.google.common.base.Preconditions
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.LayoutBB
import com.sunya.cdm.iosp.LayoutBBTiled
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.util.IOcopyB
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

private val logger = KotlinLogging.logger("H5tiledLayoutBB")

/**
 * Iterator to read/write subsets of an array.
 * This calculates byte offsets for HD5 chunked datasets.
 * Assumes that the data is stored in chunks, indexed by a Btree.
 * Used for filtered data
 */
class H5tiledLayoutBB(
    h5: H5builder,
    v2: Variable,
    want: Section,
    val filters: List<Filter>,
    byteOrder: ByteOrder
) : LayoutBB {
    private val delegate: LayoutBBTiled
    private val raf: OpenFile
    private val byteOrder: ByteOrder
    private var wantSection: Section
    private val chunkSize : IntArray // from the StorageLayout message (exclude the elemSize)
    override val elemSize : Int // last dimension of the StorageLayout message
    private val nChunkDims: Int
    private var inflatebuffersize = DEFAULTZIPBUFFERSIZE

    /**
     * Constructor.
     * This is for HDF5 chunked data storage. The data is read by chunk, for efficency.
     *
     * @param v2 Variable to index over; assumes that vinfo is the data object
     * @param wantSection the wanted section of data, contains a List of Range objects. must be complete
     * @param raf the RandomAccessFile
     * @param filters set of filters that have been applied to the data
     * @throws InvalidRangeException if section invalid for this variable
     * @throws IOException on io error
     */
    init {
        val vinfo = v2.spObject as VariableData
        requireNotNull(vinfo)
        require(vinfo.isChunked)

        this.raf = h5.raf
        this.byteOrder = byteOrder

        // we have to translate the want section into the same rank as the storageSize, in order to be able to call
        // Section.intersect(). It appears that storageSize (actually msl.chunkSize) may have an extra dimension, reletive
        // to the Variable.
        val dtype = v2.datatype
        if (dtype == Datatype.CHAR && want.rank() < vinfo.storageSize.size) {
            wantSection = Section.builder().appendRanges(want.ranges).appendRange(1).build()
        } else {
            wantSection = Section.fill(want, v2.shape)
        }

        // one less chunk dimension, except in the case of char
        nChunkDims = if (dtype === Datatype.CHAR) vinfo.storageSize.size else vinfo.storageSize.size - 1
        chunkSize = IntArray(nChunkDims)
        System.arraycopy(vinfo.storageSize, 0, chunkSize, 0, nChunkDims)
        elemSize = vinfo.storageSize.get(vinfo.storageSize.size - 1) // last one is always the elements size

        // create the data chunk iterator
        val btree = DataBTree(h5, vinfo.dataPos, v2.shape, vinfo.storageSize, null)
        val iter: DataBTree.DataChunkIterator = btree.getDataChunkIteratorFilter(want)
        val dcIter: DataChunkIterator = DataChunkIterator(iter)
        delegate = LayoutBBTiled(dcIter, chunkSize, elemSize, want)
        if (System.getProperty(INFLATEBUFFERSIZE_PROPERTY) != null) {
            try {
                val size = System.getProperty(INFLATEBUFFERSIZE_PROPERTY).toInt()
                if (size <= 0) logger.warn(String.format("-D%s must be > 0", INFLATEBUFFERSIZE_PROPERTY)
                ) else inflatebuffersize = size
            } catch (nfe: NumberFormatException) {
                logger.warn(String.format("-D%s is not an integer", INFLATEBUFFERSIZE_PROPERTY))
            }
        }
        if (debugFilter) System.out.printf(
            "inflate buffer size -D%s = %d%n", INFLATEBUFFERSIZE_PROPERTY,
            inflatebuffersize
        )
        if (debug) println(" H5tiledLayout: $this")
    }

    override val totalNelems: Long
        get() = delegate.totalNelems

    override operator fun hasNext(): Boolean {
        return delegate.hasNext()
    }

    override operator fun next(): LayoutBB.Chunk {
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

    private inner class DataChunkIterator(val delegate: DataBTree.DataChunkIterator) :
        LayoutBBTiled.DataChunkIterator {
        override operator fun hasNext(): Boolean {
            return delegate.hasNext()
        }

        @Throws(IOException::class)
        override operator fun next(): LayoutBBTiled.DataChunk {
            return DataChunk(delegate.next())
        }
    }

    private inner class DataChunk(val delegate: DataBTree.DataChunk) : LayoutBBTiled.DataChunk {
        init {

            // Check that the chunk length (delegate.size) isn't greater than the maximum array length that we can
            // allocate (MAX_ARRAY_LEN). This condition manifests in two ways.
            // 1) According to the HDF docs (https://www.hdfgroup.org/HDF5/doc/Advanced/Chunking/, "Chunk Maximum Limits"),
            // max chunk length is 4GB (i.e. representable in an unsigned int). Java, however, only has signed ints.
            // So, if we try to store a large unsigned int in a singed int, it'll overflow, and the signed int will come
            // out negative. We're trusting here that the chunk size read from the HDF file is never negative.
            // 2) In most JVM implementations MAX_ARRAY_LEN is actually less than Integer.MAX_VALUE (see note in ArrayList).
            // So, we could have: "MAX_ARRAY_LEN < chunkSize <= Integer.MAX_VALUE".
            if (delegate.size < 0 || delegate.size > Companion.MAX_ARRAY_LEN) {
                // We want to report the size of the chunk, but we may be in an arithmetic overflow situation. So to get the
                // correct value, we're going to reinterpet the integer's bytes as long bytes.
                val intBytes = Ints.toByteArray(delegate.size)
                val longBytes = ByteArray(8)
                System.arraycopy(intBytes, 0, longBytes, 4, 4) // Copy int bytes to the lowest 4 positions.
                val chunkSize = Longs.fromByteArray(longBytes) // Method requires an array of length 8.
                throw IllegalArgumentException(
                    String.format(
                        "Filtered data chunk is %s bytes and we must load it all "
                                + "into memory. However the maximum length of a byte array in Java is %s.",
                        chunkSize,
                        Companion.MAX_ARRAY_LEN
                    )
                )
            }
        }

        override val offset: IntArray
            get() {
                var offset = delegate.offset
                if (offset.size > nChunkDims) { // may have to eliminate last offset
                    offset = IntArray(nChunkDims)
                    System.arraycopy(delegate.offset, 0, offset, 0, nChunkDims)
                }
                return offset
            }

        @Throws(IOException::class)
        override fun getByteBuffer(expectedLengthBytes: Int): ByteBuffer {
            try {
                // read the data
                val state = OpenFileState(delegate.filePos)
                var data = raf.readByteBuffer(state, delegate.size).array()

                // apply filters backwards
                for (i in filters.indices.reversed()) {
                    val filter = filters[i]
                    if (isBitSet(delegate.filterMask, i)) {
                        continue
                    }
                    data = when (filter.filterType) {
                        FilterType.deflate -> inflate(data)
                        FilterType.shuffle -> shuffle(data, filter.clientValues[0])
                        FilterType.fletcher32 -> checkfletcher32(data)
                        /* FilterType.zstandard -> {
                            val result = zstandard(data, expectedLengthBytes)
                            result.order(byteOrder)
                            return result // LOOK end of filters ??
                        } */
                        else -> throw RuntimeException("Unknown filter type=" + filter.filterType)
                    }
                }
                val result = ByteBuffer.wrap(data)
                result.order(byteOrder)
                return result
            } catch (e: OutOfMemoryError) {
                val oom: Error = OutOfMemoryError(
                    ("Ran out of memory trying to read HDF5 filtered chunk. Either increase the "
                            + "JVM's heap size (use the -Xmx switch) or reduce the size of the dataset's chunks (use nccopy -c).")
                )
                oom.initCause(e) // OutOfMemoryError lacks a constructor with a cause parameter.
                throw oom
            }
        }

        /*
         * decompress using Zstandard
         *
         * @param compressed compressed data
         * @return uncompressed data
         *
        private fun zstandard(compressed: ByteArray, expectedLengthBytes: Int): ByteBuffer {
            val input = ByteBuffer.wrap(compressed)
            val output = ByteBuffer.wrap(ByteArray(expectedLengthBytes))
            val decompressor = ZstdDecompressor()
            decompressor.decompress(input, output)
            output.flip()
            if (debug || debugFilter) {
                val compress = compressed.size.toFloat() / output.limit()
                System.out.printf(
                    " zstandard bytes in= %d out= %d compress = %f.2%n", compressed.size, output.limit(),
                    compress
                )
            }
            return output.slice()
        }
        
         */

        @Throws(IOException::class)
        private fun inflate(compressed: ByteArray): ByteArray {
            // run it through the Inflator
            val input = ByteArrayInputStream(compressed)
            val inflater = Inflater()
            val inflatestream = InflaterInputStream(input, inflater, inflatebuffersize)
            val len = Math.min(8 * compressed.size, Companion.MAX_ARRAY_LEN)
            val out = ByteArrayOutputStream(len) // Fixes KXL-349288
            IOcopyB(inflatestream, out, len)
            val uncomp = out.toByteArray()
            if (debug || debugFilter) println(" inflate bytes in= " + compressed.size + " bytes out= " + uncomp.size)
            return uncomp
        }

        // just strip off the 4-byte fletcher32 checksum at the end
        private fun checkfletcher32(org: ByteArray): ByteArray {
            val result = ByteArray(org.size - 4)
            System.arraycopy(org, 0, result, 0, result.size)
            if (debug) println(" checkfletcher32 bytes in= " + org.size + " bytes out= " + result.size)
            return result
        }

        private fun shuffle(data: ByteArray, n: Int): ByteArray {
            if (debug) println(" shuffle bytes in= " + data.size + " n= " + n)
            Preconditions.checkArgument(data.size % n == 0)
            if (n <= 1) return data
            val m = data.size / n
            val count = IntArray(n)
            for (k in 0 until n) count[k] = k * m
            val result = ByteArray(data.size)
            for (i in 0 until m) {
                for (j in 0 until n) {
                    result[i * n + j] = data[i + count[j]]
                }
            }
            return result
        }

        fun isBitSet(`val`: Int, bitno: Int): Boolean {
            return ((`val` ushr bitno) and 1) != 0
        }
    }

    companion object {
        var debugFilter = false
        private val DEFAULTZIPBUFFERSIZE = 512
        private val MAX_ARRAY_LEN = Int.MAX_VALUE - 8

        // System property name for -D flag
        private val INFLATEBUFFERSIZE_PROPERTY = "unidata.h5iosp.inflate.buffersize"
        private val debug = false
    }
}