package com.sunya.cdm.api

import com.sunya.cdm.array.ArrayString
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.array.StructureMember
import com.sunya.cdm.util.Indent
import com.sunya.cdm.util.makeValidCdmObjectName

enum class TypedefKind {Compound, Enum, Opaque, Vlen, Unknown}

abstract class Typedef(val kind : TypedefKind, orgName : String, val baseType : Datatype) {
    val name = makeValidCdmObjectName(orgName)

    abstract fun cdl(indent : Indent): String
}

class CompoundTypedef(name : String, val members : List<StructureMember>) : Typedef(TypedefKind.Compound, name, Datatype.COMPOUND) {
    override fun cdl(indent : Indent) : String {
        return buildString {
            append("${indent}compound $name {\n")
            val nindent = indent.incr()
            members.forEach {
                val typename = if (it.datatype.typedef != null) it.datatype.typedef.name  else it.datatype.cdlName
                append("${nindent}${typename} ${it.name}${showDims(it.dims)} ;\n")
            }
            append("${indent}}; // $name")
        }
    }
}

private fun showDims(dims : IntArray) : String {
    return if (dims.isEmpty() || dims.computeSize() == 1) "" else
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

    /** Convert array of ENUM into equivalent array of String */
    fun ArrayTyped<*>.convertEnums(): ArrayString {
        return this.convertEnums(values)
    }
}

/** Convert array of ENUM into equivalent array of String */
fun ArrayTyped<*>.convertEnums(map: Map<Int, String>): ArrayString {
    val size = Section.computeSize(this.shape).toInt()
    val enumIter = this.iterator()
    val stringValues = List(size) {
        val enumVal = enumIter.next()
        val num = when (enumVal) {
            is UByte ->  enumVal.toInt()
            is UShort ->  enumVal.toInt()
            is UInt ->  enumVal.toInt()
            else -> RuntimeException("unknown enum ${enumVal!!::class}")
        }
        map[num] ?: "Unknown enum number=$enumVal"
    }
    return ArrayString(this.shape, stringValues)
}

fun Datatype.strictEnumType() : Datatype {
    return when(this) {
        Datatype.ENUM1 -> Datatype.UBYTE
        Datatype.ENUM2 -> Datatype.USHORT
        Datatype.ENUM4 -> Datatype.UINT
        else -> this
    }
}

// dont really need a typedef, no extra information
class OpaqueTypedef(name : String, val elemSize : Int) : Typedef(TypedefKind.Opaque, name, Datatype.OPAQUE) {
    override fun cdl(indent : Indent): String {
        return "${indent}opaque($elemSize) $name ;"
    }
}

// dont really need a typedef, no extra information
class VlenTypedef(name : String, baseType : Datatype) : Typedef(TypedefKind.Vlen, name, baseType) {
    override fun cdl(indent : Indent) : String {
        return "${indent}${baseType.cdlName}(*) $name ;"
    }
}