package com.sunya.netchdf.netcdf4

import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.hdf5.Hdf5File
import com.sunya.netchdf.netcdf3.Netcdf3File

fun openNetchdfFile(filename : String) : Netcdf? {
    OpenFile(filename).use { raf ->
        val format = NetcdfFileFormat.findNetcdfFormatType(raf)
        return when (format) {
            NetcdfFileFormat.NC_FORMAT_CLASSIC, NetcdfFileFormat.NC_FORMAT_64BIT_OFFSET -> Netcdf3File(filename)
            NetcdfFileFormat.NC_FORMAT_NETCDF4, NetcdfFileFormat.NC_FORMAT_NETCDF4_CLASSIC -> Hdf5File(filename, true)
            NetcdfFileFormat.NC_FORMAT_64BIT_DATA -> throw RuntimeException(" unsupported NetcdfFileFormat $format")
            else -> null
        }
    }
}