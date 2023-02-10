package com.sunya.cdm.api

// could use Array<*>
data class Attribute(val name : String, val dataType : DataType, val values : List<*>) {

    constructor(name : String, svalue : String) : this(name, DataType.STRING, List<String>(1) { svalue})

    val isString = (dataType == DataType.STRING)

    fun cdlString(indent : Indent = Indent(2)) : String {
        return buildString {
            append("${indent}:$name = ")
            values.forEachIndexed { idx, it ->
                if (idx != 0) {
                    append(", ")
                }
                when (dataType) {
                    DataType.STRING -> append("\"${escapeCdl(it as String)}\"")
                    DataType.FLOAT -> append("${it}f")
                    DataType.SHORT -> append("${it}s")
                    DataType.BYTE -> append("${it}b")
                    else -> append("$it")
                }
            }
            append(";")
        }
    }

    class Builder {
        var name : String? = null
        var dataType : DataType? = null
        var values :List<*>? = null

        fun setName(name : String) : Attribute.Builder {
            this.name = name
            return this
        }

        fun setDataType(type : DataType) : Attribute.Builder {
            this.dataType = type
            return this
        }

        fun build() : Attribute {
            if (dataType == DataType.CHAR && values == null) {
                values = listOf("") // special case to match c library
            }
            val useType = if (dataType == DataType.CHAR) DataType.STRING else dataType
            return Attribute(name!!, useType!!, values ?: emptyList<Any>())
        }
    }
}

private val org = charArrayOf('\b', '\n', '\r', '\t', '\\', '\'', '\"')
private val replace = arrayOf("\\b", "\\n", "\\r", "\\t", "\\\\", "\\'", "\\\"")

private fun escapeCdl(s: String): String {
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