package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.api.Section.Companion.equivalent

abstract class ArrayTyped<T>(val shape : IntArray) : Iterable<T> {
    val nelems = computeSize(shape).toInt()

    override fun toString(): String {
        return buildString {
            append("class ${this@ArrayTyped::class.java.simpleName} shape=${shape.contentToString()} data=")
            append("${values()}")
            append("\n")
        }
    }

    fun values(): String {
        return buildString {
            val iter = this@ArrayTyped.iterator()
            for (value in iter) {
                append("$value,")
            }
        }
    }

    companion object {
        fun contentEquals(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Boolean {
            if (!array1.shape.equivalent(array2.shape)) {
                return false
            }
            return valuesEqual(array1, array2)
        }

        fun valuesEqual(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Boolean {
            val iter1 = array1.iterator()
            val iter2 = array2.iterator()
            while (iter1.hasNext() && iter2.hasNext()) {
                val v1 = iter1.next()
                val v2 = iter2.next()
                if (v1 != v2) {
                    return false
                }
            }
            return true
        }

        fun countDiff(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Int {
            val iter1 = array1.iterator()
            val iter2 = array2.iterator()
            var allcount = 0
            var count = 0
            while (iter1.hasNext() && iter2.hasNext()) {
                val v1 = iter1.next()
                val v2 = iter2.next()
                if (v1 != v2) {
                    // println("$allcount $v1 != $v2")
                    count++
                }
                allcount++
            }
            return count
        }
    }
}

// An array of any shape that has a single value for all elements
class ArraySingle<T>(shape : IntArray, val datatype : Datatype, val fillValue : T) : ArrayTyped<T>(shape) {
    override fun iterator(): Iterator<T> = SingleIterator()
    private inner class SingleIterator : AbstractIterator<T>() {
        private var idx = 0
        override fun computeNext() = if (idx++ >= nelems) done() else setNext(fillValue)
    }

    override fun toString(): String {
        return buildString {
            append("ArraySingle shape=${shape.contentToString()} data= $fillValue\n")
        }
    }
}
