package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.array.StructureMember

internal class H5typedef(val dataObject: DataObject) {
    var enumMessage : DatatypeEnum? = null
    var vlenMessage : DatatypeVlen? = null
    var opaqueMessage : DatatypeOpaque? = null
    var compoundMessage : DatatypeCompound? = null

    val kind : TypedefKind
    val mdtAddress : Long
    val mdtHash : Int

    init {
        require(dataObject.mdt != null)
        mdtAddress = dataObject.mdt.address
        mdtHash = dataObject.mdt.hashCode()

        when (dataObject.mdt.type) {
            Datatype5.Enumerated -> {
                this.enumMessage = (dataObject.mdt) as DatatypeEnum
                kind = TypedefKind.Enum
            }
            Datatype5.Vlen -> {
                this.vlenMessage = (dataObject.mdt) as DatatypeVlen
                kind = TypedefKind.Vlen
            }
            Datatype5.Opaque -> {
                this.opaqueMessage = (dataObject.mdt) as DatatypeOpaque
                kind = TypedefKind.Opaque
            }
            Datatype5.Compound -> {
                this.compoundMessage = (dataObject.mdt) as DatatypeCompound
                kind = TypedefKind.Compound
            }
            else -> {
                kind = TypedefKind.Unknown
            }
        }
        if (debugTypedefs) println("H5Typedef mdtAddress=$mdtAddress mdtHash=$mdtHash kind=$kind")
    }
}

internal fun H5builder.buildTypedef(groupb : Group.Builder, typedef5: H5typedef): H5TypeInfo {
    val typedef : Typedef? = when (typedef5.kind) {
        TypedefKind.Compound -> {
            val mess = typedef5.compoundMessage!!
            this.buildCompoundTypedef(groupb, typedef5.dataObject.name!!, mess)
        }
        TypedefKind.Enum -> {
            val mess = typedef5.enumMessage!!
            EnumTypedef(typedef5.dataObject.name!!, mess.datatype, mess.valuesMap)
        }
        TypedefKind.Opaque -> {
            val mess = typedef5.opaqueMessage!!
            OpaqueTypedef(typedef5.dataObject.name!!, mess.elemSize)
        }
        TypedefKind.Vlen -> {
            val mess = typedef5.vlenMessage!!
            val h5type = makeH5TypeInfo(mess.base)
            VlenTypedef(typedef5.dataObject.name!!, h5type.datatype())
        }
        else -> null
    }
    val typeinfo = makeH5TypeInfo(typedef5.dataObject.mdt!!, typedef)
    return registerTypedef(typeinfo, groupb)

}

// allow it to recurse
internal fun H5builder.buildCompoundTypedef(groupb : Group.Builder, name : String, mess: DatatypeCompound) : CompoundTypedef {
    // first look for embedded typedefs that need to be added
    mess.members.forEach { member ->
        val nestedTypedef = when (member.mdt.type) {
            Datatype5.Compound -> buildCompoundTypedef(groupb, member.name, member.mdt as DatatypeCompound)
            Datatype5.Enumerated -> buildEnumTypedef(member.name, member.mdt as DatatypeEnum)
            else -> null
        }
        if (nestedTypedef != null) {
            val ntypeinfo = makeH5TypeInfo(member.mdt, nestedTypedef)
            registerTypedef(ntypeinfo, groupb)
        }
    }

    // now build the typedef for the compound message
    val members = mess.members.map {
        val h5type = makeH5TypeInfo(it.mdt)
        val datatype = h5type.datatype()
        StructureMember(it.name, datatype, it.offset, it.dims, it.mdt.endian)
    }
    return CompoundTypedef(name, members)
}

internal fun buildEnumTypedef(name : String, mess: DatatypeEnum): EnumTypedef {
    return EnumTypedef(name, mess.datatype, mess.valuesMap)
}