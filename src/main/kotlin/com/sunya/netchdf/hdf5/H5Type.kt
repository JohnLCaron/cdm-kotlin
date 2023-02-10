package com.sunya.netchdf.hdf5

import mu.KotlinLogging
import com.sunya.cdm.api.DataType
import java.nio.ByteOrder

private const val warnings = true
private val logger = KotlinLogging.logger("H5Type")
private val defaultDataType = DataType.STRING

internal class H5Type(mdt: DatatypeMessage) {
    val hdfType: Datatype5 = mdt.type
    val elemSize: Int = mdt.elemSize
    var base: H5Type? = null // only different for vlen, enum, array
    var endian: ByteOrder = mdt.endian()
    var unsigned = false
    var dataType: DataType = getNCtype(hdfType, elemSize, unsigned)

    var isVString = false // is a vlen string
    var isVlen = false // is a vlen but not string

    init {
        if (hdfType == Datatype5.Fixed) { // int, long, short, byte
            unsigned = (mdt as DatatypeFixed).unsigned
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.BitField) { // bit field
            unsigned = true
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.Reference) { // reference
            // TODO - could get the object, and change type to whatever it is (?)
            endian = ByteOrder.LITTLE_ENDIAN
        } else if (hdfType == Datatype5.Enumerated) { // enums
            base = H5Type((mdt as DatatypeEnum).base)
            endian = base!!.endian
        } else if (hdfType == Datatype5.Vlen) { // variable length array
            val vlenMdt = mdt as DatatypeVlen
            isVString = vlenMdt.isVString
            isVlen = !vlenMdt.isVString
            base = H5Type(vlenMdt.base) // LOOK maybe make DataType.VLEN, maps to Sequence of baseType ??
            if (vlenMdt.isVString) {
                dataType = DataType.STRING
            } else {
                unsigned = vlenMdt.base.unsigned()
                endian = vlenMdt.base.endian()
                val baseType = getNCtype(vlenMdt.base.type, vlenMdt.base.elemSize, unsigned)
                dataType = if (vlenMdt.base.type == Datatype5.Reference) DataType.STRING else baseType
            }
        } else if (hdfType == Datatype5.Array) { // array : used for structure members
            val arrayMdt = mdt as DatatypeArray
            base = H5Type(arrayMdt.base)
            endian = arrayMdt.base.endian()
            unsigned = arrayMdt.base.unsigned()
            dataType = getNCtype(arrayMdt.base.type, arrayMdt.base.elemSize, unsigned)
            if (arrayMdt.base.type == Datatype5.Vlen) {
                val vlenMdt = arrayMdt.base as DatatypeVlen
                if (vlenMdt.isVString) {
                    dataType = DataType.STRING
                }
            }
        } else if (warnings) {
            logger.warn("WARNING not handling hdf dataType = $hdfType size= $elemSize")
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
    private fun getNCtype(hdfType: Datatype5, size: Int, unsigned: Boolean): DataType {
        return when (hdfType) {
            Datatype5.Fixed, Datatype5.BitField ->
                when (size) {
                    1 -> DataType.BYTE.withSignedness(!unsigned)
                    2 -> DataType.SHORT.withSignedness(!unsigned)
                    4 -> DataType.INT.withSignedness(!unsigned)
                    8 -> DataType.LONG.withSignedness(!unsigned)
                    else -> throw RuntimeException("Bad hdf5 integer type ($hdfType) with size= $size")
                }

            Datatype5.Floating ->
                when (size) {
                    4 -> DataType.FLOAT
                    8 -> DataType.DOUBLE
                    else -> throw RuntimeException("Bad hdf5 float type with size= $size")
                }

            Datatype5.Time -> DataType.LONG.withSignedness(true) // LOOK use bitPrecision i suppose
            Datatype5.String -> DataType.CHAR // fixed length strings. String is used for Vlen type = 1
            Datatype5.Opaque -> DataType.OPAQUE
            Datatype5.Compound -> DataType.STRUCTURE
            Datatype5.Reference -> DataType.STRING
            Datatype5.Enumerated ->
                when (size) {
                    1 -> DataType.ENUM1
                    2 -> DataType.ENUM2
                    4 -> DataType.ENUM4
                    else -> throw RuntimeException("Bad hdf5 enum type with size= $size")
                }

            Datatype5.Vlen -> defaultDataType // dunno
            Datatype5.Array -> defaultDataType // dunno
        }
    }

    override fun toString(): String {
        return "hdfType=$hdfType, elemSize=$elemSize, endian=$endian, unsigned=$unsigned, dataType=$dataType, isVString=$isVString, isVlen=$isVlen, base=$base)"
    }

}