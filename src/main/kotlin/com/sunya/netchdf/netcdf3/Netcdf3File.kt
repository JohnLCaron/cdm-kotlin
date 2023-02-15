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
        rootGroup = rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun cdlStrict() = com.sunya.cdm.api.cdlStrict(this)

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable): ArrayTyped<*> {
        return if (!v2.isUnlimited()) readData(v2)
            else {
                val vinfo = v2.spObject as N3header.Vinfo
                val layout = LayoutRegularSegmented(vinfo.begin, v2.elementSize, header.recsize, v2.shape, null)
                readDataLayout(layout, v2)
            }
    }

    @Throws(IOException::class)
    fun readData(v2: Variable): ArrayTyped<*> {
        val vinfo = v2.spObject as N3header.Vinfo
        val nbytes = (v2.elementSize * v2.nelems)
        require(nbytes < Int.MAX_VALUE)
        val filePos = OpenFileState(vinfo.begin, ByteOrder.BIG_ENDIAN)
        val values = raf.readByteBuffer(filePos, nbytes.toInt())

        when (v2.datatype) {
            Datatype.CHAR, Datatype.BYTE -> {
                return ArrayByte(values, v2.shape)
            }

            Datatype.SHORT -> {
                return ArrayShort(values.asShortBuffer(), v2.shape)
            }

            Datatype.INT -> {
                return ArrayInt(values.asIntBuffer(), v2.shape)
            }

            Datatype.FLOAT -> {
                return ArrayFloat(values.asFloatBuffer(), v2.shape)
            }

            Datatype.DOUBLE -> {
                return ArrayDouble(values.asDoubleBuffer(), v2.shape)
            }

            Datatype.LONG -> {
                return ArrayLong(values.asLongBuffer(), v2.shape)
            }
            else -> throw IllegalArgumentException()
        }
    }

    @Throws(IOException::class)
    private fun readDataLayout(layout: Layout, v2: Variable): ArrayTyped<*> {
        val vinfo = v2.spObject as N3header.Vinfo
        val nbytes = (v2.elementSize * v2.nelems)
        require(nbytes < Int.MAX_VALUE)
        val filePos = OpenFileState(vinfo.begin, ByteOrder.BIG_ENDIAN)
        val values = ByteBuffer.allocate( nbytes.toInt())

        when (v2.datatype) {
            Datatype.CHAR, Datatype.BYTE -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), chunk.destElem.toInt(), chunk.nelems);
                }
                return ArrayByte(values, v2.shape)
            }


            Datatype.DOUBLE, Datatype.LONG -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 8 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 8 * chunk.destElem.toInt(),8 * chunk.nelems);
                }
                return if (v2.datatype == Datatype.LONG) ArrayLong(values.asLongBuffer(), v2.shape) else
                    ArrayDouble(values.asDoubleBuffer(), v2.shape)
            }

            Datatype.FLOAT, Datatype.INT -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 4 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 4 * chunk.destElem.toInt(),4 * chunk.nelems);
                }
                return if (v2.datatype == Datatype.INT) ArrayInt(values.asIntBuffer(), v2.shape) else
                    ArrayFloat(values.asFloatBuffer(), v2.shape)
            }

            Datatype.SHORT-> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 2 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 2 * chunk.destElem.toInt(),2 * chunk.nelems);
                }
                return ArrayShort(values.asShortBuffer(), v2.shape)
            }

            else -> throw IllegalArgumentException("datatype ${v2.datatype}")
        }
    }
}