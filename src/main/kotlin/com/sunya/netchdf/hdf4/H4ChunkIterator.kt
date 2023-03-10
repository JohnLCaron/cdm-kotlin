package com.sunya.netchdf.hdf4

import com.sunya.cdm.iosp.LayoutTiled
import com.sunya.cdm.iosp.LayoutTiledBB
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.util.IOcopyB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream

internal class H4ChunkIterator(h4file : Hdf4File, vinfo: Vinfo) : LayoutTiled.DataChunkIterator {
    val chunks: List<SpecialDataChunk>
    var chunkNo = 0

    init {
        chunks = vinfo.readChunks(h4file)
    }

    override operator fun hasNext(): Boolean {
        return chunkNo < chunks.size
    }

    override operator fun next(): LayoutTiled.DataChunk {
        val chunk: SpecialDataChunk = chunks[chunkNo]
        val chunkData: TagData = chunk.data
        chunkNo++
        return LayoutTiled.DataChunk(chunk.origin, chunkData.offset)
    }
}

internal class H4CompressedChunkIterator(val h4 : H4builder, vinfo: Vinfo) : LayoutTiledBB.DataChunkIterator {
    val chunks: List<SpecialDataChunk>
    var chunkNo = 0

    init {
        chunks = vinfo.chunks!!
    }

    override operator fun hasNext(): Boolean {
        return chunkNo < chunks.size
    }

    override operator fun next(): LayoutTiledBB.DataChunk {
        val chunk = chunks[chunkNo]
        val chunkData: TagData = chunk.data
        require(chunkData.extendedTag == TagEnum.SPECIAL_COMP)
        requireNotNull(chunkData.compress)
        chunkNo++
        return H4DataChunkCompressed(h4, chunk.origin, chunkData.compress!!)
    }
}

private const val defaultBufferSize = 50_000

private class H4DataChunkCompressed(
    val h4 : H4builder,
    override val offset: IntArray,  // offset index of this chunk, reletive to entire array
    private val compress: SpecialComp
) : LayoutTiledBB.DataChunk {
    
    private var bb: ByteBuffer? = null // the data is placed into here

    @Throws(IOException::class)
    override fun getByteBuffer(expectedSizeBytes: Int): ByteBuffer {
        if (bb == null) {
            // read compressed data in
            val cdata = compress!!.getDataTag(h4)
            val input: InputStream

            // compressed data stored in one place
            input = if (cdata.linked == null) {
                val state = OpenFileState(cdata.offset, ByteOrder.BIG_ENDIAN)
                val cbuffer = h4.raf.readBytes(state, cdata.length)
                ByteArrayInputStream(cbuffer)
            } else { // or compressed data stored in linked storage
                makeSpecialLinkedInputStream(h4, cdata.linked!!)
            }

            // uncompress it
            bb = if (compress.compress_type == TagEnum.COMP_CODE_DEFLATE) {
                // read the stream in and uncompress
                val zin: InputStream = InflaterInputStream(input)
                val out = ByteArrayOutputStream(compress.uncomp_length)
                IOcopyB(zin, out, defaultBufferSize)
                val buffer = out.toByteArray()
                ByteBuffer.wrap(buffer)
            } else if (compress.compress_type == TagEnum.COMP_CODE_NONE) {
                // just read the stream in
                val out = ByteArrayOutputStream(compress.uncomp_length)
                IOcopyB(input, out, defaultBufferSize)
                val buffer = out.toByteArray()
                ByteBuffer.wrap(buffer)
            } else {
                throw IllegalStateException("unknown compression type =" + compress.compress_type)
            }
        }
        return bb!!
    }
}