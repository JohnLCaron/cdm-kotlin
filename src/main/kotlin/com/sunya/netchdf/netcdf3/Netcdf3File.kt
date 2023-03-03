package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.netcdf4.NetchdfFileFormat
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Netcdf3File(val filename : String) : Iosp, Netcdf {
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
    override fun cdl(strict : Boolean) = com.sunya.cdm.api.cdl(this, strict)
    override fun type() = NetchdfFileFormat.NC_FORMAT_CLASSIC.formatName()

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)
        val vinfo = v2.spObject as N3header.Vinfo
        val layout = if (!v2.isUnlimited()) {
            LayoutRegular(vinfo.begin, vinfo.elemSize, v2.shape, wantSection)
        } else {
            LayoutRegularSegmented(vinfo.begin, vinfo.elemSize, header.recsize, v2.shape, wantSection)
        }
        return readDataWithLayout(layout, v2, wantSection)
    }

    @Throws(IOException::class)
    private fun readDataWithLayout(layout: Layout, v2: Variable, wantSection : Section): ArrayTyped<*> {
        val vinfo = v2.spObject as N3header.Vinfo
        val nbytes = (vinfo.elemSize * v2.nelems)
        require(nbytes < Int.MAX_VALUE)
        require(nbytes < 100_000_000) { "${v2.name}[${wantSection}]"}
        val filePos = OpenFileState(vinfo.begin, ByteOrder.BIG_ENDIAN)
        val values = ByteBuffer.allocate(nbytes.toInt())

        while (layout.hasNext()) {
            val chunk = layout.next()
            filePos.pos = chunk.srcPos()
            val bytesRead = raf.readByteBufferDirect(filePos, vinfo.elemSize * chunk.nelems())
            // extra copy
            System.arraycopy(bytesRead.array(), 0, values.array(), vinfo.elemSize * chunk.destElem().toInt(), vinfo.elemSize * chunk.nelems());
        }

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
        if (other !is Netcdf) return false

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