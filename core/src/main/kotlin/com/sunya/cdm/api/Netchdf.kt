package com.sunya.cdm.api

import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.ReadChunkConcurrent
import java.io.Closeable
import java.io.IOException

interface Netchdf : Closeable {
    fun location() : String
    fun type() : String
    val size : Long get() = 0L
    fun rootGroup() : Group
    fun cdl() : String

    @Throws(IOException::class, InvalidRangeException::class)
    fun <T> readArrayData(v2: Variable<T>, section: SectionPartial? = null) : ArrayTyped<T>

    @Throws(IOException::class, InvalidRangeException::class)
    fun <T> chunkIterator(v2: Variable<T>, section: SectionPartial? = null, maxElements : Int? = null) : Iterator<ArraySection<T>>
}

data class ArraySection<T>(val array : ArrayTyped<T>, val section : Section)

// Experimental
fun <T> Netchdf.chunkConcurrent(v2: Variable<T>, section: SectionPartial? = null, maxElements : Int? = null, lamda : (ArraySection<T>) -> Unit) {
    val reader = ReadChunkConcurrent()
    val chunkIter = this.chunkIterator( v2, section, maxElements)
    reader.readChunks(20, chunkIter, lamda)
}