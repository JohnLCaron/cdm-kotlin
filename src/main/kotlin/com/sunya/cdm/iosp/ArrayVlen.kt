package com.sunya.cdm.iosp

import com.sunya.cdm.api.Datatype

class ArrayVlen<T>(shape : IntArray, val values : List<Iterator<T>>, val baseType : Datatype) : ArrayTyped<T>(shape) {

    init {
        println("HEY")
    }

    // iterate over all the values, needed eg for toList()
    override fun iterator(): Iterator<T> = AllIterator()

    private inner class AllIterator : AbstractIterator<T>() {
        private var idx = 0
        private var currentIterator : Iterator<T>? = null
        override fun computeNext() {
            if (currentIterator == null) {
                if (idx >= values.size) return done()
                currentIterator = values[idx++]
            }
            if (currentIterator!!.hasNext()) {
                setNext(currentIterator!!.next()!!)
            } else {
                currentIterator = null
                return computeNext()
            }
        }
    }
    override fun toString(): String {
        return "ArrayVlen(baseType=$baseType)"
    }

}