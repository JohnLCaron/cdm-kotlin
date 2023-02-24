package com.sunya.cdm.iosp

import com.sunya.cdm.api.Group
import com.sunya.cdm.api.InvalidRangeException
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArrayTyped
import java.io.IOException

interface Iosp {

    fun rootGroup() : Group

    @Throws(IOException::class, InvalidRangeException::class)
    fun readArrayData(v2: Variable, section: Section? = null) : ArrayTyped<*>

}