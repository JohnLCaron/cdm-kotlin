package com.sunya.netchdf.hdf4

import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.Odometer
import com.sunya.cdm.layout.Tiling
import com.sunya.cdm.util.IOcopyB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream

private const val defaultBufferSize = 50_000

// replace H4ChunkIterator, LayoutBB
internal class H4tiledData(val h4 : H4builder, varShape : IntArray, chunk : IntArray, val chunks: List<SpecialDataChunk>) {
    val tiling = Tiling(varShape, chunk)

    // optimize later
    fun findEntryContainingKey(want : IntArray) : SpecialDataChunk? {
        chunks.forEach { chunk ->
            if (chunk.origin.contentEquals(want)) return chunk
        }
        return null
    }

    fun findDataChunks(wantSpace : IndexSpace) : Iterable<H4CompressedDataChunk> {
        val chunks = mutableListOf<H4CompressedDataChunk>()

        val tileSection = tiling.section( wantSpace) // section in tiles that we want
        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        // println("tileSection = ${tileSection}")

        while (!tileOdometer.isDone()) {
            val wantTile = tileOdometer.current
            val wantKey = tiling.index(wantTile) // convert to chunk origin
            val chunk = findEntryContainingKey(wantKey)
            val useEntry = if (chunk != null) H4CompressedDataChunk(h4, chunk.origin, chunk.data.compress)
                else H4CompressedDataChunk(h4, wantKey, null)
            chunks.add(useEntry)
            tileOdometer.incr()
        }
        return chunks
    }
}

internal class H4CompressedDataChunk(
    val h4 : H4builder,
    val offsets: IntArray,  // offset index of this chunk, reletive to entire array
    private val compress: SpecialComp?
) {
    fun isMissing() = (compress == null)

    private var bb: ByteBuffer? = null // the data is placed into here

    @Throws(IOException::class)
    fun getByteBuffer(): ByteBuffer {
        if (bb != null) return bb!!
        if (compress == null) {
        } else {
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
            var outSize = 0
            bb = if (compress.compress_type == TagEnum.COMP_CODE_DEFLATE) {
                // read the stream in and uncompress
                val zin: InputStream = InflaterInputStream(input)
                val out = ByteArrayOutputStream(compress.uncomp_length)
                IOcopyB(zin, out, defaultBufferSize)
                val buffer = out.toByteArray()
                outSize = buffer.size
                ByteBuffer.wrap(buffer)
            } else if (compress.compress_type == TagEnum.COMP_CODE_NONE) {
                // just read the stream in
                val out = ByteArrayOutputStream(compress.uncomp_length)
                IOcopyB(input, out, defaultBufferSize)
                val buffer = out.toByteArray()
                outSize = buffer.size
                ByteBuffer.wrap(buffer)
            } else {
                throw IllegalStateException("unknown compression type =" + compress.compress_type)
            }
            println("uncompress offset ${cdata.offset} length ${cdata.length} uncomp_length=${compress.uncomp_length} outSize=${outSize}")
        }
        bb!!.position(0)
        return bb!!
    }

    fun show(tiling : Tiling) =
        "chunkStart=${offsets.contentToString()}, tile= ${tiling.tile(offsets).contentToString()}"

}