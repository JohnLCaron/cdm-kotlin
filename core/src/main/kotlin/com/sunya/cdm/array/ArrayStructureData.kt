package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

// fixed length data in the ByteBuffer, var length data goes on the heap
class ArrayStructureData(shape : IntArray, bb : ByteBuffer, val recsize : Int, val members : List<StructureMember<*>>)
        : ArrayTyped<ArrayStructureData.StructureData>(bb, Datatype.COMPOUND, shape) {

    init {
        require(bb.capacity() >= recsize * shape.computeSize())
    }

    fun get(idx: Int) = StructureData(bb, recsize * idx, members)

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
    fun putOnHeap(offset: Int, value: Any): Int {
        heap[heapIndex] = value
        bb.putInt(offset, heapIndex)
        val result = heapIndex
        heapIndex++
        return result
    }

    // not a bad ide to start at 1, so that 0 == not set
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

    override fun section(section: Section): ArrayStructureData {
        require( IndexSpace(shape).contains(IndexSpace(section))) {"Variable does not contain requested section"}
        val sectionNelems = section.totalElements.toInt()
        if (sectionNelems == nelems)
            return this

        // copy the requested records
        val sectionBB = ByteBuffer.allocate(sectionNelems * recsize)
        val chunker = Chunker(IndexSpace(this.shape), IndexSpace(section))
        chunker.transfer(bb, recsize, sectionBB)
        bb.position(0)
        sectionBB.position(0)

        return ArrayStructureData(section.shape.toIntArray(), sectionBB, recsize, members)
    }

    inner class StructureData(val bb: ByteBuffer, val offset: Int, val members: List<StructureMember<*>>) {
        override fun toString(): String {
            return buildString {
                append("{")
                members.forEachIndexed { idx, m ->
                    if (idx > 0) append(", ")
                    append("${m.name} = ")
                    val value = m.value(this@StructureData)
                    when (value) {
                        is String -> append("\"$value\"")
                        is ArrayTyped<*> -> append("[${value.showValues()}]")
                        else -> append("$value")
                    }
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
                        is ArrayTyped<*> -> append("[${value.showValues()}]")
                        is String -> append("\"${"%12s".format(value.toString())}\"")
                        else -> append("%12s".format(value.toString()))
                    }
                }
            }
        }

        fun getFromHeap(offset: Int) = this@ArrayStructureData.getFromHeap(offset)
        fun putOnHeap(member: StructureMember<*>, value: Any) =
            this@ArrayStructureData.putOnHeap(member.offset + this.offset, value)

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

    fun putStringsOnHeap(lamda: (StructureMember<*>, Int) -> List<String>) {
        members.filter { it.datatype.isVlenString }.forEach { member ->
            this.forEach { sdata ->
                val sval = lamda(member, sdata.offset + member.offset)
                sdata.putOnHeap(member, sval)
            }
        }
    }

    fun putVlensOnHeap(lamda: (StructureMember<*>, Int) -> ArrayVlen<*>) {
        members.filter { it.datatype == Datatype.VLEN }.forEach { member ->
            // println("member ${member.name}")
            this.forEachIndexed { idx, sdata ->
                // println("sdata $idx")
                val vlen = lamda(member, sdata.offset + member.offset)
                sdata.putOnHeap(member, vlen)
            }
        }
    }
}
