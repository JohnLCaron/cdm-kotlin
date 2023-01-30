package sunya.cdm.netcdf3

import sunya.cdm.api.DataType
import sunya.cdm.api.Section
import sunya.cdm.api.Variable
import sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteOrder

class N3iosp(val raf : OpenFile) : Iosp {

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable): ArrayTyped<*> {
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
}