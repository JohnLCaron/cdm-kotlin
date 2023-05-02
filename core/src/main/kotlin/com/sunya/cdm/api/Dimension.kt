package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

/**
 * @param length: Long, not Unsigned Long. see "https://github.com/Kotlin/KEEP/blob/master/proposals/unsigned-types.md"
 */
data class Dimension(val orgName : String, val length : Long, val isShared : Boolean) {
    val name = makeValidCdmObjectName(orgName)

    constructor(name : String, len : Int) : this(name, len.toLong(), true)
    constructor(len : Int) : this("", len.toLong(), false)
    constructor(len : Long) : this("", len, false)
}