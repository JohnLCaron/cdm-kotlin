package com.sunya.cdm.iosp

import com.google.common.util.concurrent.AtomicDouble
import com.sunya.cdm.api.InvalidRangeException
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArrayTyped
import java.io.IOException

interface Iosp {
    @Throws(IOException::class, InvalidRangeException::class)
    fun readArrayData(v2: Variable, section: Section? = null) : ArrayTyped<*>

    @Throws(IOException::class, InvalidRangeException::class)
    fun chunkIterator(v2: Variable, section: Section? = null) : Iterator<ArraySection>?
}

data class ArraySection(val array : ArrayTyped<*>, val section : Section)

fun Iosp.chunkConcurrent(v2: Variable, section: Section? = null, lamda : (ArraySection) -> Unit) {
    val reader = ReadChunkConcurrent()
    val chunkIter = this.chunkIterator( v2, section)
    if (chunkIter != null) {
        reader.readChunks(20, chunkIter, lamda)
    }
}