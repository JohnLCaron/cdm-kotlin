package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// fixed length data in the ByteBuffer, var length data goes on the heap
class ArrayStructureData(shape : IntArray, val bb : ByteBuffer, val sizeElem : Int, val members : List<StructureMember>)
    : ArrayTyped<ArrayStructureData.StructureData>(shape) {

    fun get(idx : Int) = StructureData(bb, sizeElem * idx, members)

    override fun iterator(): Iterator<StructureData> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<StructureData>() {
        private var idx = 0
        override fun computeNext() {
            if (idx >= nelems) done()
            else setNext(StructureData(bb, sizeElem * idx, members))
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
            append("ArrayStructureData(nelems=$nelems sizeElem=$sizeElem, members=$members)\n")
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

    inner class StructureData(val bb: ByteBuffer, val offset: Int, val members: List<StructureMember>) {
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
                    if (value is ArrayTyped<*>) append(value.values())
                    else if (value is String) append("\"${"%12s".format(value.toString())}\"")
                    else append("%12s".format(value.toString()))
                }
            }
        }

        fun getFromHeap(offset: Int) = this@ArrayStructureData.getFromHeap(offset)
        fun putOnHeap(member : StructureMember, value: Any) = this@ArrayStructureData.putOnHeap(member.offset + this.offset, value)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StructureData) return false

            if (members != other.members) {
                return false
            }
            // check each member's value
            members.forEachIndexed { idx, m ->
                val om = other.members[idx]
                if (m.value(this) != om.value(other)) {
                    val v1 = m.value(this)
                    val v2 = om.value(other)
                    val ok = v1.equals(v2)
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

fun ArrayStructureData.putStringsOnHeap(lamda : (Int) -> String) {
    members.filter { it.datatype == Datatype.STRING }.forEach { member ->
        this.forEach { sdata ->
            val sval = lamda(sdata.offset + member.offset)
            sdata.putOnHeap(member, sval)
        }
    }
}

fun ArrayStructureData.putVlensOnHeap(lamda : (StructureMember, Int) -> ArrayVlen) {
    members.filter { it.datatype == Datatype.VLEN }.forEach { member ->
        // println("member ${member.name}")
        this.forEachIndexed { idx, sdata ->
            // println("sdata $idx")
            val vlen = lamda(member, sdata.offset + member.offset)
            sdata.putOnHeap(member, vlen)
        }
    }
}

open class StructureMember(val name: String, val datatype : Datatype, val offset: Int, val dims : IntArray) {
    val nelems = dims.computeSize()

    open fun value(sdata: ArrayStructureData.StructureData): Any {
        val bb = sdata.bb
        val offset = sdata.offset + this.offset
        if (nelems > 1) {
            val memberBB = ByteBuffer.allocate(nelems * datatype.size)
            repeat(nelems * datatype.size) { memberBB.put(it, sdata.bb.get(offset + it)) }
            return when (datatype) {
                Datatype.BYTE -> ByteArray(nelems) { bb.get(offset + it) }
                Datatype.SHORT -> ShortArray(nelems) { bb.getShort(offset + it * 2) }
                Datatype.INT -> ArrayInt(dims, memberBB.asIntBuffer())
                Datatype.LONG -> bb.getLong(offset)
                Datatype.UBYTE -> bb.get(offset).toUByte()
                Datatype.USHORT -> bb.getShort(offset).toUShort()
                Datatype.UINT -> bb.getInt(offset).toUInt()
                Datatype.ULONG -> bb.getLong(offset).toULong()
                Datatype.FLOAT -> bb.getFloat(offset)
                Datatype.DOUBLE -> bb.getDouble(offset)
                Datatype.CHAR -> makeStringZ(bb, offset, nelems)
                Datatype.STRING -> {
                    val ret = sdata.getFromHeap(offset)
                    if (ret == null) "unknown" else ret as String
                }
                else -> throw RuntimeException("unimplemented array of $datatype")
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
                val ret = sdata.getFromHeap(offset)
                if (ret == null) "unknown" else ret as String
            }
            Datatype.VLEN -> {
                val ret = sdata.getFromHeap(offset)
                if (ret != null) (ret as ArrayVlen) else {
                    sdata.getFromHeap(offset)
                    throw RuntimeException("cant find ArrayVlen on heap at $offset")
                }
            }
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8)
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
        if (other !is StructureMember) return false

        if (name != other.name) return false
        if (datatype != other.datatype) return false
        if (!dims.contentEquals(other.dims)) return false
        if (nelems != other.nelems) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + datatype.hashCode()
        result = 31 * result + dims.contentHashCode()
        result = 31 * result + nelems
        return result
    }
}
