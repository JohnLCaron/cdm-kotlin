package com.sunya.cdm.iosp

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

// fixed length data in the ByteBuffer, var length data goes on the heap
class ArrayStructureData(val bb : ByteBuffer, val sizeElem : Int, val shape : IntArray, val members : List<StructureMember>)
    : ArrayTyped<ArrayStructureData.StructureData>() {

    val nelems = Section(shape).computeSize().toInt()

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

    private val heap by lazy { mutableMapOf<Int, Any>() }
    private var heapIndex = 0
    fun putHeap(offset : Int, value: Any): Int {
        heap[heapIndex] = value
        bb.putInt(offset, heapIndex)
        val result = heapIndex
        heapIndex++
        return result
    }

    fun getHeap(offset: Int): Any? {
        val index = bb.getInt(offset)
        return heap[index]
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

        fun getHeap(offset: Int) = this@ArrayStructureData.getHeap(offset)
        fun putHeap(offset : Int, value: Any) = this@ArrayStructureData.putHeap(offset, value)
        fun putHeap(member : StructureMember, value: Any) = this@ArrayStructureData.putHeap(member.offset + this.offset, value)
    }
}

open class StructureMember(val name: String, val datatype : Datatype, val offset: Int, val dims : IntArray) {
    val nelems = Section(dims).computeSize().toInt()

    open fun value(sdata: ArrayStructureData.StructureData): Any {
        val bb = sdata.bb
        val offset = sdata.offset + this.offset
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
            Datatype.STRING -> {
                val ret = sdata.getHeap(offset)
                if (ret == null) "unknown" else ret as String
            }
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8)
        }
    }
}
