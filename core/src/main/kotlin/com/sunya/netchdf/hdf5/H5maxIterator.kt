package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.ArraySection
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.layout.MaxChunker

internal class H5maxIterator<T>(val h5 : H5builder, val v2: Variable<T>, val wantSection : Section, maxElems: Int) : AbstractIterator<ArraySection<T>>() {
    private val debugChunking = false
    private val maxIterator  = MaxChunker(maxElems,  wantSection)

    override fun computeNext() {
        if (maxIterator.hasNext()) {
            val indexSection = maxIterator.next()
            if (debugChunking) println("  chunk=${indexSection}")

            val section = indexSection.section(v2.shape)
            val array = h5.readRegularData(v2.spObject as DataContainerVariable, v2.datatype, section)
            setNext(ArraySection(array, section))
        } else {
            done()
        }
    }
}
