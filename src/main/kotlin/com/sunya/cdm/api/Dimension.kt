package com.sunya.cdm.api

data class Dimension(val name : String, val length : Int, val isUnlimited : Boolean) {

    constructor(name : String, len : Int) : this(name, len, false)

    fun cdlString(indent : Indent = Indent(2)) : String {
        return if (isUnlimited) "${indent}$name = UNLIMITED;   // ($length currently)"
        else "${indent}$name = $length;"
    }

}