package com.sunya.cdm.api

interface Netcdf {
    fun rootGroup() : Group
    fun location() : String
    fun cdl() : String
    fun cdlStrict() : String
}