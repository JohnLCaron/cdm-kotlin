package com.sunya.cdm.api

import com.sunya.cdm.iosp.Iosp
import java.io.Closeable

interface Netcdf : Closeable, Iosp {
    fun location() : String
    fun cdl() : String
    fun type() : String
}