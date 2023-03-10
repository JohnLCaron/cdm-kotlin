package com.sunya.netchdf.hdf4Clib

import org.junit.jupiter.api.Test

class TestHdf4CFile {
    @Test
    fun testHdf4CFile() {
        open("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf")
    }

    private fun open(filename: String) {
        Hdf4ClibFile(filename).use { hcFile ->
            println("hcFile = ${hcFile.cdl()}")
        }
    }

}