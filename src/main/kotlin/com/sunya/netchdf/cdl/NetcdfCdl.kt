package com.sunya.netchdf.cdl

import com.sunya.cdm.api.Group
import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArrayTyped

// A Netcdf created from CDL, no backing file store
class NetcdfCdl(val location : String, val rootGroup : Group) : Netcdf {
    override fun rootGroup() = rootGroup

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    override fun location() = location
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = "Created from CDL"

    override fun close() {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Netcdf) return false

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