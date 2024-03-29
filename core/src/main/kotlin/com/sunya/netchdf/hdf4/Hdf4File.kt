package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.*
import com.sunya.cdm.layout.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream

class Hdf4File(val filename : String) : Netchdf {
    private val raf: OpenFile = OpenFile(filename)
    val header: H4builder
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
    override fun cdl() = cdl(this)
    override fun type() = header.type()
    override val size : Long get() = raf.size

    @Throws(IOException::class)
    override fun <T> readArrayData(v2: Variable<T>, section: SectionPartial?): ArrayTyped<T> {
        if (v2.nelems == 0L) {
            return ArrayEmpty(v2.shape.toIntArray(), v2.datatype)
        }
        val filledSection = SectionPartial.fill(section, v2.shape)
        return if (v2.datatype == Datatype.COMPOUND) {
            readStructureDataArray(v2, filledSection) as ArrayTyped<T>
        } else {
            readRegularDataArray(v2, filledSection)
        }
    }

    override fun <T> chunkIterator(v2: Variable<T>, section: SectionPartial?, maxElements : Int?): Iterator<ArraySection<T>> {
        if (v2.nelems == 0L) {
            return listOf<ArraySection<T>>().iterator()
        }
        val wantSection = SectionPartial.fill(section, v2.shape)
        val vinfo = v2.spObject as Vinfo

        return if (vinfo.isChunked) {  // LOOK isLinked?
            H4chunkIterator(header, v2, wantSection)
        } else {
            H4maxIterator(v2, wantSection, maxElements ?: 100_000)
        }
    }

    private inner class H4maxIterator<T>(val v2: Variable<T>, wantSection : Section, maxElems: Int) : AbstractIterator<ArraySection<T>>() {
        private val debugChunking = false
        private val maxIterator  = MaxChunker(maxElems,  wantSection)

        override fun computeNext() {
            if (maxIterator.hasNext()) {
                val indexSection = maxIterator.next()
                if (debugChunking) println("  chunk=${indexSection}")

                val section = indexSection.section(v2.shape)
                val array = if (v2.datatype == Datatype.COMPOUND) {
                    readStructureDataArray(v2, section)
                } else {
                    readRegularDataArray(v2, section)
                }
                setNext(ArraySection(array as ArrayTyped<T>, section))
            } else {
                done()
            }
        }
    }

