package com.sunya.cdm.api

import org.junit.jupiter.api.Test

class TestGenerics {

    @Test
    fun testGetGenericData() {
        println("${Datatype.BYTE} ${getData(Datatype.BYTE)}")
        println("${Datatype.INT}  ${getData(Datatype.INT)}")
        println("${Datatype.UINT}  ${getData(Datatype.UINT)}")
    }

    fun <T> getData(datatype: Datatype<T>): List<T>? {
        return when (datatype) {
            Datatype.BYTE -> listOf(1.toByte(), 2.toByte()) as List<T>
            Datatype.INT -> listOf(1, 2) as List<T>
            else -> null
        }
    }
}