package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.toIntArray
import com.sunya.cdm.api.toLongArray
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

class ArrayVlen(shape : IntArray, val values : List<Array<*>>, val baseType : Datatype) : ArrayTyped<Any>(ByteBuffer.allocate(1), Datatype.VLEN, shape) {

    // iterate over all the values, needed eg for toList()
    override fun iterator(): Iterator<Any> = AllIterator()
    private inner class AllIterator : AbstractIterator<Any>() {
        private var idx = 0
        private var currentIterator : Iterator<Any>? = null
        override fun computeNext() {
            if (currentIterator == null) {
                if (idx >= values.size) return done()
                currentIterator = values[idx++].iterator() as Iterator<Any> // LOOK
            }
            if (currentIterator!!.hasNext()) {
                setNext(currentIterator!!.next())
            } else {
                currentIterator = null
                return computeNext()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayVlen) return false

        if (baseType != other.baseType) return false

        values.zip(other.values).forEach {pair ->
            if (!pair.component1().contentEquals(pair.component2())) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + baseType.hashCode()
        return result
    }

    override fun section(section: Section): ArrayVlen {
        val odo = IndexND(IndexSpace(section), this.shape.toLongArray())
        val sectionList = mutableListOf<Array<*>>()
        for (index in odo) {
            sectionList.add(values[odo.element().toInt()])
        }
        return ArrayVlen(section.shape.toIntArray(), sectionList, baseType)
    }
}