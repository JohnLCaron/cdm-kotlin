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
    override fun type() = "netcdf3"
    override val size : Long get() = raf.size

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)
        val vinfo = v2.spObject as N3header.Vinfo
        val layout = if (!v2.hasUnlimited()) {
            LayoutRegular(vinfo.begin, vinfo.elemSize, v2.shape, IndexSpace(wantSection))
        } else {
            LayoutRegularSegmented(vinfo.begin, vinfo.elemSize, header.recsize, v2.shape, IndexSpace(wantSection))
        }
        return readDataWithLayout(layout, v2, wantSection)
    }

    private fun Variable.hasUnlimited() : Boolean {
        return this.dimensions.find { it == header.unlimitedDimension } != null
    }

    override fun chunkIterator(v2: Variable, section: Section?, maxElements : Int?): Iterator<ArraySection> {
        val wantSection = Section.fill(section, v2.shape)
        return NCmaxIterator(v2, wantSection, maxElements ?: 100_000)
    }

    private inner class NCmaxIterator(val v2: Variable, val wantSection : Section, maxElems: Int) : AbstractIterator<ArraySection>() {
        private val debugChunking = false
        val vinfo = v2.spObject as N3header.Vinfo
        private val maxIterator  = MaxChunker(maxElems,  IndexSpace(wantSection), v2.shape)

        override fun computeNext() {
            if (maxIterator.hasNext()) {
                val indexSection = maxIterator.next()
                if (debugChunking) println("  chunk=${indexSection}")
                val section = indexSection.section()

                val layout = if (!v2.hasUnlimited()) {
                    LayoutRegular(vinfo.begin, vinfo.elemSize, v2.shape, IndexSpace(section))
                } else {
                    // I think this will segment on the record dimension
                    LayoutRegularSegmented(vinfo.begin, vinfo.elemSize, header.recsize, v2.shape, IndexSpace(section))
                }

                val array = readDataWithLayout(layout, v2, section)
                setNext(ArraySection(array, section))
            } else {
                done()
            }
        }
    }

    @Throws(IOException::class)
    private fun readDataWithLayout(layout: Layout, v2: Variable, wantSection : Section): ArrayTyped<*> {
        require(wantSection.size() == layout.totalNelems)
        val vinfo = v2.spObject as N3header.Vinfo
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