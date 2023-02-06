package sunya.cdm.hdf5

import sunya.cdm.api.Group
import sunya.cdm.api.Section
import sunya.cdm.api.Variable
import sunya.cdm.iosp.*
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