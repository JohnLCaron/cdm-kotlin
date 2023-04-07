package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import java.nio.ByteOrder

internal fun H5builder.makeH5TypeInfo(mdt: DatatypeMessage, typedef : Typedef? = null) : H5TypeInfo {
    val datatype5: Datatype5 = mdt.type
    val elemSize: Int = mdt.elemSize
    val endian: ByteOrder = mdt.endian()
    val isVlenString = if (mdt is DatatypeVlen) mdt.isVString else false
    val isRefObject = if (mdt is DatatypeReference) mdt.referenceType == 0 else false

    var unsigned = false
    var base: H5TypeInfo? = null // used for vlen, array
    val mdtAddress = mdt.address // used to look up typedefs
    val mdtHash = mdt.hashCode() // used to look up typedefs

    if (datatype5 == Datatype5.Fixed) {
        unsigned = (mdt as DatatypeFixed).unsigned

    } else if (datatype5 == Datatype5.BitField) {
        unsigned = (mdt as DatatypeBitField).unsigned

    } else if (datatype5 == Datatype5.Vlen) { // variable length array
        val vlenMdt = mdt as DatatypeVlen
        base = makeH5TypeInfo(vlenMdt.base)

    } else if (datatype5 == Datatype5.Array) { // array : used for structure members
        val arrayMdt = mdt as DatatypeArray
        base = makeH5TypeInfo(arrayMdt.base)
    }

    val useTypedef = typedef ?: findTypedef(mdtAddress, mdtHash)

    return H5TypeInfo(isVlenString, isRefObject, datatype5, elemSize, !unsigned, endian,
        mdtAddress, mdtHash, base, useTypedef)
}

internal data class H5TypeInfo(val isVlenString: Boolean, val isRefObject : Boolean, val datatype5 : Datatype5, val elemSize : Int,
                               val signed : Boolean, val endian : ByteOrder, val mdtAddress : Long, val mdtHash : Int,
                               val base : H5TypeInfo? = null, val typedef : Typedef? = null, val dims : IntArray? = null) {

    // Call this after all the typedefs have been found
    fun datatype(): Datatype {
        return when (datatype5) {
            Datatype5.Fixed, Datatype5.BitField ->
                when (this.elemSize) {
                    1 -> Datatype.BYTE.withSignedness(signed)
                    2 -> Datatype.SHORT.withSignedness(signed)
                    4 -> Datatype.INT.withSignedness(signed)
                    8 -> Datatype.LONG.withSignedness(signed)
                    else -> throw RuntimeException("Bad hdf5 integer type ($datatype5) with size= ${this.elemSize}")
                }

            Datatype5.Floating ->
                when (this.elemSize) {
                    4 -> Datatype.FLOAT
                    8 -> Datatype.DOUBLE
                    else -> throw RuntimeException("Bad hdf5 float type with size= ${this.elemSize}")
                }

            Datatype5.Time -> Datatype.LONG.withSignedness(true) // LOOK use bitPrecision i suppose?
            Datatype5.String -> if ((isVlenString) or (elemSize > 1)) Datatype.STRING else Datatype.CHAR
            Datatype5.Reference -> Datatype.REFERENCE // "object" gets converted to dataset path, "region" ignored

            Datatype5.Opaque -> if (typedef != null) Datatype.OPAQUE.withTypedef(typedef) else Datatype.OPAQUE

            Datatype5.Compound -> {
                if (typedef == null)
                    println()
                Datatype.COMPOUND.withTypedef(typedef!!)
            }

            Datatype5.Enumerated -> {
                when (this.elemSize) {
                    1 -> Datatype.ENUM1.withTypedef(typedef)
                    2 -> Datatype.ENUM2.withTypedef(typedef)
                    4 -> Datatype.ENUM4.withTypedef(typedef)
                    else -> throw RuntimeException("Bad hdf5 enum type with size= ${this.elemSize}")
                }
            }

            Datatype5.Vlen -> {
                if (isVlenString || this.base!!.isVlenString) Datatype.STRING
                else Datatype.VLEN.withTypedef(typedef)
            }

            Datatype5.Array -> {
                return this.base!!.datatype() // ??
            }
        }
    }
}