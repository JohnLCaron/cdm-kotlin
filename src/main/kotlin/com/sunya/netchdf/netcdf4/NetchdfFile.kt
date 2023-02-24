package com.sunya.netchdf.netcdf4

import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.hdf5.Hdf5File
import com.sunya.netchdf.netcdf3.Netcdf3File

fun openNetchdfFile(filename : String) : Netcdf? {
    OpenFile(filename).use { raf ->
        val format = NetchdfFileFormat.findNetcdfFormatType(raf)
        return when (format) {
            NetchdfFileFormat.NC_FORMAT_CLASSIC, NetchdfFileFormat.NC_FORMAT_64BIT_OFFSET -> Netcdf3File(filename)
            NetchdfFileFormat.NC_FORMAT_NETCDF4, NetchdfFileFormat.NC_FORMAT_NETCDF4_CLASSIC,  -> Hdf5File(filename, true)
            NetchdfFileFormat.HDF5  -> Hdf5File(filename, false)
            NetchdfFileFormat.NC_FORMAT_64BIT_DATA -> throw RuntimeException(" unsupported NetcdfFileFormat $format")
            else -> null
        }
    }
}