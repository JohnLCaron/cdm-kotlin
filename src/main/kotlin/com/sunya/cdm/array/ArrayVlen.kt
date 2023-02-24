package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype

class ArrayVlen(shape : IntArray, val values : List<Array<*>>, val baseType : Datatype) : ArrayTyped<Any>(shape) {

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
    //override fun toString(): String {
    //    return "ArrayVlen(baseType=$baseType)"
    //}

}