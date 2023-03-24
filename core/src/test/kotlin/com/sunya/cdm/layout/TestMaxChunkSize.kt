package com.sunya.cdm.layout

import com.sunya.cdm.api.computeSize
import org.junit.jupiter.api.Test

class TestMaxChunkSize {

    @Test
    fun testMaxChunkShape() {
        testMaxChunkShape(intArrayOf(20, 30, 40), 10)
        testMaxChunkShape(intArrayOf(20, 30, 40), 20)
        testMaxChunkShape(intArrayOf(20, 30, 40), 40)
        testMaxChunkShape(intArrayOf(20, 30, 40), 100)
        testMaxChunkShape(intArrayOf(20, 30, 40), 900)
        testMaxChunkShape(intArrayOf(20, 30, 40), 1200)
        testMaxChunkShape(intArrayOf(20, 30, 40), 2000)
        testMaxChunkShape(intArrayOf(20, 30, 40), 2010)
        testMaxChunkShape(intArrayOf(20, 30, 40), 20000)
        testMaxChunkShape(intArrayOf(20, 30, 40), 23999)
        testMaxChunkShape(intArrayOf(20, 30, 40), 24000)
        testMaxChunkShape(intArrayOf(20, 30, 40), 30000)
    }

    fun testMaxChunkShape(shape : IntArray, max : Int) {
        val chunk = maxChunkShape(shape, max)
        println("shape=${shape.contentToString()} max=$max nelems=${chunk.computeSize()} chunk=${chunk.contentToString()} ")
    }

}