package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream

class Hdf4File(val filename : String) : Iosp, Netcdf {
    private val raf: OpenFile = OpenFile(filename)
    private val header: H4builder
    private val rootGroup: Group
    var valueCharset: Charset = StandardCharsets.UTF_8

    init {
        header = H4builder(raf, valueCharset)
        rootGroup = header.rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl(strict: Boolean) = com.sunya.cdm.api.cdl(this, strict)
    override fun type() = "hdf4   "

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filledSection = Section.fill(section, v2.shape)
        if (v2.datatype == Datatype.COMPOUND) {
            return readStructureDataArray(v2, filledSection)
        }
        return readRegularDataArray(v2, filledSection)
    }

    private fun readRegularDataArray(v: Variable, section: Section): ArrayTyped<*> {
        val vinfo = v.spObject as Vinfo
        vinfo.setLayoutInfo(header, this) // make sure needed info is present

        if (vinfo.hasNoData) {
            // LOOK not handling case where hasNoData, and has no fillvalue
            return ArraySingle(section.shape, v.datatype, vinfo.fillValue)
        }

        if (!vinfo.isCompressed) {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(vinfo.start, vinfo.elemSize, v.shape, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                val layout = LayoutSegmented(vinfo.segPos, vinfo.segSize, vinfo.elemSize, v.shape, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                val chunkIterator = H4ChunkIterator(header, vinfo)
                val layout = LayoutTiled(chunkIterator, vinfo.chunkSize, vinfo.elemSize, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)
            }
        } else {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(0, vinfo.elemSize, v.shape, section)
                val input: InputStream = getCompressedInputStream(vinfo)
                val dataSource = PositioningDataInputStream(input)
                return readDataFromPositioningStream(dataSource, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                val layout = LayoutRegular(0, vinfo.elemSize, v.shape, section)
                val input: InputStream = getLinkedCompressedInputStream(vinfo)
                val dataSource = PositioningDataInputStream(input)
                return readDataFromPositioningStream(dataSource, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                val chunkIterator = H4CompressedChunkIterator(header, vinfo)
                val layout = LayoutTiledBB(chunkIterator, vinfo.chunkSize, vinfo.elemSize, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)
            }
        }
        throw IllegalStateException()
    }

    @Throws(IOException::class)
    private fun getCompressedInputStream(vinfo: Vinfo): InputStream {
        // probably could construct an input stream from a channel from a raf for now, just read it all in.
        val buffer = raf.readBytes(OpenFileState(vinfo.start, ByteOrder.BIG_ENDIAN), vinfo.length)
        val input = ByteArrayInputStream(buffer)
        return InflaterInputStream(input)
    }

    private fun getLinkedCompressedInputStream(vinfo: Vinfo): InputStream {
        return InflaterInputStream(LinkedInputStream(header, vinfo))
    }

    private fun readDataWithFill(raf: OpenFile, layout: Layout, v2: Variable, fillValue: Any?, wantSection: Section)
            : ArrayTyped<*> {
        require(wantSection.size() == layout.totalNelems)
        val vinfo = v2.spObject as Vinfo
        val totalNbytes = (vinfo.elemSize * layout.totalNelems)
        require(totalNbytes < Int.MAX_VALUE)
        val values = ByteBuffer.allocate(totalNbytes.toInt())

        var bytesRead = 0
        val filePos = OpenFileState(vinfo.start, ByteOrder.BIG_ENDIAN)
        while (layout.hasNext()) {
            val chunk = layout.next()
            filePos.pos = chunk.srcPos()
            val dstPos = (vinfo.elemSize * chunk.destElem()).toInt()
            val chunkBytes = vinfo.elemSize * chunk.nelems()
            bytesRead += raf.readIntoByteBufferDirect(filePos, values, dstPos, chunkBytes)
        }
        require(bytesRead == totalNbytes.toInt())
        values.position(0)

        return when (v2.datatype) {
            Datatype.BYTE -> ArrayByte(wantSection.shape, values)
            Datatype.CHAR, Datatype.STRING -> ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
            Datatype.DOUBLE -> ArrayDouble(wantSection.shape, values.asDoubleBuffer())
            Datatype.FLOAT -> ArrayFloat(wantSection.shape, values.asFloatBuffer())
            Datatype.INT -> ArrayInt(wantSection.shape, values.asIntBuffer())
            Datatype.LONG -> ArrayLong(wantSection.shape, values.asLongBuffer())
            Datatype.SHORT -> ArrayShort(wantSection.shape, values.asShortBuffer())
            else -> throw IllegalArgumentException("datatype ${v2.datatype}")
        }
    }

    /**
     * Structures must be fixed sized.
     *
     * @param s the record structure
     * @param section the record range to read
     * @return an Array of StructureData, with all the data read in.
     * @throws IOException on error
     * @throws InvalidRangeException if invalid section
     */
    @Throws(IOException::class, InvalidRangeException::class)
    private fun readStructureDataArray(v2: Variable, section: Section): ArrayStructureData {
        val vinfo = v2.spObject as Vinfo
        vinfo.setLayoutInfo(this.header, this)
        val recsize: Int = vinfo.elemSize

        requireNotNull(v2.datatype.typedef)
        require(v2.datatype.typedef is CompoundTypedef)
        val members = v2.datatype.typedef.members

        if (!vinfo.isLinked && !vinfo.isCompressed) {
            val layout = LayoutRegular(vinfo.start, recsize, v2.shape, section)
            return header.readArrayStructureData(layout, section.shape, members)
        } else if (vinfo.isLinked && !vinfo.isCompressed) {
            val input: InputStream = LinkedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout = LayoutRegular(0, recsize, v2.shape, section)
            return dataSource.readArrayStructureData(layout, section.shape, members)
        } else if (!vinfo.isLinked && vinfo.isCompressed) {
            val input: InputStream = getCompressedInputStream(vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, v2.shape, section)
            return dataSource.readArrayStructureData(layout, section.shape, members)
        } else if (vinfo.isLinked && vinfo.isCompressed) {
            val input: InputStream = getLinkedCompressedInputStream(vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, v2.shape, section)
            return dataSource.readArrayStructureData(layout, section.shape, members)
        } else {
            throw IllegalStateException()
        }
    }

    internal fun H4builder.readArrayStructureData(
        layout: Layout,
        shape: IntArray,
        members: List<StructureMember>
    ): ArrayStructureData {
        val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
        val sizeBytes = Section.computeSize(shape).toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        bb.order(state.byteOrder)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos()
            raf.readIntoByteBuffer(
                state,
                bb,
                layout.elemSize * chunk.destElem().toInt(),
                layout.elemSize * chunk.nelems()
            )
        }
        bb.position(0)
        bb.limit(bb.capacity())
        bb.order(state.byteOrder)
        return ArrayStructureData(shape, bb, layout.elemSize, members)
    }
}

internal fun PositioningDataInputStream.readArrayStructureData(layout: Layout, shape : IntArray, members : List<StructureMember>): ArrayStructureData {
    val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
    val sizeBytes = Section.computeSize(shape).toInt() * layout.elemSize
    val bb = ByteBuffer.allocate(sizeBytes)
    bb.order(state.byteOrder)
    while (layout.hasNext()) {
        val chunk: Layout.Chunk = layout.next()
        state.pos = chunk.srcPos()
        this.readIntoByteArray(state.pos, bb.array(), layout.elemSize * chunk.destElem().toInt(), layout.elemSize * chunk.nelems())
    }
    bb.position(0)
    bb.limit(bb.capacity())
    bb.order(state.byteOrder)
    return ArrayStructureData(shape, bb, layout.elemSize, members)
}
