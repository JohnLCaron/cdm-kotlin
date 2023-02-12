package com.sunya.cdm.api

import com.sunya.cdm.util.Indent

fun cdl(netcdf : Netcdf) : String {
    val filename = netcdf.location().substringAfterLast('/')
    val name = filename.substringBefore('.')
    return buildString{
        append("netcdf $name {\n")
        append(netcdf.rootGroup().cdl(Indent(2, 1)))
        append("}")
    }
}

fun Group.cdl(indent : Indent = Indent(2)) : String {
    return buildString{
        if (dimensions.isNotEmpty()) {
            append("${indent}dimensions:\n")
            dimensions.sortedBy { it.name }.forEach { append("${it.cdl(indent.incr())}\n") }
        }
        if (variables.isNotEmpty()) {
            append("${indent}variables:\n")
            variables.sortedBy { it.name }. map { append(it.cdl(indent.incr())) }
        }
        if (attributes.isNotEmpty()) {
            append("\n${indent}// group attributes:\n")
            attributes.sortedBy { it.name }.forEach { append("${it.cdl("", indent)}\n") }
        }
        if (groups.isNotEmpty()) {
            groups.sortedBy { it.name }.forEach {
                append("\n${indent}group: ${it.name} {\n")
                append(it.cdl(indent.incr()))
                append("${indent}}\n")
            }
        }
    }
}

fun Dimension.cdl(indent : Indent = Indent(2)) : String {
    return if (isUnlimited) "${indent}$name = UNLIMITED;   // ($length currently)"
    else if (!isShared) "${indent}$length"
    else "${indent}$name = $length;"
}

fun Variable.cdl(indent : Indent = Indent(2)) : String {
    return buildString {
        append("${indent}${dataType.cdlName} $name")
        if (dimensions.isNotEmpty()) {
            append("(")
            dimensions.forEachIndexed { idx, it ->
                if (idx > 0) append(", ")
                if (!it.isShared) append("$length")
                else append(it.name + "=" + it.length)
            }
            append(")")
        }
        append(";")
        if (attributes.isNotEmpty()) {
            append("\n")
            attributes.forEach { append("${it.cdl(name, indent.incr())}\n") }
        } else {
            append("\n")
        }
    }
}

fun Attribute.cdl(varname: String, indent : Indent = Indent(2)) : String {
    return buildString {
        append("${indent}${dataType.cdlName} $varname:$name = ")
        if (values.isEmpty()) {
            append("NIL")
        }
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

////////////////////////////////////////////////////////////

fun cdlStrict(netcdf : Netcdf) : String {
    val filename = netcdf.location().substringAfterLast('/')
    val name = filename.substringBefore('.')
    return buildString{
        append("netcdf $name {\n")
        append(netcdf.rootGroup().cdlStrict(true, Indent(2, 0, 0)))
        append("}")
    }
}

fun Group.cdlStrict(isRoot : Boolean, indent : Indent) : String {
    return buildString{
        if (dimensions.isNotEmpty()) {
            append("${indent}dimensions:\n")
            dimensions.forEach { append("${it.cdlStrict(indent.incrTab())}\n") }
        }
        if (variables.isNotEmpty()) {
            append("${indent}variables:\n")
            variables.map { append(it.cdlStrict(isRoot, indent.incrTab())) }
        }
        if (attributes.isNotEmpty()) {
            val nindent = if (isRoot) indent else indent.incr()
            val text = if (isRoot) "global" else "group"
            append("\n${nindent}// $text attributes:\n")
            val indenta = if (isRoot) indent.incrTab(2) else indent.incr()
            attributes.forEach { append("${it.cdlStrict("", isRoot, indenta)}\n") }
        }
        if (groups.isNotEmpty()) {
            groups.forEach {
                val nindent = indent.incr()
                append("\n${indent}group: ${it.name} {\n")
                append(it.cdlStrict(false, nindent))
                append("${indent}} // group ${it.name}\n")
            }
        }
    }
}

fun Dimension.cdlStrict(indent : Indent = Indent(2)) : String {
    return if (isUnlimited) "${indent}$name = UNLIMITED;   // ($length currently)"
    else "${indent}$name = $length ;"
}

fun Variable.cdlStrict(isRoot : Boolean, indent : Indent = Indent(2)) : String {
    return buildString {
        append("${indent}${dataType.cdlName} $name")
        if (dimensions.isNotEmpty()) {
            append("(")
            dimensions.forEachIndexed { idx, it ->
                if (idx > 0) append(", ")
                append(it.name)
            }
            append(")")
        }
        append(" ;")
        if (attributes.isNotEmpty()) {
            append("\n")
            attributes.forEach { append("${it.cdlStrict(name, isRoot, indent.incrTab())}\n") }
        } else {
            append("\n")
        }
    }
}

fun Attribute.cdlStrict(varname: String, addType : Boolean, indent : Indent = Indent(2)) : String {
    return buildString {
        append("${indent}")
        if (addType || dataType != DataType.STRING) {
            append("${dataType.cdlName} ")
        }
        append("$varname:$name = ")
        if (values.isEmpty()) {
            append("NIL")
        }
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
        append(" ;")
    }
}

/////////////////////
private val org = charArrayOf('\b', '\n', '\r', '\t', '\\', '\'', '\"')
private val replace = arrayOf("\\b", "\\n", "\\r", "\\t", "\\\\", "\\'", "\\\"")

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