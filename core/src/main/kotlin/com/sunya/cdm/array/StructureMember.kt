package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.computeSize
import com.sunya.cdm.util.makeValidCdmObjectName
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// dim lengths here are ints; Hdf4,5 only supports ints.
/**
 * @param offset byte offset into the ByteBuffer
 * @param endian only needed if different from the ByteBuffer
 */
open class StructureMember<T>(val orgName: String, val datatype : Datatype<T>, val offset: Int, val dims : IntArray, val endian : ByteOrder? = null) {
    val name = makeValidCdmObjectName(orgName)
    val nelems = dims.computeSize()

    /**
     * Get the value of this member from the given StructureData.
     * return T for nelems = 1, ArrayTyped<T> for nelems > 1
     */
    open fun value(sdata: ArrayStructureData.StructureData): Any {
        val bb = sdata.bb
        bb.order(this.endian ?: sdata.bb.order())
        val offset = sdata.offset + this.offset

        if (nelems > 1) {
            val memberBB = ByteBuffer.allocate(nelems * datatype.size) // why cant we use a view ??
            memberBB.order(this.endian ?: sdata.bb.order())
            repeat(nelems * datatype.size) { memberBB.put(it, sdata.bb.get(offset + it)) }
            return when (datatype) {
                Datatype.BYTE -> ArrayByte(dims, memberBB)
                Datatype.SHORT -> ArrayShort(dims, memberBB)
                Datatype.INT -> ArrayInt(dims, memberBB)
                Datatype.LONG -> ArrayLong(dims, memberBB)
                Datatype.UBYTE, Datatype.ENUM1  -> ArrayUByte(dims, memberBB)
                Datatype.USHORT, Datatype.ENUM2  -> ArrayUShort(dims, memberBB)
                Datatype.UINT, Datatype.ENUM4  -> ArrayUInt(dims, memberBB)
                Datatype.ULONG -> ArrayULong(dims, memberBB)
                Datatype.FLOAT -> ArrayFloat(dims, memberBB)
                Datatype.DOUBLE -> ArrayDouble(dims, memberBB)
                // TODO 3 kinds of Strings: CHAR, STRING, STRING.isVlenString
                //   CHAR often ubyte, or fixed String
                //   perhaps CHAR, STRING, STRING_FIXED ??
                Datatype.CHAR -> makeStringZ(bb, offset, nelems)
                Datatype.STRING -> {
                    if (datatype.isVlenString) {
                        val ret = sdata.getFromHeap(offset)
                        if (ret == null) "unknown" else ret
                    } else {
                        makeStringZ(bb, offset, nelems)
                    }
                }
                Datatype.VLEN -> {
                    val ret = sdata.getFromHeap(offset)
                    if (ret != null) (ret as ArrayVlen<*>) else {
                        throw RuntimeException("cant find ArrayVlen on heap at $offset")
                    }
                }
                else -> throw RuntimeException("unimplemented datatype=$datatype")
            }
        }

        return when (datatype) {
            Datatype.BYTE -> bb.get(offset)
            Datatype.SHORT -> bb.getShort(offset)
            Datatype.INT -> bb.getInt(offset)
            Datatype.LONG -> bb.getLong(offset)
            Datatype.UBYTE, Datatype.ENUM1 -> bb.get(offset).toUByte()
            Datatype.USHORT, Datatype.ENUM2 -> bb.getShort(offset).toUShort()
            Datatype.UINT, Datatype.ENUM4 -> bb.getInt(offset).toUInt()
            Datatype.ULONG -> bb.getLong(offset).toULong()
            Datatype.FLOAT -> bb.getFloat(offset)
            Datatype.DOUBLE -> bb.getDouble(offset)
            Datatype.CHAR -> makeStringZ(bb, offset, nelems)
            Datatype.STRING -> {
                if (datatype.isVlenString) {
                    val ret = sdata.getFromHeap(offset)
                    if (ret == null) "unknown" else ret
                } else {
                    makeStringZ(bb, offset, nelems)
                }
            }
            Datatype.VLEN -> {
                val ret = sdata.getFromHeap(offset)
                if (ret != null) (ret as ArrayVlen<*>) else {
                    throw RuntimeException("cant find ArrayVlen on heap at $offset")
                }
            }
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8) // wtf?
        }
    }

    override fun toString(): String {
        return "\nStructureMember(name='$name', datatype=$datatype, offset=$offset, dims=${dims.contentToString()}, nelems=$nelems)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StructureMember<*>) return false

        if (name != other.name) return false
        if (datatype != other.datatype) return false
        if (!dims.contentEquals(other.dims)) return false
        return nelems == other.nelems
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + datatype.hashCode()
        result = 31 * result + dims.contentHashCode()
        result = 31 * result + nelems
        return result
    }

    companion object {
       fun datatypes() = listOf(Datatype.BYTE, Datatype.UBYTE, Datatype.SHORT, Datatype.USHORT, Datatype.INT,
           Datatype.UINT, Datatype.LONG, Datatype.ULONG, Datatype.DOUBLE, Datatype.FLOAT, Datatype.ENUM1,
           Datatype.ENUM2, Datatype.ENUM4, Datatype.CHAR, Datatype.STRING,
           // Datatype.OPAQUE, Datatype.COMPOUND, Datatype.VLEN, Datatype.REFERENCE
        )
    }
}

/* read a String from ByteBuffer, starting from offset, up to maxBytes, terminate at a zero byte. */
fun makeStringZ(bb : ByteBuffer, offset : Int, maxBytes : Int, charset : Charset = StandardCharsets.UTF_8): String {
    var count = 0
    while (count < maxBytes && bb[offset + count] != 0.toByte()) count++
    return String(bb.array(), offset, count, charset)
}