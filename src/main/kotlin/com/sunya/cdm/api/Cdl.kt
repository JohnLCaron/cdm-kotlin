package com.sunya.cdm.api

import com.sunya.cdm.util.Indent
import java.nio.ByteBuffer
import java.util.*

/** strict = make cdl agree with clib if possible. also sort, so its easier to compare equAL */
fun cdl(netcdf : Netcdf) : String {
    val filename = netcdf.location().substringAfterLast('/')
    val name = filename.substringBefore('.')
    return buildString{
        append("netcdf $name {\n")
        append(netcdf.rootGroup().cdl(true, Indent(2, 1)))
        append("}")
    }
}

fun Group.cdl(isRoot : Boolean, indent : Indent = Indent(2)) : String {
    return buildString{
        if (typedefs.isNotEmpty()) {
            append("${indent}types:\n")
            typedefs.sortedBy { it.name }.forEach { append("${it.cdl(indent.incr())}\n") }
        }
        if (dimensions.isNotEmpty()) {
            append("${indent}dimensions:\n")
            dimensions.sortedBy { it.name }.forEach { append("${it.cdl(indent.incr())}\n") }
        }
        if (variables.isNotEmpty()) {
            append("${indent}variables:\n")
            variables.sortedBy { it.name }.map { append(it.cdl(indent.incr())) }
        }
        if (attributes.isNotEmpty()) {
            val nindent = if (isRoot) indent else indent.incr()
            val text = if (isRoot) "global" else "group"
            append("\n${nindent}// $text attributes:\n")
            attributes.sortedBy { it.name }.forEach { append("${it.cdl("", indent)}\n") }
        }
        if (groups.isNotEmpty()) {
            groups.sortedBy { it.name }.forEach {
                append("\n${indent}group: ${it.name} {\n")
                append(it.cdl(false, indent.incr()))
                append("${indent}}\n")
            }
        }
    }
}

fun Dimension.cdl(indent : Indent = Indent(2)) : String {
    return if (isUnlimited) "${indent}$name = UNLIMITED ; // ($length currently)"
    else if (!isShared) "${indent}$length"
    else "${indent}$name = $length ;"
}

fun Variable.cdl(indent : Indent = Indent(2)) : String {
    val typedef = datatype.typedef
    val typename = if (typedef != null) typedef.name else datatype.cdlName
    return buildString {
        append("${indent}${typename} ${escapeName(name)}")
        if (dimensions.isNotEmpty()) {
            append("(")
            dimensions.forEachIndexed { idx, it ->
                if (idx > 0) append(", ")
                if (!it.isShared) append("${it.length}") else append(it.name)
            }
            append(")")
        }
        append(" ;")
        if (attributes.isNotEmpty()) {
            append("\n")
            attributes.sortedBy { it.name }.forEach { append("${it.cdl(escapeName(name), indent.incr())}\n") }
        } else {
            append("\n")
        }
    }
}

fun Attribute.cdl(varname: String, indent : Indent = Indent(2)) : String {
    val typedef = datatype.typedef
    val typename = if (typedef != null) typedef.name else "" // datatype.cdlName
    val valueDatatype = if (typedef != null) typedef.baseType else datatype
    return buildString {
        append("${indent}${typename} $varname:$name = ")
        if (values.isEmpty()) {
            append("NIL")
        }
        if (datatype == Datatype.OPAQUE) {
            append("${(values[0] as ByteBuffer).toHex()}")
        } else {
            values.forEachIndexed { idx, it ->
                if (idx != 0) {
                    append(", ")
                }
                when (valueDatatype) {
                    Datatype.STRING -> append("\"${escapeCdl(it as String)}\"")
                    Datatype.FLOAT -> append("${it}f")
                    Datatype.SHORT -> append("${it}s")
                    Datatype.BYTE -> append("${it}b")
                    else -> append("$it")
                }
            }
        }
        append(" ;")
    }
}

internal fun ByteBuffer.toHex() : String {
    return "0X" + HexFormat.of().withUpperCase().formatHex(this.array())
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