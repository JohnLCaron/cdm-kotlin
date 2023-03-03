package com.sunya.netchdf.hdf5

import mu.KotlinLogging
import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Typedef
import java.nio.ByteOrder

private const val warnings = true
private val logger = KotlinLogging.logger("H5Type")
private val defaultDatatype = Datatype.STRING

// everything needed to read data from a dataset (attribute, variable) in HDF5
// sometime you need to read data before you have a cdm object, eg for attributes
internal class H5TypeInfo(mdt: DatatypeMessage) {
    val hdfType: Datatype5 = mdt.type
    val elemSize: Int = mdt.elemSize
    val endian: ByteOrder = mdt.endian()
    val isVString = if (mdt is DatatypeVlen) mdt.isVString else false // is a vlen string
    val isRefObject = if (mdt is DatatypeReference) mdt.referenceType == 0 else false // is a vlen string

    var unsigned = false
    var base: H5TypeInfo? = null // used for vlen, array
    val mdtAddress = mdt.address // used to look up typedefs
    val mdtHash = mdt.hashCode() // used to look up typedefs

    init {
        if (hdfType == Datatype5.Fixed) {
            unsigned = (mdt as DatatypeFixed).unsigned

        } else if (hdfType == Datatype5.BitField) {
            unsigned = (mdt as DatatypeBitField).unsigned

        } else if (hdfType == Datatype5.Vlen) { // variable length array
            val vlenMdt = mdt as DatatypeVlen
            base = H5TypeInfo(vlenMdt.base)

        } else if (hdfType == Datatype5.Array) { // array : used for structure members
            val arrayMdt = mdt as DatatypeArray
            base = H5TypeInfo(arrayMdt.base)
        }
    }

    /*
     * Value Description
     * 0 Fixed-Point
     * 1 Floating-point
     * 2 Time
     * 3 String
     * 4 Bit field
     * 5 Opaque
     * 6 Compound
     * 7 Reference
     * 8 Enumerated
     * 9 Variable-Length
     * 10 Array
     */
    // Call this after all the typedefs have been found
    fun datatype(h5builder : H5builder): Datatype {
        return when (hdfType) {
            Datatype5.Fixed, Datatype5.BitField ->
                when (this.elemSize) {
                    1 -> Datatype.BYTE.withSignedness(!unsigned)
                    2 -> Datatype.SHORT.withSignedness(!unsigned)
                    4 -> Datatype.INT.withSignedness(!unsigned)
                    8 -> Datatype.LONG.withSignedness(!unsigned)
                    else -> throw RuntimeException("Bad hdf5 integer type ($hdfType) with size= ${this.elemSize}")
                }

            Datatype5.Floating ->
                when (this.elemSize) {
                    4 -> Datatype.FLOAT
                    8 -> Datatype.DOUBLE
                    else -> throw RuntimeException("Bad hdf5 float type with size= ${this.elemSize}")
                }

            Datatype5.Time -> Datatype.LONG.withSignedness(true) // LOOK use bitPrecision i suppose
            Datatype5.String -> Datatype.CHAR // fixed length strings. String is used for Vlen type = 1
            Datatype5.Reference -> Datatype.LONG // addresses; type 1 gets converted to object name

            Datatype5.Opaque -> {
                val typedef = h5builder.findTypedef(this.mdtAddress, this.mdtHash)
                    return if (typedef == null) {
                        // theres no actual info in the typedef, so we will just allow this
                        logger.warn("Cant find Opaque typedef for $this")
                        Datatype.OPAQUE
                    } else {
                        Datatype.OPAQUE.withTypedef(typedef)
                    }
            }

            Datatype5.Compound -> {
                val typedef = h5builder.findTypedef(this.mdtAddress, this.mdtHash) ?: throw RuntimeException("Cant find Compound typedef for $this")
                Datatype.COMPOUND.withTypedef(typedef)
            }
            Datatype5.Enumerated -> {
                val typedef = h5builder.findTypedef(this.mdtAddress, this.mdtHash) ?: throw RuntimeException("Cant find Enum typedef for $this")
                when (this.elemSize) {
                    1 -> Datatype.ENUM1.withTypedef(typedef)
                    2 -> Datatype.ENUM2.withTypedef(typedef)
                    4 -> Datatype.ENUM4.withTypedef(typedef)
                    else -> throw RuntimeException("Bad hdf5 enum type with size= ${this.elemSize}")
                }
            }
            Datatype5.Vlen -> {
                if (this.isVString or this.base!!.isVString or (this.base!!.hdfType == Datatype5.Reference)) Datatype.STRING else {
                    val typedef = h5builder.findTypedef(this.mdtAddress, this.mdtHash)
                    return if (typedef == null) {
                        // theres no actual info in the typedef, so we will just allow this
                        logger.warn("Cant find Vlen typedef for $this")
                        Datatype.VLEN
                    } else {
                        Datatype.VLEN.withTypedef(typedef)
                    }
                }
            }
            Datatype5.Array -> {
                return this.base!!.datatype(h5builder) // ??
            }
        }
    }

    override fun toString(): String {
        return "H5TypeInfo(hdfType=$hdfType, elemSize=$elemSize, endian=$endian, isVString=$isVString, unsigned=$unsigned, base=$base)"
    }


}