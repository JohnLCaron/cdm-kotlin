package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

// here, shape must be integers, since size cant exceed 32 bits
abstract class ArrayTyped<T>(val bb : ByteBuffer, val datatype : Datatype, val shape : IntArray) : Iterable<T> {
    val nelems = shape.computeSize()

    override fun toString(): String {
        return buildString {
            append("class ${this@ArrayTyped::class.java.simpleName} shape=${shape.contentToString()} data=")
            append("${showValues()}")
            append("\n")
        }
    }

    fun showValues(): String {
        return buildString {
            val iter = this@ArrayTyped.iterator()
            for (value in iter) {
                append("$value,")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayTyped<*>) return false
        if (datatype != other.datatype) return false
        if (nelems != other.nelems) return false
        if (!shape.equivalent(other.shape)) return false
        return valuesEqual(this, other)
    }

    companion object {
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
                    println("$allcount $v1 != $v2")
                    count++
                }
                allcount++
            }
            return count
        }
    }

    // create a section of this Array. LOOK not checking section against array shape.
    // This makes a copy. might do logical sections in the future.
    abstract fun section(section : Section) : ArrayTyped<T>

    protected fun sectionFrom(section : Section) : ByteBuffer {
        val sectionNelems = section.totalElements.toInt()
        if (sectionNelems == nelems)
            return bb

        val dst = ByteBuffer.allocate(sectionNelems * datatype.size)
        val chunker = Chunker(IndexSpace(this.shape), IndexSpace(section))
        chunker.transfer(bb, datatype.size, dst)

        bb.position(0)
        dst.position(0)

        return dst
    }
}

// An array of any shape that has a single value for all elements, usually the fill value
class ArraySingle<T>(shape : IntArray, datatype : Datatype, val fillValue : T) : ArrayTyped<T>(ByteBuffer.allocate(1), datatype, shape) {
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

    override fun section(section : Section) : ArrayTyped<T> {
        return ArraySingle(section.shape.toIntArray(), datatype, fillValue)
    }
}

// An empty array of any shape that has mo values
class ArrayEmpty<T>(shape : IntArray, datatype : Datatype) : ArrayTyped<T>(ByteBuffer.allocate(1), datatype, shape) {
    override fun iterator(): Iterator<T> = listOf<T>().iterator()
    override fun section(section : Section) : ArrayTyped<T> {
        return this
    }
}


