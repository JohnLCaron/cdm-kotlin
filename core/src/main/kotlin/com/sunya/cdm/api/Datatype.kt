package com.sunya.cdm.api

/**
 * The CDM API datatype. Note that file storage types may be different.
 * @param cdlName name in CDL
 * @param size Size in bytes of one element of this data type.
 * @param typedef used for ENUM, VLEN, OPAQUE, COMPOUND
 */
data class Datatype(val cdlName: String, val size: Int, val typedef : Typedef? = null) {

    companion object {
        val BYTE = Datatype("byte", 1)
        val CHAR = Datatype("char", 1)
        val SHORT = Datatype("short", 2)
        val INT = Datatype("int", 4)
        val LONG = Datatype("int64", 8)
        val FLOAT = Datatype("float", 4)
        val DOUBLE = Datatype("double", 8)
        val UBYTE = Datatype("ubyte", 1)
        val USHORT = Datatype("ushort", 2)
        val UINT = Datatype("uint", 4)
        val ULONG = Datatype("uint64", 8)

        val ENUM1 = Datatype("ubyte enum", 1)
        val ENUM2 = Datatype("ushort enum", 2)
        val ENUM4 = Datatype("uint enum", 4)

        //// object types are variable length; inside a structure, they have 32 bit index onto a heap
        val STRING = Datatype("string", 4)
        val COMPOUND = Datatype("compound", 4)
        val OPAQUE = Datatype("opaque", 4)
        val VLEN = Datatype("vlen", 4)
        val REFERENCE = Datatype("reference", 4) // string = full path to referenced dataset
    }

    override fun toString(): String {
        return if (this == VLEN) "$cdlName ${typedef?.baseType?.cdlName}" else cdlName
    }

    val isString : Boolean
        get() = (this == STRING) || (this == CHAR)

    val isNumeric: Boolean
        get() = (this == FLOAT) || (this == DOUBLE) || isIntegral

    val isNumber: Boolean
        get() = (this == FLOAT) || (this == DOUBLE) || (this == BYTE) || (this == INT) || (this == SHORT) ||
                (this == LONG)


    val isUnsigned: Boolean
        get() = (this == UBYTE) || (this == USHORT) || (this == UINT) || (this == ULONG)

    val isIntegral: Boolean
        get() = ((this == BYTE) || (this == INT) || (this == SHORT) || (this == LONG)
                || (this == UBYTE) || (this == UINT) || (this == USHORT)
                || (this == ULONG))
    
    val isFloatingPoint: Boolean
        get() =  (this == FLOAT) || (this == DOUBLE)

    /**
     * Returns the DataType that is related to `this`, but with the specified signedness.
     * This method is only meaningful for [integral][.isIntegral] data types; if it is called on a non-integral
     * type, then `this` is simply returned.
     */
    fun withSignedness(signed: Boolean): Datatype {
        return when (this) {
            BYTE, UBYTE -> if (!signed) UBYTE else BYTE
            SHORT, USHORT -> if (!signed) USHORT else SHORT
            INT, UINT -> if (!signed) UINT else INT
            LONG, ULONG -> if (!signed) ULONG else LONG
            else -> this
        }
    }

    /** Used for Hdf5 Enum, Compound, Opaque, Vlen.
     * The last two arent particularly useful, but we leave them in to agree with the Netcdf4 C library. */
    fun withTypedef(typedef : Typedef?) : Datatype = this.copy(typedef = typedef)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Datatype

        if (cdlName != other.cdlName) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cdlName.hashCode()
        result = 31 * result + size
        return result
    }

}