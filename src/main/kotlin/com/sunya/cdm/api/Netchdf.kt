package com.sunya.cdm.api

import com.sunya.cdm.iosp.Iosp
import java.io.Closeable

interface Netchdf : Closeable, Iosp {
    fun location() : String
    fun type() : String
    val size : Long get() = 0
    fun rootGroup() : Group
    fun cdl() : String
}