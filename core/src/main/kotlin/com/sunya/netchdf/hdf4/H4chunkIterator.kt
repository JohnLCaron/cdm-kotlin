package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.transferMissingNelems
import java.nio.ByteBuffer

class H4chunkIterator(h4 : H4builder, val v2: Variable, val wantSection : SectionL) : AbstractIterator<ArraySection>() {
    private val debugChunking = false

    private val vinfo = v2.spObject as Vinfo
    private val elemSize = vinfo.elemSize
    private val datatype = v2.datatype
    private val wantSpace = IndexSpace(wantSection)

    private val tiledData : H4tiledData
    private val chunkIterator : Iterator<H4CompressedDataChunk>

    init {
        tiledData = H4tiledData(h4, wantSection.varShape, vinfo.chunkLengths, vinfo.chunks!!)
        if (debugChunking) println(" ${tiledData.tiling}")
        chunkIterator = tiledData.findDataChunks(wantSpace).iterator()
    }

    override fun computeNext() {
        if (chunkIterator.hasNext()) {
            setNext(getaPair(chunkIterator.next()))
        } else {
            done()
        }
    }

    private fun getaPair(dataChunk : H4CompressedDataChunk) : ArraySection {
        val dataSpace = IndexSpace(v2.rank, dataChunk.offsets.toLongArray(), vinfo.chunkLengths.toLongArray())
        val useEntireChunk = wantSpace.contains(dataSpace)
        val intersectSpace = if (useEntireChunk) dataSpace else wantSpace.intersect(dataSpace)

        val bb = if (dataChunk.isMissing()) {
            if (debugChunking) println("   missing ${dataChunk.show(tiledData.tiling)}")
            val sizeBytes = intersectSpace.totalElements * elemSize
            val bbmissing = ByteBuffer.allocate(sizeBytes.toInt())
            bbmissing.order(vinfo.endian)
            transferMissingNelems(vinfo.fillValue, datatype, intersectSpace.totalElements.toInt(), bbmissing)
            if (debugChunking) println("   missing transfer ${intersectSpace.totalElements} fillValue=${vinfo.fillValue}")
            bbmissing
        } else {
            if (debugChunking) println("  chunkIterator=${dataChunk.show(tiledData.tiling)}")
            val filteredData = dataChunk.getByteBuffer() // filter already applied
            if (useEntireChunk) {
                filteredData
            } else {
                val chunker = Chunker(dataSpace, wantSpace) // each DataChunkEntry has its own Chunker iteration
                chunker.transferBB(filteredData, elemSize, intersectSpace.totalElements.toInt())
            }
        }

        bb.position(0)
        bb.limit(bb.capacity())
        bb.order(vinfo.endian)

        val shape = wantSpace.shape.toIntArray()
        val array = when (datatype) {
            Datatype.BYTE -> ArrayByte(shape, bb)
            Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
            Datatype.SHORT -> ArrayShort(shape, bb)
            Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb)
            Datatype.INT -> ArrayInt(shape, bb)
            Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb)
            Datatype.FLOAT -> ArrayFloat(shape, bb)
            Datatype.DOUBLE -> ArrayDouble(shape, bb)
            Datatype.LONG -> ArrayLong(shape, bb)
            Datatype.ULONG -> ArrayULong(shape, bb)
            Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }

        return ArraySection(array, intersectSpace.section()) // LOOK use space instead of Section ??
    }

}