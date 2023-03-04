package com.sunya.cdm.util

/**
    remove leading and trailing blanks
    remove control characters (< 0x20)
    transform "/", ",", embedded space to "_"
*/
fun makeValidCdmObjectName(orgName: String): String {
    val name = orgName.trim { it <= ' ' }
    // common case no change
    var ok = true
    for (element in name) {
        val c = element.code
        if (c < 0x20) ok = false
        if (c == '/'.code) ok = false
        if (c == ' '.code) ok = false
        if (c == ','.code) ok = false
        if (!ok) break
    }
    if (ok) return name

    return buildString {
        var i = 0
        val len = name.length
        while (i < len) {
            val c = name[i].code
            if (c == '/'.code || c == ' '.code || c == ','.code) append('_') else if (c >= 0x20) append(c.toChar())
            i++
        }
    }
}