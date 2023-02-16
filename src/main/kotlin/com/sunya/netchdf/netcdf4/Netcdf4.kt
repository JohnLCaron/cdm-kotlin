package com.sunya.netchdf.netcdf4

class Netcdf4 {
    companion object {
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