package com.sunya.cdm.api

enum class TypedefKind {Compound, Enum, Opaque, Vlen, Unknown}

open class Typedef(val name : String, val kind : TypedefKind)

class CompoundTypedef(name : String, val dims : IntArray) : Typedef(name, TypedefKind.Compound)

class EnumTypedef(name : String, val baseType : Datatype, val values : Map<Int, String>) : Typedef(name, TypedefKind.Enum) {
    override fun toString() : String {
        return buildString {
            append("${baseType.strictEnumType().cdlName} enum $name {")
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

class OpaqueTypedef(name : String, val dims : IntArray) : Typedef(name, TypedefKind.Opaque)

class VlenTypedef(name : String, val baseType : Datatype) : Typedef(name, TypedefKind.Vlen) {
    override fun toString() : String {
        return "${baseType.cdlName}(*) $name ;"
    }
}