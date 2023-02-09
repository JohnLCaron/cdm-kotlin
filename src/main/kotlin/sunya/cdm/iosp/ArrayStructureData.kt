package sunya.cdm.iosp

import sunya.cdm.api.Section
import sunya.cdm.api.StructureData
import sunya.cdm.api.StructureMembers
import java.nio.ByteBuffer

class ArrayStructureData(val bb : ByteBuffer, val sizeElem : Int, val shape : IntArray, val members : StructureMembers) : ArrayTyped<StructureData>() {
    val nelems = Section(shape).computeSize().toInt()
    override fun iterator(): Iterator<StructureData> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<StructureData>() {
        private var idx = 0
        override fun computeNext() {
            if (idx >= nelems) done()
            else setNext(StructureData(bb, sizeElem * idx, members))
            idx++
        }
    }
}