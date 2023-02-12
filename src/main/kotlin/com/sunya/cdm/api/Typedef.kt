package com.sunya.cdm.api

enum class TypedefKind {Compound, Enum, Opaque, Vlen}

open class Typedef(val name : String, val kind : TypedefKind)

class CompoundTypedef(name : String, val dims : IntArray) : Typedef(name, TypedefKind.Compound)
class EnumTypedef(name : String, val baseType : DataType, val values : Map<Int, String>) : Typedef(name, TypedefKind.Enum)
class OpaqueTypedef(name : String, val dims : IntArray) : Typedef(name, TypedefKind.Opaque)
class VlenTypedef(name : String, val baseType : DataType) : Typedef(name, TypedefKind.Vlen)