    private fun <T> readRegularDataArray(v: Variable<T>, section: Section): ArrayTyped<T> {
        requireNotNull(v.spObject) { "Variable ${v.name}"}
        val vinfo = v.spObject as Vinfo

        if (vinfo.tagData != null) {
            vinfo.setLayoutInfo(header) // make sure needed info is present LOOK why wait until now ??
        }

        if (vinfo.hasNoData) {
            return ArraySingle(section.shape.toIntArray(), v.datatype, vinfo.fillValue!!)
        }

        if (vinfo.svalue != null) {
            return ArrayString(intArrayOf(), listOf(vinfo.svalue!!)) as ArrayTyped<T>
        }

        if (!vinfo.isCompressed) {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(vinfo.start, vinfo.elemSize, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                // val layout = LayoutSegmentedOld(vinfo.segPos, vinfo.segSize, vinfo.elemSize, v.shape, section)
                val layout = LayoutSegmented(vinfo.segPos, vinfo.segSize, vinfo.elemSize, section)
                return readDataWithFill(raf, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                return H4chunkReader(header).readChunkedData(v, section)
            }
        } else {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout = LayoutRegular(0, vinfo.elemSize, section)
                val input: InputStream = getCompressedInputStream(header, vinfo)
                val reader = PositioningDataInputStream(input)
                return readDataWithFill(reader, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isLinked) {
                val layout = LayoutRegular(0, vinfo.elemSize, section)
                val input: InputStream = getLinkedCompressedInputStream(header, vinfo)
                val reader = PositioningDataInputStream(input)
                return readDataWithFill(reader, layout, v, vinfo.fillValue, section)

            } else if (vinfo.isChunked) {
                return H4chunkReader(header).readChunkedData(v, section)
            }
        }
        throw IllegalStateException()
    }

    // LOOK use fillValue
    private fun <T> readDataWithFill(reader: ReaderIntoByteArray, layout: Layout, v2: Variable<T>, fillValue: Any?, wantSection: Section)
            : ArrayTyped<T> {
        require(wantSection.totalElements == layout.totalNelems)
        val vinfo = v2.spObject as Vinfo
        val totalNbytes = (vinfo.elemSize * layout.totalNelems)
        require(totalNbytes < Int.MAX_VALUE)
        val values = ByteBuffer.allocate(totalNbytes.toInt())

        var bytesRead = 0
        val filePos = OpenFileState(vinfo.start, vinfo.endian)
        while (layout.hasNext()) {
            val chunk = layout.next()
            filePos.pos = chunk.srcPos()
            val dstPos = (vinfo.elemSize * chunk.destElem()).toInt()
            val chunkBytes = vinfo.elemSize * chunk.nelems()
            bytesRead += reader.readIntoByteArray(filePos, values.array(), dstPos, chunkBytes)
        }
        values.position(0)

        val shape = wantSection.shape.toIntArray()
        val array = when (v2.datatype) {
            Datatype.BYTE -> ArrayByte(shape, values)
            Datatype.UBYTE, Datatype.CHAR -> ArrayUByte(shape, v2.datatype as Datatype<UByte>, values)
            Datatype.STRING -> ArrayUByte(shape, values).makeStringsFromBytes()
            Datatype.DOUBLE -> ArrayDouble(shape, values)
            Datatype.FLOAT -> ArrayFloat(shape, values)
            Datatype.INT -> ArrayInt(shape, values)
            Datatype.UINT -> ArrayUInt(shape, values)
            Datatype.LONG -> ArrayLong(shape, values)
            Datatype.ULONG -> ArrayULong(shape, values)
            Datatype.SHORT -> ArrayShort(shape, values)
            Datatype.USHORT -> ArrayUShort(shape, values)
            else -> throw IllegalArgumentException("datatype ${v2.datatype}")
        }
        return array as ArrayTyped<T>
    }

    @Throws(IOException::class, InvalidRangeException::class)
    private fun <T> readStructureDataArray(v2: Variable<T>, section: Section): ArrayStructureData {
        val vinfo = v2.spObject as Vinfo

        if (vinfo.tagData != null) {
            vinfo.setLayoutInfo(header) // make sure needed info is present LOOK why wait until now ??
        } else {
            vinfo.hasNoData = true
        }

        requireNotNull(v2.datatype.typedef)
        require(v2.datatype.typedef is CompoundTypedef)
        val recsize: Int = vinfo.elemSize
        val members = v2.datatype.typedef.members
        val shape = section.shape.toIntArray()

        val array = if (vinfo.hasNoData) {
            // class ArrayStructureData(shape : IntArray, val bb : ByteBuffer, val recsize : Int, val members : List<StructureMember>)
            // can you just use a zero bb ??
            val nbytes = (recsize * section.totalElements).toInt()
            val bbz = ByteBuffer.allocate(nbytes)
            ArrayStructureData(shape, bbz, recsize, members)

        } else if (!vinfo.isLinked && !vinfo.isCompressed) {
            val layout = LayoutRegular(vinfo.start, recsize, section)
            header.readArrayStructureData(layout, shape, members)

        } else if (vinfo.isLinked && !vinfo.isCompressed) {
            val input: InputStream = LinkedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout = LayoutRegular(0, recsize, section)
            dataSource.readArrayStructureData(layout, shape, members)

        } else if (!vinfo.isLinked && vinfo.isCompressed) {
            val input: InputStream = getCompressedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, section)
            dataSource.readArrayStructureData(layout, shape, members)

        } else  { // if (vinfo.isLinked && vinfo.isCompressed)
            val input: InputStream = getLinkedCompressedInputStream(header, vinfo)
            val dataSource = PositioningDataInputStream(input)
            val layout: Layout = LayoutRegular(0, recsize, section)
            dataSource.readArrayStructureData(layout, shape, members)
        }

        return array
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

// called from special.getDataChunks()
internal fun readStructureDataArray(h4: H4builder, vinfo: Vinfo, section: Section, members: List<StructureMember<*>>): ArrayStructureData {
    val shape = section.shape.toIntArray()
    val recsize: Int = vinfo.elemSize

    if (!vinfo.isLinked && !vinfo.isCompressed) {
        val layout = LayoutRegular(vinfo.start, recsize, section)
        return h4.readArrayStructureData(layout, shape, members)
    } else if (vinfo.isLinked && !vinfo.isCompressed) {
        val input: InputStream = LinkedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout = LayoutRegular(0, recsize, section)
        return dataSource.readArrayStructureData(layout, shape, members)

    } else if (!vinfo.isLinked && vinfo.isCompressed) {
        val input: InputStream = getCompressedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout: Layout = LayoutRegular(0, recsize, section)
        return dataSource.readArrayStructureData(layout, shape, members)

    } else { // if (vinfo.isLinked && vinfo.isCompressed) {
        val input: InputStream = getLinkedCompressedInputStream(h4, vinfo)
        val dataSource = PositioningDataInputStream(input)
        val layout: Layout = LayoutRegular(0, recsize, section)
        return dataSource.readArrayStructureData(layout, shape, members)
    }
}

internal fun H4builder.readArrayStructureData(layout: Layout, shape: IntArray, members: List<StructureMember<*>>)
: ArrayStructureData {
    val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
    val sizeBytes = shape.computeSize() * layout.elemSize
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

internal fun PositioningDataInputStream.readArrayStructureData(layout: Layout, shape : IntArray, members : List<StructureMember<*>>)
: ArrayStructureData {
    val state = OpenFileState(0, ByteOrder.BIG_ENDIAN)
    val sizeBytes = shape.computeSize() * layout.elemSize
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
