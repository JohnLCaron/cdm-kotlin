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

///////////////////////////////////////////////////////////////////////
// All CDM naming convention enforcement are here.
// reservedFullName defines the characters that must be escaped
// when a short name is inserted into a full name
const val reservedFullName = ".\\"

/**
 * backslash escape a string
 *
 * @param x escape this; may be null
 * @param reservedChars these chars get a backslash in front of them
 * @return escaped string
 */
fun backslashEscape(x: String, reservedChars: String = reservedFullName): String {
    var ok = true
    for (pos in 0 until x.length) {
        val c = x[pos]
        if (reservedChars.indexOf(c) >= 0) {
            ok = false
            break
        }
    }
    if (ok) {
        return x
    }

    // gotta do it
    val sb = java.lang.StringBuilder(x)
    var pos = 0
    while (pos < sb.length) {
        val c = sb[pos]
        if (reservedChars.indexOf(c) < 0) {
            pos++
            continue
        }
        sb.setCharAt(pos, '\\')
        pos++
        sb.insert(pos, c)
        pos++
        pos++
    }
    return sb.toString()
}

/**
 * backslash unescape a string
 *
 * @param x unescape this
 * @return string with \c -> c
 */
fun backslashUnescape(x: String): String {
    if (!x.contains("\\")) {
        return x
    }

    // gotta do it
    val sb = java.lang.StringBuilder(x.length)
    var pos = 0
    while (pos < x.length) {
        var c = x[pos]
        if (c == '\\') {
            c = x[++pos] // skip backslash, get next cha
        }
        sb.append(c)
        pos++
    }
    return sb.toString()
}

///////////////////////////////////////////////////////////////

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