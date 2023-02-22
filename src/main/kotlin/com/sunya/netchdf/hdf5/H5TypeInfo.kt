package com.sunya.netchdf.hdf5

import mu.KotlinLogging
import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Typedef
import java.nio.ByteOrder

private const val warnings = true
private val logger = KotlinLogging.logger("H5Type")
private val defaultDatatype = Datatype.STRING

// LOOK needs to be rewritten, unclear on its scope
internal class H5TypeInfo(mdt: DatatypeMessage, typedef : Typedef? = null) {
    val hdfType: Datatype5 = mdt.type
    val elemSize: Int = mdt.elemSize
    var base: BaseType? = null // only different for vlen, enum
    var endian: ByteOrder = mdt.endian()
    var unsigned = false
    var datatype: Datatype = makeNCdatatype(hdfType, elemSize, unsigned)

    var isVString = false // is a vlen string
    var isVlen = false // is a vlen but not string

    init {
        if (hdfType == Datatype5.Fixed || hdfType == Datatype5.BitField) { // int, long, short, byte
            unsigned = (mdt as DatatypeFixed).unsigned
            datatype = makeNCdatatype(hdfType, elemSize, unsigned) // redo now that we know the sign

        } else if (hdfType == Datatype5.Enumerated) {
            val enumMdt = mdt as DatatypeEnum
            base = BaseType(enumMdt.datatype, enumMdt.endian(), hdfType) // LOOK hdfType wrong

        } else if (hdfType == Datatype5.Vlen) { // variable length array
            val vlenMdt = mdt as DatatypeVlen
            isVString = vlenMdt.isVString
            isVlen = !vlenMdt.isVString
            val baseDatatype = H5TypeInfo(vlenMdt.base)

            base = BaseType(baseDatatype.datatype, vlenMdt.endian(), vlenMdt.type)
            if (vlenMdt.isVString) {
                datatype = Datatype.STRING
            } else {
                unsigned = vlenMdt.base.unsigned()
                endian = vlenMdt.base.endian()
                // LOOK val baseType = getNCtype(vlenMdt.base.type, vlenMdt.base.elemSize, unsigned)
                if (vlenMdt.base.type == Datatype5.Reference) {
                    datatype = Datatype.STRING
                }
            }

        } else if (hdfType == Datatype5.Array) { // array : used for structure members
            val arrayMdt = mdt as DatatypeArray
            // val baseDatatype = H5Type(arrayMdt.base)
            endian = arrayMdt.base.endian()
            unsigned = arrayMdt.base.unsigned()
            datatype = makeNCdatatype(arrayMdt.base.type, arrayMdt.base.elemSize, unsigned)
            if (arrayMdt.base.type == Datatype5.Vlen) {
                val vlenMdt = arrayMdt.base as DatatypeVlen
                if (vlenMdt.isVString) {
                    datatype = Datatype.STRING
                }
            }

        } else if (warnings) {
            logger.warn("WARNING not handling hdf datatype = $hdfType size= $elemSize")
        }

        if (typedef != null) {
            datatype = datatype.withTypedef(typedef)
        }
    }

    class BaseType(
        val datatype : Datatype,
        val endian : ByteOrder,
        val hdfType : Datatype5,
    )

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
    private fun makeNCdatatype(hdfType: Datatype5, size: Int, unsigned: Boolean): Datatype {
        return when (hdfType) {
            Datatype5.Fixed, Datatype5.BitField ->
                when (size) {
                    1 -> Datatype.BYTE.withSignedness(!unsigned)
                    2 -> Datatype.SHORT.withSignedness(!unsigned)
                    4 -> Datatype.INT.withSignedness(!unsigned)
                    8 -> Datatype.LONG.withSignedness(!unsigned)
                    else -> throw RuntimeException("Bad hdf5 integer type ($hdfType) with size= $size")
                }

            Datatype5.Floating ->
                when (size) {
                    4 -> Datatype.FLOAT
                    8 -> Datatype.DOUBLE
                    else -> throw RuntimeException("Bad hdf5 float type with size= $size")
                }

            Datatype5.Time -> Datatype.LONG.withSignedness(true) // LOOK use bitPrecision i suppose
            Datatype5.String -> Datatype.CHAR // fixed length strings. String is used for Vlen type = 1
            Datatype5.Opaque -> Datatype.OPAQUE
            Datatype5.Compound -> Datatype.COMPOUND
            Datatype5.Reference -> Datatype.STRING
            Datatype5.Enumerated ->
                when (size) {
                    1 -> Datatype.ENUM1
                    2 -> Datatype.ENUM2
                    4 -> Datatype.ENUM4
                    else -> throw RuntimeException("Bad hdf5 enum type with size= $size")
                }

            Datatype5.Vlen -> Datatype.VLEN
            Datatype5.Array -> defaultDatatype // dunno, has to be transformed to base[dims]
        }
    }

    override fun toString(): String {
        return "hdfType=$hdfType, elemSize=$elemSize, endian=$endian, unsigned=$unsigned, datatype=$datatype, isVString=$isVString, isVlen=$isVlen, base=$base)"
    }

}