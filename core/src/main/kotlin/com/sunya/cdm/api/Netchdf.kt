package com.sunya.cdm.api

import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.ReadChunkConcurrent
import java.io.Closeable
import java.io.IOException

interface Netchdf : Closeable {
    fun location() : String
    fun type() : String
    val size : Long get() = 0
    fun rootGroup() : Group
    fun cdl() : String

    @Throws(IOException::class, InvalidRangeException::class)
    fun readArrayData(v2: Variable, section: Section? = null) : ArrayTyped<*>

    @Throws(IOException::class, InvalidRangeException::class)
    fun chunkIterator(v2: Variable, section: Section? = null, maxElements : Int? = null) : Iterator<ArraySection>
}

data class ArraySection(val array : ArrayTyped<*>, val section : Section)

// Experimental
fun Netchdf.chunkConcurrent(v2: Variable, section: Section? = null, maxElements : Int? = null, lamda : (ArraySection) -> Unit) {
    val reader = ReadChunkConcurrent()
    val chunkIter = this.chunkIterator( v2, section, maxElements)
    if (chunkIter != null) {
        reader.readChunks(20, chunkIter, lamda)
    }
}