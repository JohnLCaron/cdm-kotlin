package sunya.cdm.hdf5

import mu.KotlinLogging
import sunya.cdm.api.DataType
import java.nio.ByteOrder

private const val warnings = true
private val logger = KotlinLogging.logger("H5Type")
private val defaultDataType = DataType.STRING

internal class H5Type(mdt: DatatypeMessage) {
    val hdfType: Datatype5
    val elemSize: Int
    var dataType: DataType = defaultDataType
    var endian: ByteOrder = mdt.endian()
    var unsigned = false
    var isVString = false // is it a vlen string
    var isVlen = false // vlen but not string
    var base: H5Type? = null // vlen, enum

    init {
        hdfType = mdt.type
        elemSize = mdt.elemSize
        if (hdfType == Datatype5.Fixed) { // int, long, short, byte
            unsigned = (mdt as DatatypeFixed).unsigned
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.Floating) { // floats, doubles
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.Time) { // time
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.String) { // fixed length strings map to CHAR. String is used for Vlen type = 1.
            // LOOK when elem length = 1, there is a problem with dimensionality.
            //   eg char cr(2); has a storage_size of [1,1].
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.BitField) { // bit field
            dataType = getNCtype(hdfType, elemSize, true)
        } else if (hdfType == Datatype5.Opaque) { // opaque
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.Compound) { // structure
            dataType = getNCtype(hdfType, elemSize, unsigned)
        } else if (hdfType == Datatype5.Reference) { // reference
            // TODO - should get the object, and change type to whatever it is (?)
            endian = ByteOrder.LITTLE_ENDIAN
            dataType = DataType.LONG // file offset of the referenced object
        } else if (hdfType == Datatype5.Enumerated) { // enums
            dataType = getNCtype(hdfType, elemSize, unsigned)
            endian = (mdt as DatatypeEnum).base.endian()
            base = H5Type(mdt.base)
        } else if (hdfType == Datatype5.Vlen) { // variable length array
            val vlenMdt = mdt as DatatypeVlen
            isVString = vlenMdt.isVString
            isVlen = !vlenMdt.isVString
            base = H5Type(vlenMdt.base)
            // LOOK maybe make DataType.VLEN, maps to Sequence of baseType ??
            if (vlenMdt.isVString) {
                dataType = DataType.STRING
            } else {
                unsigned = vlenMdt.base.unsigned()
                dataType = getNCtype(vlenMdt.base.type, vlenMdt.base.elemSize, unsigned)
                endian = vlenMdt.base.endian()
            }
        } else if (hdfType == Datatype5.Array) { // array : used for structure members
            val arrayMdt = mdt as DatatypeArray
            endian = arrayMdt.base.endian()
            dataType = getNCtype(arrayMdt.base.type, arrayMdt.base.elemSize, arrayMdt.base.unsigned())
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
        // TODO not translating all of them !
        return when (hdfType) {
            Datatype5.Fixed, Datatype5.BitField, Datatype5.Time ->
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

            Datatype5.String -> DataType.CHAR // fixed length strings. String is used for Vlen type = 1
            Datatype5.Opaque -> DataType.OPAQUE
            Datatype5.Compound -> DataType.STRUCTURE
            Datatype5.Reference -> DataType.ULONG
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
        return "H5Type(hdfType=$hdfType, elemSize=$elemSize, dataType=$dataType, endian=$endian, unsigned=$unsigned, isVString=$isVString, isVlen=$isVlen, base=$base)"
    }


}