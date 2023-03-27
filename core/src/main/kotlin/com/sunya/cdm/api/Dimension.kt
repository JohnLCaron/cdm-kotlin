package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

data class Dimension(val orgName : String, val length : Int, val isShared : Boolean) {
    val name = makeValidCdmObjectName(orgName)

    constructor(name : String, len : Int) : this(name, len, true)
    constructor(len : Int) : this("", len, false)
}

fun IntArray.computeSize(): Int {
    var product = 1
    this.forEach { product *= it }
    return product
}