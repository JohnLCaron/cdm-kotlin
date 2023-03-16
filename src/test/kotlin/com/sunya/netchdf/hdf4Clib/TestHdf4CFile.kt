package com.sunya.netchdf.hdf4Clib

import org.junit.jupiter.api.Test

class TestHdf4CFile {
    @Test
    fun testHCFile() {
        open("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf")
    }

    private fun open(filename: String) {
        Hdf4ClibFile(filename).use { hcFile ->
            println("hcFile = ${hcFile.cdl()}")
        }
    }

}