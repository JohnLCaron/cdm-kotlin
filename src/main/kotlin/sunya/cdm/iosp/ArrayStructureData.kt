package sunya.cdm.iosp

import sunya.cdm.api.StructureData

class ArrayStructureData(val values : Array<StructureData>, val shape : IntArray) : ArrayTyped<StructureData>() {
    override fun iterator(): Iterator<StructureData> = BufferIterator()
    private inner class BufferIterator : AbstractIterator<StructureData>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.size) done() else setNext(values[idx++])
    }

}