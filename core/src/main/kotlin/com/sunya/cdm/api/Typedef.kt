package com.sunya.cdm.api

import com.sunya.cdm.array.ArrayString
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.array.StructureMember
import com.sunya.cdm.util.Indent
import com.sunya.cdm.util.makeValidCdmObjectName

enum class TypedefKind {Compound, Enum, Opaque, Vlen, Unknown}

abstract class Typedef(val kind : TypedefKind, orgName : String, val baseType : Datatype<*>) {
    val name = makeValidCdmObjectName(orgName)

    abstract fun cdl(indent : Indent = Indent(2)): String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typedef) return false

        if (kind != other.kind) return false
        if (baseType != other.baseType) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + baseType.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return cdl()
    }
}

class CompoundTypedef(name : String, val members : List<StructureMember<*>>) : Typedef(TypedefKind.Compound, name, Datatype.COMPOUND) {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompoundTypedef) return false
        if (!super.equals(other)) return false

        return members == other.members
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + members.hashCode()
        return result
    }
}

private fun showDims(dims : IntArray) : String {
    return if (dims.isEmpty() || dims.computeSize() == 1) "" else
    buildString {
        append("(")
        dims.forEachIndexed { idx, num ->
            if (idx > 0) append(",")
            append("$num")
        }
        append(")")
    }
}

class EnumTypedef(name : String, baseType : Datatype<*>, val valueMap : Map<Int, String>) : Typedef(TypedefKind.Enum, name, baseType) {
    override fun cdl(indent : Indent) : String {
        return buildString {
            append("${indent}${baseType.strictEnumType().cdlName} enum $name {")
            var idx = 0
            valueMap.forEach {
                if (idx > 0) append(", ")
                append("${it.key} = ${it.value}")
                idx++
            }
            append("};")
        }
    }

    /** Convert enums into equivalent array of String */
    fun convertEnums(enums : Any): ArrayString {
        return when (enums) {
            is ArrayTyped<*> -> enums.convertEnums(valueMap)
            is Attribute<*> -> ArrayString(intArrayOf(enums.values.size), enums.values.convertEnums(valueMap))
            is List<*> -> ArrayString(intArrayOf(enums.size), enums.convertEnums(valueMap))
            else -> this.convertEnums(valueMap)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumTypedef) return false
        if (!super.equals(other)) return false

        return valueMap == other.valueMap
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + valueMap.hashCode()
        return result
    }
}

/** Convert Attribute values of type ENUM into equivalent names */
fun Attribute<*>.convertEnums(): List<String> {
    require(this.datatype.isEnum)
    val typedef = (this.datatype.typedef as EnumTypedef)
    return values.convertEnums(typedef.valueMap)
}

/** Convert array of ENUM into equivalent names */
fun ArrayTyped<*>.convertEnums(): ArrayString {
    require(this.datatype.isEnum)
    val typedef = (this.datatype.typedef as EnumTypedef)
    return this.convertEnums(typedef.valueMap)
}

/** Convert array of ENUM into equivalent names */
private fun ArrayTyped<*>.convertEnums(map: Map<Int, String>): ArrayString {
    val size = this.shape.computeSize()
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

/** Convert Iterator of ENUM into equivalent List of String */
private fun List<*>.convertEnums(map: Map<Int, String>): List<String> {
    val stringValues = this.map { enumVal ->
        val num = when (enumVal) {
            is UByte ->  enumVal.toInt()
            is UShort ->  enumVal.toInt()
            is UInt ->  enumVal.toInt()
            else -> RuntimeException("unknown enum ${enumVal!!::class}")
        }
        map[num] ?: "Unknown enum number=$enumVal"
    }
    return stringValues
}

private fun Datatype<*>.strictEnumType() : Datatype<*> {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpaqueTypedef) return false
        if (!super.equals(other)) return false

        return elemSize == other.elemSize
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + elemSize
        return result
    }
}

// dont really need a typedef, no extra information
class VlenTypedef(name : String, baseType : Datatype<*>) : Typedef(TypedefKind.Vlen, name, baseType) {
    override fun cdl(indent : Indent) : String {
        return "${indent}${baseType.cdlName}(*) $name ;"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VlenTypedef) return false
        return super.equals(other)
    }
}