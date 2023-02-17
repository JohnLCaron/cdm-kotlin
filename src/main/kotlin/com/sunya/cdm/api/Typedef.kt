package com.sunya.cdm.api

import com.sunya.cdm.iosp.StructureMember
import com.sunya.cdm.util.Indent

enum class TypedefKind {Compound, Enum, Opaque, Vlen, Unknown}

abstract class Typedef(val kind : TypedefKind, val name : String, val baseType : Datatype) {
    abstract fun cdl(indent : Indent): String
}

class CompoundTypedef(name : String, val members : List<StructureMember>) : Typedef(TypedefKind.Compound, name, Datatype.COMPOUND) {
    override fun cdl(indent : Indent) : String {
        return buildString {
            append("${indent}compound $name {\n")
            val nindent = indent.incr()
            members.forEach {
                append("${nindent}${it.datatype} ${it.name}${showDims(it.dims)} ;\n")
            }
            append("${indent}}; // $name")
        }
    }
}

private fun showDims(dims : IntArray) : String {
    return if (dims.size == 0 || dims.computeSize() == 1) "" else
    buildString {
        append("(")
        dims.forEachIndexed { idx, num ->
            if (idx > 1) append(",")
            append("$num")
            append(")")
        }
    }
}

class EnumTypedef(name : String, baseType : Datatype, val values : Map<Int, String>) : Typedef(TypedefKind.Enum, name, baseType) {
    override fun cdl(indent : Indent) : String {
        return buildString {
            append("${indent}${baseType.strictEnumType().cdlName} enum $name {")
            var idx = 0
            values.forEach {
                if (idx > 0) append(", ")
                append("${it.key} = ${it.value}")
                idx++
            }
            append("};")
        }
    }
}


fun Datatype.strictEnumType() : Datatype {
    return when(this) {
        Datatype.ENUM1 -> Datatype.UBYTE
        Datatype.ENUM2 -> Datatype.USHORT
        Datatype.ENUM4 -> Datatype.UINT
        else -> this
    }
}

class OpaqueTypedef(name : String, val elemSize : Int) : Typedef(TypedefKind.Opaque, name, Datatype.OPAQUE) {
    override fun cdl(indent : Indent): String {
        return "${indent}opaque($elemSize) $name ;"
    }
}

class VlenTypedef(name : String, baseType : Datatype) : Typedef(TypedefKind.Vlen, name, baseType) {
    override fun cdl(indent : Indent) : String {
        return "${indent}${baseType.cdlName}(*) $name ;"
    }
}