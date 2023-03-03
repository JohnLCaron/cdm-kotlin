package com.sunya.netchdf.hdf5

import com.sunya.cdm.dsl.structdsl
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteBuffer

// Message Type 8 "Data Layout" : regular (contiguous), chunked, or compact (stored with the message)
// The dimensions were specified in version 1 and 2. In version 3 and 4, dimensions are in the Dataspace message.
// When chunked, the last dimension is the chunk size. LOOK version 4 not done.

// The Data Layout message describes how the elements of a multi-dimensional array are stored in the HDF5 file.
// Four types of data layout are supported:
//  Contiguous: The array is stored in one contiguous area of the file. This layout requires that the size of the array
//     be constant: data manipulations such as chunking, compression, checksums, or encryption are not permitted.
//     The message stores the total storage size of the array. The offset of an element from the beginning of the
//     storage area is computed as in a C array.
//  Chunked: The array domain is regularly decomposed into chunks, and each chunk is allocated and stored separately.
//     This layout supports arbitrary element traversals, compression, encryption, and checksums (these features are
//     described in other messages). The message stores the size of a chunk instead of the size of the entire array;
//     the storage size of the entire array can be calculated by traversing the chunk index that stores the chunk addresses.
//  Compact: The array is stored in one contiguous block as part of this object header message.
//  Virtual: This is only supported for version 4 of the Data Layout message. The message stores information that is
//     used to locate the global heap collection containing the Virtual Dataset (VDS) mapping information. The mapping
//     associates the VDS to the source dataset elements that are stored across a collection of HDF5 files.


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
                array("dims", 4, "rank")
                if (layoutClass == 2) { // chunked
                    fld("chunkedElementSize", 4)
                }
                if (layoutClass == 0) { // compact
                    fld("compactDataSize", 4)
                    array("compactData", 1, "compactDataSize")
                }
            }
        if (debugMessage) rawdata.show()

        return when (layoutClass) {
            0 -> DataLayoutCompact(rawdata.getIntArray("dims"), rawdata.getByteBuffer("compactData"))
            1 -> DataLayoutContiguous(rawdata.getIntArray("dims"), rawdata.getLong("dataAddress"))
            2 -> DataLayoutChunked(version, rawdata.getIntArray("dims"), rawdata.getLong("dataAddress"), rawdata.getInt("chunkedElementSize"))
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
                        array("dims", 4,"rank")
                        fld("chunkedElementSize", 4)
                    }

                    else -> throw RuntimeException()
                }
            }
        if (debugMessage) rawdata.show()

        return when (layoutClass) {
            0 -> DataLayoutCompact3(rawdata.getByteBuffer("compactData"))
            1 -> DataLayoutContiguous3(rawdata.getLong("dataAddress"), rawdata.getLong("dataSize"))
            2 -> DataLayoutChunked(version, rawdata.getIntArray("dims"), rawdata.getLong("btreeAddress"), rawdata.getInt("chunkedElementSize"))
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
    override fun show() : String = "class=$layoutClass"
}
data class DataLayoutCompact(val dims : IntArray, val compactData: ByteBuffer?) : DataLayoutMessage(0)

data class DataLayoutContiguous(val dims : IntArray, val dataAddress: Long) : DataLayoutMessage(1) {
    override fun show() : String = "class=$layoutClass dims=${dims.contentToString()} dataAddress=$dataAddress"
}
data class DataLayoutChunked(val version : Int, val dims : IntArray, val btreeAddress: Long, val chunkedElementSize : Int) : DataLayoutMessage(2) {
    override fun show(): String = "class=$layoutClass dims=${dims.contentToString()} btreeAddress=$btreeAddress chunkedElementSize=$chunkedElementSize"
}

data class DataLayoutCompact3(val compactData: ByteBuffer?) : DataLayoutMessage(0)

data class DataLayoutContiguous3(val dataAddress: Long, val dataSize: Long) : DataLayoutMessage(1) {
    override fun show(): String = "class=$layoutClass dataAddress=$dataAddress dataSize=$dataSize"
}
