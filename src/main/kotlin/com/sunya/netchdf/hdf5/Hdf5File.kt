package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Group
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.*
import java.io.IOException

class Hdf5File(val filename : String) : Iosp {
    private val raf : OpenFile = OpenFile(filename)
    private val header : H5builder
    private val rootGroup : Group

    init {
        val rootBuilder = Group.Builder("")
        header = H5builder(raf, rootBuilder)
        rootGroup = rootBuilder.build(null)
    }

    override fun rootGroup() = rootGroup

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable): ArrayTyped<*> {
        TODO("Not yet implemented")
    }
}