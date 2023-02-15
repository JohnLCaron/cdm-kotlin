package com.sunya.cdm.api

import java.io.Closeable

interface Netcdf : Closeable {
    fun rootGroup() : Group
    fun location() : String
    fun cdl() : String
    fun cdlStrict() : String
}