package sunya.cdm.api

data class Dimension(val name : String, val length : Int, val isUnlimited : Boolean) {

    constructor(name : String, len : Int) : this(name, len, false)

    fun cdlString() : String {
        return if (isUnlimited) "  $name = UNLIMITED;   // ($length currently)"
        else "  $name = $length;"
    }

}