package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.util.makeValidCdmObjectName
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// fixed length data in the ByteBuffer, var length data goes on the heap
class ArrayStructureData(shape : IntArray, bb : ByteBuffer, val recsize : Int, val members : List<StructureMember<*>>)
        : ArrayTyped<ArrayStructureData.StructureData>(bb, Datatype.COMPOUND, shape) {

    init {
        require(bb.capacity() >= recsize * shape.computeSize())
    }

    fun get(idx : Int) = StructureData(bb, recsize * idx, members)

    override fun iterator(): Iterator<StructureData> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<StructureData>() {
        private var idx = 0
        override fun computeNext() {
            if (idx >= nelems) done()
            else setNext(StructureData(bb, recsize * idx, members))
            idx++
        }
    }

    private val heap = mutableMapOf<Int, Any>()
    private var heapIndex = 0
    fun putOnHeap(offset : Int, value: Any): Int {
        heap[heapIndex] = value
        bb.putInt(offset, heapIndex)
        val result = heapIndex
        heapIndex++
        return result
    }

    fun getFromHeap(offset: Int): Any? {
        val index = bb.getInt(offset) // youve clobbered the byte buffer. is that ok ??
        return heap[index]
    }

    override fun toString(): String {
        return buildString {
            append("ArrayStructureData(nelems=$nelems sizeElem=$recsize, members=$members)\n")
            for (member in this@ArrayStructureData.members) {
                append("${"%12s".format(member.name)}, ")
            }
            append("\n")
            for (sdata in this@ArrayStructureData) {
                append(sdata.memberValues())
                append("\n")
            }
        }
    }

    override fun section(section : Section) : ArrayStructureData {
        return ArrayStructureData(section.shape.toIntArray(), sectionFrom(section), recsize, members)
    }

    inner class StructureData(val bb: ByteBuffer, val offset: Int, val members: List<StructureMember<*>>) {
        override fun toString(): String {
            return buildString {
                append("{")
                members.forEachIndexed { idx, m ->
                    if (idx > 0) append(", ")
                    append("${m.name} = ")
                    val value = m.value(this@StructureData)
                    if (value is String) append("\"$value\"") else append("$value")
                }
                append("}")
            }
        }

        fun memberValues(): String {
            return buildString {
                members.forEachIndexed { idx, m ->
                    if (idx > 0) append(", ")
                    val value = m.value(this@StructureData)
                    when (value) {
                        is ArrayTyped<*> -> append(value.showValues())
                        is String -> append("\"${"%12s".format(value.toString())}\"")
                        else -> append("%12s".format(value.toString()))
                    }
                }
            }
        }

        fun getFromHeap(offset: Int) = this@ArrayStructureData.getFromHeap(offset)
        fun putOnHeap(member : StructureMember<*>, value: Any) = this@ArrayStructureData.putOnHeap(member.offset + this.offset, value)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StructureData) return false

            if (members != other.members) {
                return false
            }
            // check each member's value
            members.forEachIndexed { idx, m ->
                val om = other.members[idx]
                val val1 = m.value(this)
                val val2 = om.value(other)
                if (val1 != val2) {
                    return false
                }
            }
            return true
        }

        override fun hashCode(): Int {
            var result = bb.hashCode()
            result = 31 * result + offset
            result = 31 * result + members.hashCode()
            members.forEach { result = 31 * result + it.value(this).hashCode() } // LOOK probably wrong
            return result
        }
    }
}

fun ArrayStructureData.putStringsOnHeap(lamda : (StructureMember<*>, Int) -> List<String>) {
    members.filter { it.datatype.isVlenString }.forEach { member ->
        this.forEach { sdata ->
            val sval = lamda(member, sdata.offset + member.offset)
            sdata.putOnHeap(member, sval)
        }
    }
}

fun ArrayStructureData.putVlensOnHeap(lamda : (StructureMember<*>, Int) -> ArrayVlen<*>) {
    members.filter { it.datatype == Datatype.VLEN }.forEach { member ->
        // println("member ${member.name}")
        this.forEachIndexed { idx, sdata ->
            // println("sdata $idx")
            val vlen = lamda(member, sdata.offset + member.offset)
            sdata.putOnHeap(member, vlen)
        }
    }
}

// dim lengths here are ints; Hdf4,5 only supports ints.
open class StructureMember<T>(val orgName: String, val datatype : Datatype<T>, val offset: Int, val dims : IntArray, val endian : ByteOrder? = null) {
    val name = makeValidCdmObjectName(orgName)
    val nelems = dims.computeSize()

    // LOOK clumsy Any
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
                Datatype.UBYTE -> ArrayUByte(dims, memberBB)
                Datatype.USHORT -> ArrayUShort(dims, memberBB)
                Datatype.UINT -> ArrayUInt(dims, memberBB)
                Datatype.ULONG -> ArrayULong(dims, memberBB)
                Datatype.FLOAT -> ArrayFloat(dims, memberBB)
                Datatype.DOUBLE -> ArrayDouble(dims, memberBB)
                Datatype.CHAR -> makeStringZ(bb, offset, nelems)
                Datatype.STRING -> {
                    if (datatype.isVlenString) {
                        val ret = sdata.getFromHeap(offset)
                        if (ret == null) "unknown" else ret
                    } else {
                        makeStringZ(bb, offset, nelems)
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
            Datatype.UBYTE -> bb.get(offset).toUByte()
            Datatype.USHORT -> bb.getShort(offset).toUShort()
            Datatype.UINT -> bb.getInt(offset).toUInt()
            Datatype.ULONG -> bb.getLong(offset).toULong()
            Datatype.FLOAT -> bb.getFloat(offset)
            Datatype.DOUBLE -> bb.getDouble(offset)
            Datatype.CHAR -> makeStringZ(bb, offset, nelems)
            Datatype.STRING -> {
                if (datatype.isVlenString) {
                    val ret = sdata.getFromHeap(offset)
                    if (ret == null) "unknown" else (ret as List<String>)[0]
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

    // terminate at a zero
    fun makeStringZ(bb : ByteBuffer, start : Int, maxElems : Int, charset : Charset = StandardCharsets.UTF_8): String {
        var count = 0
        while (count < maxElems && bb[start + count].toInt() != 0) count++
        return String(bb.array(), start, count, charset)
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

}
