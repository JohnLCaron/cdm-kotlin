package com.sunya.cdm.api

data class Dimension(val name : String, val length : Int, val isUnlimited : Boolean, val isShared : Boolean) {

    constructor(name : String, len : Int) : this(name, len, false, true)

    constructor(len : Int) : this("", len, false, false)

}