package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.*
import java.io.IOException

class Hdf5File(val filename : String, strict : Boolean = false) : Iosp, Netcdf {
    private val raf : OpenFile = OpenFile(filename)
    private val header : H5builder

    init {
        header = H5builder(raf, strict)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = header.cdmRoot
    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun cdlStrict() = com.sunya.cdm.api.cdlStrictOld(this)

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable): ArrayTyped<*> {
        TODO("Not yet implemented")
    }
}