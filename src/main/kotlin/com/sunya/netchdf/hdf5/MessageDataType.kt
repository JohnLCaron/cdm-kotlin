package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder

//// Message Type 3 : "Datatype"
// The datatype message defines the datatype for each element of a dataset or a common datatype for sharing between
// multiple datasets. A datatype can describe an atomic type like a fixed- or floating-point type or more complex types
// like a C struct (compound datatype), array (array datatype), or C++ vector (variable-length datatype).
//
// Datatype messages that are part of a dataset object do not describe how elements are related to one another;
// the dataspace message is used for that purpose. Datatype messages that are part of a committed datatype (formerly
// named datatype) message describe a common datatype that can be shared by multiple datasets in the file.

enum class Datatype5(val num : Int) {
    Fixed(0), Floating(1), Time(2), String(3), BitField(4), Opaque(5),
    Compound(6), Reference(7), Enumerated(8), Vlen(9), Array(10);

    companion object {
        fun of(num: Int) : Datatype5 {
            return when (num) {
                0 -> Fixed
                1 -> Floating
                2 -> Time
                3 -> String
                4 -> BitField
                5 -> Opaque
                6 -> Compound
                7 -> Reference
                8 -> Enumerated
                9 -> Vlen
                10 -> Array
                else -> throw RuntimeException("Unknown Datatype5 $num")
            }
        }
    }
}

/**
 * @param elemSize The size of a datatype element in bytes.
 */
open class DatatypeMessage(val address : Long, val type: Datatype5, val elemSize: Int, val endian: ByteOrder?) :
    MessageHeader(MessageType.Datatype) {
    open fun unsigned() = false
    open fun endian() = endian?: ByteOrder.LITTLE_ENDIAN

    override fun show() : String {
        return "$type"
    }
}

open class DatatypeFixed(address : Long, elemSize: Int, endian: ByteOrder, val unsigned: Boolean) :
    DatatypeMessage(address, Datatype5.Fixed, elemSize, endian) {
    override fun unsigned() = unsigned
    override fun show() : String {
        return "$type elemSize=$elemSize"
    }
}

class DatatypeFloating(address : Long, elemSize: Int, endian: ByteOrder) : DatatypeMessage(address, Datatype5.Floating, elemSize, endian)

class DatatypeTime(address : Long, elemSize: Int, endian: ByteOrder) : DatatypeMessage(address, Datatype5.Time, elemSize, endian)

class DatatypeString(address : Long, elemSize: Int) : DatatypeMessage(address, Datatype5.String, elemSize, null)

class DatatypeBitField(address : Long, elemSize: Int, endian: ByteOrder, unsigned: Boolean, val bitOffset : Short,
                       val bitPrecision : Short) : DatatypeFixed(address, elemSize, endian, unsigned)

class DatatypeOpaque(address : Long, elemSize: Int, val desc: String) : DatatypeMessage(address, Datatype5.Opaque, elemSize, null)

class DatatypeCompound(address : Long, elemSize: Int, val members: List<StructureMember5>) :
    DatatypeMessage(address, Datatype5.Compound, elemSize, null) {
    override fun show() : String {
        return "$type elemSize=$elemSize"
    }
}

// LOOK if all we have is a mdt, must be a scalar ?? Or could be an array?
class StructureMember5(val name: String, val offset: Int, val mdt: DatatypeMessage)

/**
 * @param elemSize dunno, maybe size of offset in field? maybe ignored?
 * @param referenceType
 *  0) Object Reference: A reference to another object in this HDF5 file.
 *  1) Dataset Region Reference: A reference to a region within a dataset in this HDF5 file.
 */
class DatatypeReference(address : Long, elemSize: Int, val referenceType: Int) : DatatypeMessage(address, Datatype5.Reference, elemSize, null)

class DatatypeEnum(
    address : Long,
    elemSize: Int,
    val base: DatatypeMessage,
    val names: List<String>,
    val nums: List<Int>
) : DatatypeMessage(address, Datatype5.Enumerated, elemSize, null) {

    val valueMap = lazy {
        require(names.size == nums.size)
        val values = mutableMapOf<Int, String>()
        nums.onEachIndexed{idx, num -> values[num] = names[idx]}
        values
    }

    override fun show() : String {
        return "$type n=${names.size} base=(${base.show()})"
    }
}

class DatatypeVlen(address : Long, elemSize: Int, val base: DatatypeMessage, val isVString: Boolean) :
    DatatypeMessage(address, Datatype5.Vlen, elemSize, null) {
    override fun show() : String {
        return "$type elemSize=$elemSize base=(${base.show()}) isVString=$isVString"
    }
}

