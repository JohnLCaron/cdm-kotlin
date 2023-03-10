package com.sunya.netchdf.netcdf4

class Netcdf4 {
    companion object {
        // Default fill values, used when _FillValue variable attribute is set.
        const val  NC_FILL_BYTE: kotlin.Byte = -127
        const val  NC_FILL_CHAR = 0.toChar()
        const val  NC_FILL_SHORT: Short = (-32767.toShort()).toShort()
        const val  NC_FILL_INT: Int = -2147483647
        const val  NC_FILL_FLOAT: kotlin.Float = 9.9692099683868690e+36f /* near 15 * 2^119 */
        const val  NC_FILL_DOUBLE: kotlin.Double = 9.9692099683868690e+36

        const val  NC_FILL_UBYTE: kotlin.Byte = 255.toByte()
        const val  NC_FILL_USHORT: Short = 65535.toShort()
        const val  NC_FILL_UINT: Int = 4294967295L.toInt()
        const val  NC_FILL_INT64: Long = -9223372036854775806L // 0x8000000000000002. Only bits 63 and 1 set.
        const val  NC_FILL_UINT64: Long = -0x2L
        const val  NC_FILL_STRING: String = ""

        const val NCPROPERTIES = "_NCProperties"

        //// Special netcdf-4 specific stuff. used by both Java (H5header) and JNA interface (Nc4Iosp)
        // @see "https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_4_spec.html"
        // only on the multi-dimensional coordinate variables of the netCDF model (2D chars).
        // appears to hold the dimension ids of the 2 dimensions.
        const val NETCDF4_COORDINATES = "_Netcdf4Coordinates"

        // on dimension scales, holds a scalar H5T_NATIVE_INT,
        // which is the (zero-based) dimension ID for this dimension. used to maintain creation order
        const val NETCDF4_DIMID = "_Netcdf4Dimid"

        // global - when using classic model
        const val NETCDF4_STRICT = "_nc3_strict"

        val NETCDF4_SPECIAL_ATTS = listOf(NCPROPERTIES, NETCDF4_COORDINATES, NETCDF4_STRICT, NETCDF4_DIMID)

        // appended to variable when it conflicts with dimension scale
        val NETCDF4_NON_COORD = "_nc4_non_coord_"
    }
}