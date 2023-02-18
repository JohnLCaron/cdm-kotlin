package com.sunya.cdm.iosp

import com.sunya.cdm.api.Datatype

class ArrayVlen(shape : IntArray, val values : List<Array<*>>, val baseType : Datatype) : ArrayTyped<Any>(shape) {

    // iterate over all the values, needed eg for toList()
    override fun iterator(): Iterator<Any> = AllIterator()
    private inner class AllIterator : AbstractIterator<Any>() {
        private var idx = 0
        private var currentValue : Array<*>? = null
        private var currentIterator : Iterator<*>? = null
        override fun computeNext() {
            if (currentValue == null) {
                if (idx >= values.size) return done()
                currentValue = values[idx++]
                currentIterator = currentValue!!.iterator()
            }
            if (currentIterator!!.hasNext()) {
                setNext(currentIterator!!.next()!!)
            } else {
                currentValue = null
                return computeNext()
            }
        }
    }
}