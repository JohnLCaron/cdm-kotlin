package sunya.cdm.api

import java.math.BigInteger
import java.util.*

/** Type-safe enumeration of data types.
 * @param cdlName name in CDL
 * @param size Size in bytes of one element of this data type, Strings = 0, Structures = 1
 * @param primitiveClass The primitive class type, inverse of forPrimitiveClass()
 * @param signed only needed for integral types
 */
enum class DataType(val cdlName: String, val size: Int, val primitiveClass: Class<*>, val signed: Boolean) {
    BYTE("byte", 1, Byte::class.java, false),  //
    CHAR("char", 1, Byte::class.java, false),  //
    SHORT( "short", 2, Short::class.java, false),  //
    INT( "int", 4, Int::class.java, false),  //
    LONG( "int64", 8, Long::class.java, false),  //
    FLOAT("float", 4, Float::class.java, false),  //
    DOUBLE( "double", 8, Double::class.java, false),  //
    UBYTE( "ubyte", 1, Byte::class.java, true),  // unsigned byte
    USHORT( "ushort", 2, Short::class.java, true),  // unsigned short
    UINT( "uint", 4, Int::class.java, true),  // unsigned int
    ULONG( "uint64", 8, Long::class.java, true),  // unsigned long
    ENUM1( "enum1", 1, Byte::class.java, false),  // byte
    ENUM2( "enum2", 2, Short::class.java, false),  // short
    ENUM4( "enum4", 4, Int::class.java, false),  // int

    //// object types are variable length; inside a structure, they have 32 bit indices onto a heap
    STRING( "string", 4, String::class.java, false),  // compact storage of heterogeneous fields
    STRUCTURE("Structure", 0, StructureData::class.java, false),  // Iterator<StructureData>
    SEQUENCE(
        "Sequence",
        4,
        StructureData::class.java,
        false
    ),  // Array<Array<Byte>>, an array of variable length byte arrays
    OPAQUE( "opaque", 4, Array::class.java, false);
    
    override fun toString(): String {
        return toNcml()
    }

    /** The name used for Ncml.  */
    fun toNcml(): String {
        return if (this == STRING) "String" else name
    }

    val isString : Boolean
        get() = (this == STRING) || (this == CHAR)
    
    val isNumeric: Boolean
        get() = (this == FLOAT) || (this == DOUBLE) || isIntegral
    
    val isIntegral: Boolean
        get() = ((this == BYTE) || (this == INT) || (this == SHORT) || (this == LONG)
                || (this == UBYTE) || (this == UINT) || (this == USHORT)
                || (this == ULONG))
    
    val isFloatingPoint: Boolean
        get() =  (this == FLOAT) || (this == DOUBLE)
        
    val isEnum: Boolean
        get() = (this == ENUM1) || (this == ENUM2) || (this == ENUM4)
        
    val isStruct: Boolean
        get() =  (this == STRUCTURE) || (this == SEQUENCE)

    /**
     * Returns an DataType that is related to `this`, but with the specified signedness.
     * This method is only meaningful for [integral][.isIntegral] data types; if it is called on a non-integral
     * type, then `this` is simply returned.
     */
    fun withSignedness(signed: Boolean): DataType {
        return when (this) {
            BYTE, UBYTE -> if (!signed) UBYTE else BYTE
            SHORT, USHORT -> if (!signed) USHORT else SHORT
            INT, UINT -> if (!signed) UINT else INT
            LONG, ULONG -> if (!signed) ULONG else LONG
            else -> this
        }
    }

