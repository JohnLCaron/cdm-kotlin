package com.sunya.netchdf.parser

import com.sunya.cdm.api.Group
import com.sunya.cdm.api.Netchdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.ArraySection

// A Netcdf created from CDL, no backing file store
class NetcdfCdl(val location : String, val rootGroup : Group) : Netchdf {
    override fun rootGroup() = rootGroup

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    override fun chunkIterator(v2: Variable, section: Section?): Iterator<ArraySection>? {
        return null
    }

    override fun location() = location
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = "Created from CDL"

    override fun close() {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Netchdf) return false

        if (location != other.location()) return false
        if (rootGroup != other.rootGroup()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + rootGroup.hashCode()
        return result
    }


}