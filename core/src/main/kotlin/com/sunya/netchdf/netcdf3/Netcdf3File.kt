package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.*
import com.sunya.cdm.layout.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Netcdf3File(val filename : String) : Netchdf {
    private val raf : OpenFile = OpenFile(filename)
    private val header : N3header
    private val rootGroup : Group

    init {
        val rootBuilder = Group.Builder("")
        header = N3header(raf, rootBuilder)
        rootGroup = rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = cdl(this)
    override fun type() = header.formatType()
    override val size : Long get() = raf.size

    @Throws(IOException::class)
    override fun <T> readArrayData(v2: Variable<T>, section: SectionPartial?): ArrayTyped<T> {
        if (v2.nelems == 0L) {
            return ArrayEmpty(v2.shape.toIntArray(), v2.datatype)
        }
        val wantSection : Section = SectionPartial.fill(section, v2.shape)
        val vinfo = v2.spObject as VinfoN3
        val layout = if (!v2.hasUnlimited()) {
            LayoutRegular(vinfo.begin, vinfo.elemSize, wantSection)
        } else {
            LayoutRegularSegmented(vinfo.begin, vinfo.elemSize, header.recsize, wantSection)
        }
        return readDataWithLayout(layout, v2, wantSection)
    }

    private fun Variable<*>.hasUnlimited() : Boolean {
        return this.dimensions.find { it == header.unlimitedDimension } != null
    }

    override fun <T> chunkIterator(v2: Variable<T>, section: SectionPartial?, maxElements : Int?): Iterator<ArraySection<T>> {
        if (v2.nelems == 0L) {
            return listOf<ArraySection<T>>().iterator()
        }
        val wantSection = SectionPartial.fill(section, v2.shape)
        return NCmaxIterator(v2, wantSection, maxElements ?: 100_000)
    }

    private inner class NCmaxIterator<T>(val v2: Variable<T>, wantSection : Section, maxElems: Int) : AbstractIterator<ArraySection<T>>() {
        private val debugChunking = false
        val vinfo = v2.spObject as VinfoN3
        private val maxIterator  = MaxChunker(maxElems,  wantSection)

        override fun computeNext() {
            if (maxIterator.hasNext()) {
                val indexSection = maxIterator.next()
                if (debugChunking) println("  chunk=${indexSection}")
                val section = indexSection.section(v2.shape)

                val layout = if (!v2.hasUnlimited()) {
                    LayoutRegular(vinfo.begin, vinfo.elemSize, section)
                } else {
                    // I think this will segment on the record dimension
                    LayoutRegularSegmented(vinfo.begin, vinfo.elemSize, header.recsize, section)
                }

                val array = readDataWithLayout(layout, v2, section)
                setNext(ArraySection(array, section))
            } else {
                done()
            }
        }
    }

    @Throws(IOException::class)
    private fun <T> readDataWithLayout(layout: Layout, v2: Variable<T>, wantSection : Section): ArrayTyped<T> {
        require(wantSection.totalElements == layout.totalNelems)
        val vinfo = v2.spObject as VinfoN3
        val totalNbytes = (vinfo.elemSize * layout.totalNelems)
        require(totalNbytes < Int.MAX_VALUE)
        val values = ByteBuffer.allocate(totalNbytes.toInt())

        var bytesRead = 0
        val filePos = OpenFileState(vinfo.begin, ByteOrder.BIG_ENDIAN)
        while (layout.hasNext()) {
            val chunk = layout.next()
            filePos.pos = chunk.srcPos()
            val dstPos = (vinfo.elemSize * chunk.destElem()).toInt()
            val chunkBytes = vinfo.elemSize * chunk.nelems()
            bytesRead += raf.readIntoByteBufferDirect(filePos, values, dstPos, chunkBytes)
        }
        require(bytesRead == totalNbytes.toInt())

        val shape = wantSection.shape.toIntArray()
        val result = when (v2.datatype) {
            Datatype.BYTE -> ArrayByte(shape, values)
            Datatype.UBYTE -> ArrayUByte(shape, values)
            Datatype.CHAR -> ArrayUByte(shape, Datatype.CHAR, values)
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
        return result as ArrayTyped<T>
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Netchdf) return false

        if (filename != other.location()) return false
        if (rootGroup != other.rootGroup()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + rootGroup.hashCode()
        return result
    }

}