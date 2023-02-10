package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.DataType
import com.sunya.cdm.api.Group
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Netcdf3File(val filename : String) : Iosp {
    private val raf : OpenFile = OpenFile(filename)
    private val header : N3header
    private val rootGroup : Group

    init {
        val rootBuilder = Group.Builder("")
        header = N3header(raf, rootBuilder, null)
        rootGroup = rootBuilder.build(null)
    }

    override fun rootGroup() = rootGroup

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

        when (v2.dataType) {
            DataType.CHAR, DataType.BYTE -> {
                return ArrayByte(values, v2.shape)
            }

            DataType.SHORT -> {
                return ArrayShort(values.asShortBuffer(), v2.shape)
            }

            DataType.INT -> {
                return ArrayInt(values.asIntBuffer(), v2.shape)
            }

            DataType.FLOAT -> {
                return ArrayFloat(values.asFloatBuffer(), v2.shape)
            }

            DataType.DOUBLE -> {
                return ArrayDouble(values.asDoubleBuffer(), v2.shape)
            }

            DataType.LONG -> {
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

        when (v2.dataType) {
            DataType.CHAR, DataType.BYTE -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), chunk.destElem.toInt(), chunk.nelems);
                }
                return ArrayByte(values, v2.shape)
            }


            DataType.DOUBLE, DataType.LONG -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 8 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 8 * chunk.destElem.toInt(),8 * chunk.nelems);
                }
                return if (v2.dataType == DataType.LONG) ArrayLong(values.asLongBuffer(), v2.shape) else
                    ArrayDouble(values.asDoubleBuffer(), v2.shape)
            }

            DataType.FLOAT, DataType.INT -> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 4 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 4 * chunk.destElem.toInt(),4 * chunk.nelems);
                }
                return if (v2.dataType == DataType.INT) ArrayInt(values.asIntBuffer(), v2.shape) else
                    ArrayFloat(values.asFloatBuffer(), v2.shape)
            }

            DataType.SHORT-> {
                while (layout.hasNext()) {
                    val chunk = layout.next()
                    filePos.pos = chunk.srcPos
                    val bytesRead = raf.readByteBuffer(filePos, 2 * chunk.nelems)
                    // extra copy
                    System.arraycopy(bytesRead.array(), 0, values.array(), 2 * chunk.destElem.toInt(),2 * chunk.nelems);
                }
                return ArrayShort(values.asShortBuffer(), v2.shape)
            }

            else -> throw IllegalArgumentException("dataType ${v2.dataType}")
        }
    }
}