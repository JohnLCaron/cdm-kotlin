package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder

//// Message Type 3 : "Datatype"

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
open class DatatypeMessage(val type: Datatype5, val elemSize: Int, val endian: ByteOrder?) :
    MessageHeader(MessageType.Datatype) {
    open fun unsigned() = false
    open fun endian() = endian?: ByteOrder.LITTLE_ENDIAN
}

open class DatatypeFixed(elemSize: Int, endian: ByteOrder, val unsigned: Boolean) :
    DatatypeMessage(Datatype5.Fixed, elemSize, endian) {
    override fun unsigned() = unsigned
}

class DatatypeFloating(elemSize: Int, endian: ByteOrder) : DatatypeMessage(Datatype5.Floating, elemSize, endian)

class DatatypeTime(elemSize: Int, endian: ByteOrder) : DatatypeMessage(Datatype5.Time, elemSize, endian)

class DatatypeString(elemSize: Int) : DatatypeMessage(Datatype5.String, elemSize, null)

class DatatypeBitField(elemSize: Int, endian: ByteOrder, unsigned: Boolean, val bitOffset : Short,
                       val bitPrecision : Short) : DatatypeFixed(elemSize, endian, unsigned)

class DatatypeOpaque(elemSize: Int, val desc: String) : DatatypeMessage(Datatype5.Opaque, elemSize, null)

class DatatypeCompound(elemSize: Int, val members: List<StructureMember5>) :
    DatatypeMessage(Datatype5.Compound, elemSize, null)

// LOOK is all we have is a mdt, must be a scalar ?? Or could be an array?
class StructureMember5(val name: String, val offset: Int, val mdt: DatatypeMessage)

/**
 * @param elemSize dunno, maybe size of offset in field? maybe ignored?
 * @param referenceType
 *  0) Object Reference: A reference to another object in this HDF5 file.
 *  1) Dataset Region Reference: A reference to a region within a dataset in this HDF5 file.
 */
class DatatypeReference(elemSize: Int, val referenceType: Int) : DatatypeMessage(Datatype5.Reference, elemSize, null)

class DatatypeEnum(
    elemSize: Int,
    val base: DatatypeMessage,
    val names: List<String>,
    nums: List<Int>
) : DatatypeMessage(Datatype5.Enumerated, elemSize, null)

class DatatypeVlen(elemSize: Int, val base: DatatypeMessage, val isVString: Boolean) :
    DatatypeMessage(Datatype5.Vlen, elemSize, null)

class DatatypeArray(elemSize: Int, val base: DatatypeMessage, val dims: IntArray) :
    DatatypeMessage(Datatype5.Array, elemSize, null)

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Throws(IOException::class)
fun H5builder.readDatatypeMessage(state: OpenFileState): DatatypeMessage {
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
            return DatatypeFixed(elemSize, endian, unsigned)
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
            return DatatypeFloating(elemSize, endian)
        }

        2 -> {
            // LOOK no units, worthless, assume its integral
            val bitPrecision = raf.readInt(state)
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            return DatatypeTime(elemSize, endian)
        }

        3 -> {
            // could also store padding type and character set.
            // padding : always check for zero termination.
            // character set, ASCII < UTF8, so always use UTF8
            return DatatypeString(elemSize)
        }

        4 -> {
            val bitOffset = raf.readShort(state)
            val bitPrecision = raf.readShort(state)
            val endian = if (flags0 and 1 == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val unsigned = (flags0 and 8 == 0)
            // LOOK bitOffset, bitPrecision, to support packing ??
            return DatatypeBitField(elemSize, endian, unsigned, bitOffset, bitPrecision)
        }

        5 -> {
            val len = flags0
            val desc = raf.readString(state, len)
            return DatatypeOpaque(elemSize, desc)
        }

        6 -> {
            // compound
            val nmembers: Int = makeUnsignedIntFromBytes(flags1, flags0)
            val members = mutableListOf<StructureMember5>()
            for (i in 0 until nmembers) {
                members.add(this.readStructureMember(state, version, elemSize))
            }
            return DatatypeCompound(elemSize, members)
        }

        7 -> {
            val referenceType = flags0 and 0xf
            return DatatypeReference(elemSize, referenceType)
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

            return DatatypeEnum(elemSize, base, enumNames, enumNums)
        }

        9 -> {
            val isVString = flags0 and 0xf == 1
            val base = this.readDatatypeMessage(state)
            // TODO padding and charset

            return DatatypeVlen(elemSize, base, isVString)
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
            return DatatypeArray(elemSize, base, dim)
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

