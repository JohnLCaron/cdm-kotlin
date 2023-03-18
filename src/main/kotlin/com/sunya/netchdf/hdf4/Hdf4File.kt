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

/**
 *  @param strict true = make it agree with nclib if possible
 */
class Hdf4File(val filename : String, strict : Boolean = false) : Iosp, Netchdf {
    private val raf: OpenFile = OpenFile(filename)
    internal val header: H4builder
    private val rootGroup: Group

    var valueCharset: Charset = StandardCharsets.UTF_8

    init {
        header = H4builder(raf, valueCharset, strict)
        header.make()
        rootGroup = header.rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = "hdf4   "

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filledSection = Section.fill(section, v2.shape)
        return if (v2.datatype == Datatype.COMPOUND) {
            readStructureDataArray(v2, filledSection)
        } else {
            readRegularDataArray(v2, filledSection)
        }
    }

    private fun readRegularDataArray(v: Variable, section: Section): ArrayTyped<*> {
        val vinfo = v.spObject as Vinfo

        if (vinfo.hasNoData) {
            return ArraySingle(section.shape, v.datatype, vinfo.getFillValueOrDefault())
        }

        if (vinfo.svalue != null) {
            return ArrayString(intArrayOf(), listOf(vinfo.svalue!!))
        }

        if (vinfo.tagData != null) {
            vinfo.setLayoutInfo(this) // make sure needed info is present
        }

        if (!vinfo.isCompressed) {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(vinfo.start, vinfo.elemSize, v.shape, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                val layout = LayoutSegmented(vinfo.segPos, vinfo.segSize, vinfo.elemSize, v.shape, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                return H4chunkReader(header).readChunkedDataNew(v, section)
            }
        } else {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(0, vinfo.elemSize, v.shape, section)
                val input: InputStream = getCompressedInputStream(header, vinfo)
                val reader = PositioningDataInputStream(input)
                return readDataWithFill(reader, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                val layout = LayoutRegular(0, vinfo.elemSize, v.shape, section)
                val input: InputStream = getLinkedCompressedInputStream(header, vinfo)
                val reader = PositioningDataInputStream(input)
                return readDataWithFill(reader, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                return H4chunkReader(header).readChunkedDataNew(v, section)
            }
        }
        throw IllegalStateException()
    }

    private fun readDataWithFill(reader: ReaderIntoByteArray, layout: Layout, v2: Variable, fillValue: Any?, wantSection: Section)
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
            bytesRead += reader.readIntoByteArray(filePos, values.array(), dstPos, chunkBytes)
        }
        values.position(0)

        return when (v2.datatype) {
            Datatype.BYTE -> ArrayByte(wantSection.shape, values)
            Datatype.UBYTE -> ArrayUByte(wantSection.shape, values)
            Datatype.CHAR, Datatype.STRING -> ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
            Datatype.DOUBLE -> ArrayDouble(wantSection.shape, values.asDoubleBuffer())
            Datatype.FLOAT -> ArrayFloat(wantSection.shape, values.asFloatBuffer())
            Datatype.INT -> ArrayInt(wantSection.shape, values.asIntBuffer())
            Datatype.UINT -> ArrayUInt(wantSection.shape, values.asIntBuffer())
            Datatype.LONG -> ArrayLong(wantSection.shape, values.asLongBuffer())
            Datatype.ULONG -> ArrayULong(wantSection.shape, values.asLongBuffer())
            Datatype.SHORT -> ArrayShort(wantSection.shape, values.asShortBuffer())
            Datatype.USHORT -> ArrayUShort(wantSection.shape, values.asShortBuffer())
            else -> throw IllegalArgumentException("datatype ${v2.datatype}")
        }
    }

    private fun readCompressedDataWithFill(reader: ReaderIntoByteArray, layout: LayoutBB, v2: Variable, fillValue: Any?, wantSection: Section)
            : ArrayTyped<*> {
        require(wantSection.size() == layout.totalNelems)
        val vinfo = v2.spObject as Vinfo
        val totalNbytes = (vinfo.elemSize * layout.totalNelems)
        require(totalNbytes < Int.MAX_VALUE)
        val values = ByteBuffer.allocate(totalNbytes.toInt())
        if (fillValue != null) {
            prefill(values, layout.totalNelems.toInt(), v2.datatype, fillValue)
        }

        var bytesRead = 0
        var elemsRead = 0
        var count = 0
        while (layout.hasNext()) {
            val chunk = layout.next()
            // if (count < 100) println("$count chunk = $chunk")
            val chunkBB = chunk.byteBuffer
            chunkBB.position(0)
            // copy these bytes into destination
            val chunkBytes = vinfo.elemSize * chunk.nelems()
            var dstPos = layout.elemSize * chunk.destElem().toInt()
            for (i in 0 until chunkBytes) {
                values.put(dstPos, chunkBB.get())
                dstPos++
            }
            bytesRead += chunkBytes
            elemsRead += chunk.nelems()
            count++
        }
        require(elemsRead == layout.totalNelems.toInt())
        values.position(0)

        return when (v2.datatype) {
            Datatype.BYTE -> ArrayByte(wantSection.shape, values)
            Datatype.UBYTE -> ArrayUByte(wantSection.shape, values)
            Datatype.CHAR, Datatype.STRING -> ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
            Datatype.DOUBLE -> ArrayDouble(wantSection.shape, values.asDoubleBuffer())
            Datatype.FLOAT -> ArrayFloat(wantSection.shape, values.asFloatBuffer())
            Datatype.INT -> ArrayInt(wantSection.shape, values.asIntBuffer())
            Datatype.UINT -> ArrayUInt(wantSection.shape, values.asIntBuffer())
            Datatype.LONG -> ArrayLong(wantSection.shape, values.asLongBuffer())
            Datatype.ULONG -> ArrayULong(wantSection.shape, values.asLongBuffer())
            Datatype.SHORT -> ArrayShort(wantSection.shape, values.asShortBuffer())
            Datatype.USHORT -> ArrayUShort(wantSection.shape, values.asShortBuffer())
            else -> throw IllegalArgumentException("datatype ${v2.datatype}")
        }
    }

