package com.sunya.cdm.array

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section.Companion.computeSize

abstract class ArrayTyped<T>(val shape : IntArray) : Iterable<T> {

    override fun toString(): String {
        return buildString {
            append("class ${this@ArrayTyped::class.java.simpleName} shape=${shape.contentToString()} data=")
            val iter = this@ArrayTyped.iterator()
            for (value in iter) {
                append("$value,")
            }
            append("\n")
        }
    }

    companion object {
        fun contentEquals(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Boolean {
            if (!array1.shape.contentEquals(array2.shape)) {
                return false
            }
            return valuesEqual(array1, array2)
        }

        fun valuesEqual(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Boolean {
            val iter1 = array1.iterator()
            val iter2 = array2.iterator()
            var count = 0
            while (iter1.hasNext() && iter2.hasNext()) {
                val v1 = iter1.next()
                val v2 = iter2.next()
                count++
                if (v1 != v2) {
                    val ok = (v1 == v2)
                    return false
                }
            }
            return true
        }

        fun countDiff(array1 : ArrayTyped<*>, array2 : ArrayTyped<*>) : Int {
            val iter1 = array1.iterator()
            val iter2 = array2.iterator()
            var count = 0
            var count1 = 0
            var count2 = 0
            val fill = (-1).toShort()
            while (iter1.hasNext() && iter2.hasNext()) {
                val v1 = iter1.next()
                val v2 = iter2.next()
                if (v1 != v2) {
                    count++
                }
                if ((v1 as Short) == fill) count1++
                if ((v2 as Short) == fill) count2++
            }
            println("count -1 in array1 = $count1")
            println("count -1 in array2 = $count2")
            return count
        }
    }
}

// An array of any shape that has a single value for all elements
class ArraySingle<T>(shape : IntArray, val datatype : Datatype, val fillValue : T) : ArrayTyped<T>(shape) {
    val nelems = computeSize(shape)
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
