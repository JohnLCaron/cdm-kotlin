package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.ArraySection
import com.sunya.cdm.api.SectionL
import com.sunya.cdm.api.Variable
import com.sunya.cdm.layout.MaxChunker

internal class H5maxIterator(val h5 : H5builder, val v2: Variable, val wantSection : SectionL, maxElems: Int) : AbstractIterator<ArraySection>() {
    private val debugChunking = false

    val vinfo = v2.spObject as DataContainerVariable
    private val maxIterator  = MaxChunker(maxElems,  wantSection)

    override fun computeNext() {
        if (maxIterator.hasNext()) {
            val indexSection = maxIterator.next()
            if (debugChunking) println("  chunk=${indexSection}")

            val section = indexSection.section()
            val array = h5.readRegularData(vinfo, section)
            setNext(ArraySection(array, section))
        } else {
            done()
        }
    }
}
