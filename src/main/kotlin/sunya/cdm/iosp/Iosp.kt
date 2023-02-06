package sunya.cdm.iosp

import sunya.cdm.api.Group
import sunya.cdm.api.InvalidRangeException
import sunya.cdm.api.Section
import sunya.cdm.api.Variable
import java.io.IOException

interface Iosp {

    fun rootGroup() : Group

    @Throws(IOException::class, InvalidRangeException::class)
    fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*>

    @Throws(IOException::class)
    fun readArrayData(v2: Variable): ArrayTyped<*>

}