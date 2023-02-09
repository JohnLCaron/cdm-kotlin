package sunya.cdm.hdf5

import sunya.cdm.dsl.structdsl
import sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteBuffer

// Message Type 8 "Data Storage Layout" : regular (contiguous), chunked, or compact (stored with the message)
@Throws(IOException::class)
fun H5builder.readDataLayoutMessage(state : OpenFileState) : DataLayoutMessage {
    val tstate = state.copy()
    val version = raf.readByte(tstate).toInt()
    val layoutClass = if (version == 3) raf.readByte(tstate).toInt() else raf.readByte(tstate.incr(1)).toInt()

    if (version < 3) {
        val rawdata =
            structdsl("MessageLayout", raf, state) {
                fld("version", 1)
                fld("rank", 1)
                fld("layoutClass", 1)
                skip(5)
                if (layoutClass != 0) { // not compact
                    fld("dataAddress", sizeOffsets)
                }
                array("dims", sizeLengths, "rank")
                if (layoutClass == 2) { // chunked
                    fld("chunkedElementSize", 4)
                }
                if (layoutClass == 0) { // compact
                    fld("compactDataSize", 4)
                    array("compactData", 1, "compactDataSize")
                }
            }
        rawdata.show()

        return when (layoutClass) {
            0 -> DataLayoutCompact(rawdata.getIntArray("dims"), rawdata.getByteBuffer("compactData"))
            1 -> DataLayoutContiguous(rawdata.getIntArray("dims"), rawdata.getLong("dataAddress"))
            2 -> DataLayoutChunked(rawdata.getIntArray("dims"), rawdata.getLong("dataAddress"), rawdata.getInt("chunkedElementSize"))
            else -> throw RuntimeException()
        }

    } else if (version == 3) {
        val rawdata =
            structdsl("MessageLayout3", raf, state) {
                fld("version", 1)
                fld("layoutClass", 1)
                when (layoutClass) {
                    0 -> { // compact
                        fld("compactDataSize", 2)
                        array("compactData", 1, "compactDataSize")
                    }

                    1 -> { // contiguous
                        fld("dataAddress", sizeOffsets)
                        fld("dataSize", sizeLengths)
                    }

                    2 -> { // chunked
                        fld("rank", 1)
                        fld("btreeAddress", sizeOffsets)
                        array("dims", sizeLengths, "rank")
                        fld("chunkedElementSize", 4)
                    }

                    else -> throw RuntimeException()
                }
            }
        if (debugMessage) rawdata.show()

        return when (layoutClass) {
            0 -> DataLayoutCompact3(rawdata.getByteBuffer("compactData"))
            1 -> DataLayoutContiguous3(rawdata.getLong("dataAddress"), rawdata.getLong("dataSize"))
            2 -> DataLayoutChunked(rawdata.getIntArray("dims"), rawdata.getLong("btreeAddress"), rawdata.getInt("chunkedElementSize"))
            else -> throw RuntimeException()
        }
    } else throw RuntimeException()

}

enum class LayoutClass(val num : Int) {
    Compact(0), Contiguous(1), Chunked(2);

    companion object {
        fun of(num: Int) : LayoutClass {
            return when (num) {
                0 -> Compact
                1 -> Contiguous
                2 -> Chunked
                else -> throw RuntimeException("Unknown LayoutClass $num")
            }
        }
    }
}

open class DataLayoutMessage(layoutClassNum: Int)  : MessageHeader(MessageType.Layout) {
    val layoutClass = LayoutClass.of(layoutClassNum)
}

data class DataLayoutCompact(val dims : IntArray, val compactData: ByteBuffer?) : DataLayoutMessage(0)
data class DataLayoutContiguous(val dims : IntArray, val dataAddress: Long) : DataLayoutMessage(1)
data class DataLayoutChunked(val dims : IntArray, val btreeAddress: Long, val chunkedElementSize : Int) : DataLayoutMessage(2)

data class DataLayoutCompact3(val compactData: ByteBuffer?) : DataLayoutMessage(0)
data class DataLayoutContiguous3(val dataAddress: Long, val dataSize: Long) : DataLayoutMessage(1)