    companion object {
        ///////////////////////////////////////////////////////////////////////////////////////////////
        /**
         * Find the DataType that matches this name.
         *
         * @param name find DataType with this name.
         * @return DataType or null if no match.
         */
        fun getTypeByName(name: String?): DataType? {
            if (name == null) {
                return null
            }
            try {
                return valueOf(name.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) { // lame!
                return null
            }
        }

        /** Find the DataType used for this primitive class type.  */
        fun forPrimitiveClass(c: Class<*>, isUnsigned: Boolean): DataType {
            if ((c == Float::class.javaPrimitiveType) || (c == Float::class.java)) return FLOAT
            if ((c == Double::class.javaPrimitiveType) || (c == Double::class.java)) return DOUBLE
            if ((c == Short::class.javaPrimitiveType) || (c == Short::class.java)) return if (isUnsigned) USHORT else SHORT
            if ((c == Int::class.javaPrimitiveType) || (c == Int::class.java)) return if (isUnsigned) UINT else INT
            if ((c == Byte::class.javaPrimitiveType) || (c == Byte::class.java)) return if (isUnsigned) UBYTE else BYTE
            if ((c == Char::class.javaPrimitiveType) || (c == Char::class.java)) return CHAR
            if ((c == Long::class.javaPrimitiveType) || (c == Long::class.java)) return if (isUnsigned) ULONG else LONG
            if (c == String::class.java) return STRING
            if (c == StructureData::class.java) return STRUCTURE
            throw RuntimeException("Unsupported primitive class " + c.name)
        }

        /**
         * Convert the argument to the next largest integral data type by an unsigned conversion. In the larger data type,
         * the upper-order bits will be zero, and the lower-order bits will be equivalent to the bits in `number`.
         * Thus, we are "widening" the argument by prepending a bunch of zero bits to it.
         *
         * This widening operation is intended to be used on unsigned integral values that are being stored within one of
         * Java's signed, integral data types. For example, if we have the bit pattern "11001010" and treat it as
         * an unsigned byte, it'll have the decimal value "202". However, if we store that bit pattern in a (signed)
         * byte, Java will interpret it as "-52". Widening the byte to a short will mean that the most-significant
         * set bit is no longer the sign bit, and thus Java will no longer consider the value to be negative.
         *
         *
         * <table border="1">
         * <tr>
         * <th>Argument type</th>
         * <th>Result type</th>
        </tr> *
         * <tr>
         * <td>Byte</td>
         * <td>Short</td>
        </tr> *
         * <tr>
         * <td>Short</td>
         * <td>Integer</td>
        </tr> *
         * <tr>
         * <td>Integer</td>
         * <td>Long</td>
        </tr> *
         * <tr>
         * <td>Long</td>
         * <td>BigInteger</td>
        </tr> *
         * <tr>
         * <td>Any other Number subtype</td>
         * <td>Just return argument</td>
        </tr> *
        </table> *
         *
         * @param number an integral number to treat as unsigned.
         * @return an equivalent but wider value that Java will interpret as non-negative.
         */
        fun widenNumber(number: Number): Number {
            if (number is Byte) {
                return unsignedByteToShort(number.toByte())
            } else if (number is Short) {
                return unsignedShortToInt(number.toShort())
            } else if (number is Int) {
                return unsignedIntToLong(number.toInt())
            } else if (number is Long) {
                return unsignedLongToBigInt(number.toLong())
            } else {
                return number
            }
        }

        /**
         * This method is similar to [.widenNumber], but only integral types *that are negative* are widened.
         *
         * @param number an integral number to treat as unsigned.
         * @return an equivalent value that Java will interpret as non-negative.
         */
        fun widenNumberIfNegative(number: Number): Number {
            if (number is Byte && number.toByte() < 0) {
                return unsignedByteToShort(number.toByte())
            } else if (number is Short && number.toShort() < 0) {
                return unsignedShortToInt(number.toShort())
            } else if (number is Int && number.toInt() < 0) {
                return unsignedIntToLong(number.toInt())
            } else if (number is Long && number.toLong() < 0) {
                return unsignedLongToBigInt(number.toLong())
            }
            return number
        }

        /**
         * Converts the argument to a [BigInteger] by an unsigned conversion. In an unsigned conversion to a
         * [BigInteger], zero and positive `long` values are mapped to a numerically equal [BigInteger]
         * value and negative `long` values are mapped to a [BigInteger] value equal to the input plus
         * 2<sup>64</sup>.
         *
         * @param l a `long` to treat as unsigned.
         * @return the equivalent [BigInteger] value.
         */
        fun unsignedLongToBigInt(l: Long): BigInteger {
            // This is a copy of the implementation of Long.toUnsignedBigInteger(), which is private for some reason.
            if (l >= 0L) return BigInteger.valueOf(l) else {
                val upper: Int = (l ushr 32).toInt()
                val lower: Int = l.toInt()

                // return (upper << 32) + lower
                return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32)
                    .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)))
            }
        }

        /**
         * Converts the argument to a `long` by an unsigned conversion. In an unsigned conversion to a `long`,
         * the high-order 32 bits of the `long` are zero and the low-order 32 bits are equal to the bits of the integer
         * argument.
         *
         * Consequently, zero and positive `int` values are mapped to a numerically equal `long` value and
         * negative `int` values are mapped to a `long` value equal to the input plus 2<sup>32</sup>.
         *
         * @param i an `int` to treat as unsigned.
         * @return the equivalent `long` value.
         */
        fun unsignedIntToLong(i: Int): Long {
            return Integer.toUnsignedLong(i)
        }

        /**
         * Converts the argument to an `int` by an unsigned conversion. In an unsigned conversion to an `int`,
         * the high-order 16 bits of the `int` are zero and the low-order 16 bits are equal to the bits of the `short` argument.
         *
         * Consequently, zero and positive `short` values are mapped to a numerically equal `int` value and
         * negative `short` values are mapped to an `int` value equal to the input plus 2<sup>16</sup>.
         *
         * @param s a `short` to treat as unsigned.
         * @return the equivalent `int` value.
         */
        fun unsignedShortToInt(s: Short): Int {
            return java.lang.Short.toUnsignedInt(s)
        }

        /**
         * Converts the argument to a `short` by an unsigned conversion. In an unsigned conversion to a `short`, the high-order 8 bits of the `short` are zero and the low-order 8 bits are equal to the bits of
         * the `byte` argument.
         *
         * Consequently, zero and positive `byte` values are mapped to a numerically equal `short` value and
         * negative `byte` values are mapped to a `short` value equal to the input plus 2<sup>8</sup>.
         *
         * @param b a `byte` to treat as unsigned.
         * @return the equivalent `short` value.
         */
        fun unsignedByteToShort(b: Byte): Short {
            return java.lang.Byte.toUnsignedInt(b).toShort()
        }
    }
}