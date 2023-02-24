package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Netcdf3File(val filename : String) : Iosp, Netcdf {
    private val raf : OpenFile = OpenFile(filename)
    private val header : N3header
    private val rootGroup : Group

    init {
        val rootBuilder = Group.Builder("")
        header = N3header(raf, rootBuilder, null)
        /* rootBuilder.variables.forEach {
            if (it.datatype == Datatype.CHAR) {
                it.datatype = Datatype.STRING
                // seems like we should remove a dimension
            }
        } */
        rootGroup = rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl(strict : Boolean) = com.sunya.cdm.api.cdl(this, strict)

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
        val filePos = OpenFileState(vinfo.begin, ByteOrder.BIG_ENDIAN)
        val values = ByteBuffer.allocate(nbytes.toInt())

        when (v2.datatype) {
            Datatype.BYTE -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos()
                    val bytesRead = raf.readByteBuffer(filePos, chunk.nelems())
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), chunk.destElem().toInt(), chunk.nelems());
                }
                return ArrayByte(wantSection.shape, values)
            }

            Datatype.CHAR, Datatype.STRING -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos()
                    val bytesRead = raf.readByteBuffer(filePos, chunk.nelems())
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), chunk.destElem().toInt(), chunk.nelems());
                }
                return ArrayUByte(wantSection.shape, values).makeStringsFromBytes()
            }

            Datatype.DOUBLE, Datatype.LONG -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos()
                    val bytesRead = raf.readByteBuffer(filePos, 8 * chunk.nelems())
                    System.arraycopy(bytesRead.array(), 0, values.array(), 8 * chunk.destElem().toInt(), 8 * chunk.nelems())
                }
                return if (v2.datatype == Datatype.LONG) ArrayLong(wantSection.shape, values.asLongBuffer()) else
                    ArrayDouble(wantSection.shape, values.asDoubleBuffer())
            }

            Datatype.FLOAT, Datatype.INT -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos()
                    val bytesRead = raf.readByteBuffer(filePos, 4 * chunk.nelems())
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 4 * chunk.destElem().toInt(),4 * chunk.nelems());
                }
                return if (v2.datatype == Datatype.INT) ArrayInt(wantSection.shape, values.asIntBuffer()) else
                    ArrayFloat(wantSection.shape, values.asFloatBuffer())
            }

            Datatype.SHORT-> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos()
                    val bytesRead = raf.readByteBuffer(filePos, 2 * chunk.nelems())
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 2 * chunk.destElem().toInt(),2 * chunk.nelems());
                }
                return ArrayShort(wantSection.shape, values.asShortBuffer())
            }

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