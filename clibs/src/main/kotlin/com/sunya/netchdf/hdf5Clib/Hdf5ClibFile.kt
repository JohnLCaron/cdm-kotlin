package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h_1.H5Fclose

class Hdf5ClibFile(val filename: String) : Netchdf {
    private val header = H5Cbuilder(filename)
    private val rootGroup: Group = header.rootBuilder.build(null)

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = cdl(this)

    override fun type() = header.formatType

    override fun close() {
        val status = H5Fclose(header.file_id)
    }

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        TODO("Not yet implemented")
    }

    override fun chunkIterator(v2: Variable, section: Section?, maxElements: Int?): Iterator<ArraySection> {
        TODO("Not yet implemented")
    }
}