class DatatypeArray(address : Long, elemSize: Int, val base: DatatypeMessage, val dims: IntArray) :
    DatatypeMessage(address, Datatype5.Array, elemSize, null) {
    override fun show() : String {
        return "$type elemSize=$elemSize base=(${base.show()}) dims=${dims.contentToString()}"
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Throws(IOException::class)
fun H5builder.readDatatypeMessage(state: OpenFileState): DatatypeMessage {
    val address = state.pos
    val tandv = raf.readByte(state).toInt()
    val type = tandv and 0xf // lower 4 bits
    val version = tandv and 0xf0 shr 4 // upper 4 bits
    val flags0 = raf.readByte(state).toInt()
    val flags1 = raf.readByte(state).toInt()
    val flags2 = raf.readByte(state).toInt()
    val elemSize = raf.readInt(state)

    when (type) {
        0 -> {
            val bitOffset = raf.readShort(state)
            val bitPrecision = raf.readShort(state)
            //require(bitOffset.toInt() == 0 && bitPrecision % 8 == 0)
            //    {"bitOffset $bitOffset should be 0, bitPrecision $bitPrecision should be multiple of 8"}
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val unsigned = (flags0 and 8 == 0)
            return DatatypeFixed(address, elemSize, endian, unsigned)
        }

        1 -> {
            // LOOK anything you want as long as its IEEE
            val bitOffset = raf.readShort(state)
            val bitPrecision = raf.readShort(state)
            val expLocation = raf.readByte(state)
            val expSize = raf.readByte(state)
            val manLocation = raf.readByte(state)
            val manSize = raf.readByte(state)
            val expBias = raf.readInt(state)
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val unsigned = (flags0 and 8 == 0)
            return DatatypeFloating(address, elemSize, endian)
        }

        2 -> {
            // LOOK no units, worthless, assume its integral
            val bitPrecision = raf.readInt(state)
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            return DatatypeTime(address, elemSize, endian)
        }

        3 -> {
            // could also store padding type and character set.
            // padding : always check for zero termination.
            // character set, ASCII < UTF8, so always use UTF8
            return DatatypeString(address, elemSize)
        }

        4 -> {
            val bitOffset = raf.readShort(state)
            val bitPrecision = raf.readShort(state)
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val unsigned = (flags0 and 8 == 0)
            // LOOK bitOffset, bitPrecision, to support packing ??
            return DatatypeBitField(address, elemSize, endian, unsigned, bitOffset, bitPrecision)
        }

        5 -> {
            val len = flags0
            val desc = raf.readString(state, len)
            return DatatypeOpaque(address, elemSize, desc)
        }

        6 -> {
            // compound
            val nmembers: Int = makeUnsignedIntFromBytes(flags1, flags0)
            val members = mutableListOf<StructureMember5>()
            for (i in 0 until nmembers) {
                members.add(this.readStructureMember(state, version, elemSize))
            }
            return DatatypeCompound(address, elemSize, members)
        }

        7 -> {
            val referenceType = flags0 and 0xf
            return DatatypeReference(address, elemSize, referenceType)
        }

        8 -> {
            val nmembers: Int = makeUnsignedIntFromBytes(flags1, flags0)
            val base = this.readDatatypeMessage(state)

            // read the enum names
            val enumNames = mutableListOf<String>()
            for (i in 0 until nmembers) {
                if (version < 3) enumNames.add(readStringZ(state, 8)) // padding
                else enumNames.add(readStringZ(state))  // no padding
            }

            // read the enum values; must switch to base byte order (!)
            val tstate = if (base.endian != null) state.copy(byteOrder = base.endian) else state
            val enumNums = mutableListOf<Int>()
            for (i in 0 until nmembers) {
                enumNums.add(readVariableSizeUnsigned(tstate, base.elemSize).toInt())
            }
            // LOOK since we've switched to tstate, the state position isnt updated. but we can igmore since this is the
            //  last field in the message

            return DatatypeEnum(address, elemSize, base, enumNames, enumNums)
        }

        9 -> {
            val isVString = flags0 and 0xf == 1
            val base = this.readDatatypeMessage(state)
            // TODO padding and charset

            return DatatypeVlen(address, elemSize, base, isVString)
        }

        10 -> {
            val ndims = raf.readByte(state).toInt()
            state.pos += 3

            val dim = IntArray(ndims)
            for (i in 0 until ndims) {
                dim[i] = raf.readInt(state)
            }

            if (version < 3) { // not present in version 3, never used anyway
                val pdim = IntArray(ndims)
                for (i in 0 until ndims) pdim[i] = raf.readInt(state)
            }

            val base = this.readDatatypeMessage(state)
            return DatatypeArray(address, elemSize, base, dim)
        }

        else -> throw RuntimeException("Unimplemented Datatype = $type")
    }
}

@Throws(IOException::class)
fun H5builder.readStructureMember(state: OpenFileState, version: Int, elemSize: Int): StructureMember5 {
    // dont know how long it is, read until 0 terminated and then (if version < 8) pad to 8 bytes
    val pad = if (version < 3) 8 else 0
    val name = this.readStringZ(state, pad)
    val offset = if (version < 3) {
        raf.readInt(state) // always 4 bytes
    } else {
        // var length of bytes, stupid
        this.readVariableSizeMax(state, elemSize.toLong()).toInt()
    }

    // LOOK ignore these, because replicated in mdt? or in a seperate dataspace message ?
    if (version == 1) {
        state.pos += 28
    }

    val mdt = this.readDatatypeMessage(state)

    return StructureMember5(name, offset, mdt)
}

// read a zero terminated string
// pad to next padByte boundary if needed
@Throws(IOException::class)
fun H5builder.readStringZ(state: OpenFileState, padByte: Int? = null): String {
    val filePos: Long = state.pos
    var count = 0
    // have to include the terminating zero in the count
    while (true) {
        count++
        if (raf.readByte(state).toInt() == 0) break
    }
    state.pos = filePos
    val result: String = raf.readString(state, count)
    if (padByte != null && padByte > 0) {
        state.pos += padding(count, padByte)
    }
    return result
}

fun padding(nbytes: Int, multipleOf: Int): Int {
    var pad = nbytes % multipleOf
    if (pad != 0) pad = multipleOf - pad
    return pad
}

fun makeUnsignedIntFromBytes(upper: Int, lower: Int): Int {
    return upper * 256 + lower
}

