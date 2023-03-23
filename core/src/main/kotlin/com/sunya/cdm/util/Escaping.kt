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

/////////////////////
private val org = charArrayOf('\b', '\n', '\r', '\t', '\\', '\'', '\"')
private val replace = arrayOf("\\b", "\\n", "\\r", "\\t", "\\\\", "\\'", "\\\"")

fun escapeName(s: String): String {
    return replace(s, org, replace).replace(" ", "_")
}

fun escapeCdl(s: String): String {
    return replace(s, org, replace)
}

fun replace(x: String, replaceChar: CharArray, replaceWith: Array<String>): String {
    // common case no replacement
    var ok = true
    for (aReplaceChar in replaceChar) {
        val pos = x.indexOf(aReplaceChar)
        ok = pos < 0
        if (!ok) break
    }
    if (ok) return x

    // gotta do it
    val sb = StringBuilder(x)
    for (i in replaceChar.indices) {
        val pos = x.indexOf(replaceChar[i])
        if (pos >= 0) {
            replace(sb, replaceChar[i], replaceWith[i])
        }
    }
    return sb.toString()
}

fun replace(sb: StringBuilder, remove: Char, replaceWith: String) {
    var i = 0
    while (i < sb.length) {
        if (sb[i] == remove) {
            sb.replace(i, i + 1, replaceWith)
            i += replaceWith.length - 1
        }
        i++
    }
}