    fun prefill(values: ByteBuffer, nelems: Int, datatype: Datatype, fillValue : Any) {
        values.position(0)
        when (datatype) {
            Datatype.SHORT -> {
                val sbb = values.asShortBuffer()
                val fill = fillValue as Short
                repeat(nelems) { sbb.put(fill) }
            }
            Datatype.FLOAT -> {
                val fbb = values.asFloatBuffer()
                val fill = fillValue as Float
                repeat(nelems) { fbb.put(fill) }
            }
        }

        values.position(0)
    }

    @Throws(IOException::class, InvalidRangeException::class)
    private fun readStructureDataArray(v2: Variable, section: Section): ArrayStructureData {
        val vinfo = v2.spObject as Vinfo
        vinfo.setLayoutInfo(this)
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
            val input: InputStream = getCompressedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, v2.shape, section)
            return dataSource.readArrayStructureData(layout, section.shape, members)

        } else if (vinfo.isLinked && vinfo.isCompressed) {
            val input: InputStream = getLinkedCompressedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, v2.shape, section)
            return dataSource.readArrayStructureData(layout, section.shape, members)

        } else {
            throw IllegalStateException()
        }
    }
}

@Throws(IOException::class)
internal fun getCompressedInputStream(h4: H4builder, vinfo: Vinfo): InputStream {
    // probably could construct an input stream from a channel from a raf for now, just read it all in.
    val buffer = h4.raf.readBytes(OpenFileState(vinfo.start, ByteOrder.BIG_ENDIAN), vinfo.length)
    val input = ByteArrayInputStream(buffer)
    return InflaterInputStream(input)
}

internal fun getLinkedCompressedInputStream(h4: H4builder, vinfo: Vinfo): InputStream {
    return InflaterInputStream(LinkedInputStream(h4, vinfo))
}

internal fun readStructureDataArray(h4: H4builder, vinfo: Vinfo, shape: IntArray, members: List<StructureMember>)
: ArrayStructureData {
    val recsize: Int = vinfo.elemSize
    if (!vinfo.isLinked && !vinfo.isCompressed) {
        val layout = LayoutRegular(vinfo.start, recsize, shape, null)

        return h4.readArrayStructureData(layout, shape, members)
    } else if (vinfo.isLinked && !vinfo.isCompressed) {
        val input: InputStream = LinkedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout = LayoutRegular(0, recsize, shape, null)
        return dataSource.readArrayStructureData(layout, shape, members)

    } else if (!vinfo.isLinked && vinfo.isCompressed) {
        val input: InputStream = getCompressedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout: Layout = LayoutRegular(0, recsize, shape, null)
        return dataSource.readArrayStructureData(layout, shape, members)

    } else if (vinfo.isLinked && vinfo.isCompressed) {
        val input: InputStream = getLinkedCompressedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout: Layout = LayoutRegular(0, recsize, shape, null)
        return dataSource.readArrayStructureData(layout, shape, members)

    } else {
        throw IllegalStateException()
    }
}

internal fun H4builder.readArrayStructureData(layout: Layout, shape: IntArray, members: List<StructureMember>)
: ArrayStructureData {
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

internal fun PositioningDataInputStream.readArrayStructureData(layout: Layout, shape : IntArray, members : List<StructureMember>)
: ArrayStructureData {
    val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
    val sizeBytes = Section.computeSize(shape).toInt() * layout.elemSize
    val bb = ByteBuffer.allocate(sizeBytes)
    bb.order(state.byteOrder)

    while (layout.hasNext()) {
        val chunk: Layout.Chunk = layout.next()
        state.pos = chunk.srcPos()
        this.readIntoByteArray(state, bb.array(), layout.elemSize * chunk.destElem().toInt(), layout.elemSize * chunk.nelems())
    }
    bb.position(0)
    bb.limit(bb.capacity())
    bb.order(state.byteOrder)
    return ArrayStructureData(shape, bb, layout.elemSize, members)
}
