package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.ArraySection
import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.IndexSpace
import com.sunya.cdm.layout.transfer
import com.sunya.cdm.layout.transferMissing
import java.nio.ByteBuffer

internal class H5chunkIterator(val h5 : H5builder, val v2: Variable, val wantSection : Section) : AbstractIterator<ArraySection>() {

    private val debugChunking = false

    val vinfo : DataContainerVariable
    val h5type : H5TypeInfo
    val elemSize : Int
    val datatype : Datatype
    val tiledData : TiledH5Data
    val filters : H5filters
    val state : OpenFileState

    private val chunkIterator : Iterator<BTree1.DataChunkEntry>

    init {
        vinfo = v2.spObject as DataContainerVariable

        h5type = vinfo.h5type
        elemSize = vinfo.storageDims[vinfo.storageDims.size - 1] // last one is always the elements size
        datatype = h5type.datatype(h5)

        val wantSpace = IndexSpace(wantSection)
        val sizeBytes = wantSpace.totalElements * elemSize
        if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
            throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
        }

        val btreeNew = BTree1(h5, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
        tiledData = TiledH5Data(btreeNew)
        filters = H5filters(v2.name, vinfo.mfp, h5type.endian)
        if (debugChunking) println(" ${tiledData.tiling}")

        state = OpenFileState(0L, h5type.endian)
        chunkIterator = tiledData.dataChunks(wantSpace).iterator()
    }

    override fun computeNext() {
        if (chunkIterator.hasNext()) {
            setNext(getaPair(chunkIterator.next()))
        } else {
            done()
        }
    }

    private fun getaPair(dataChunk : BTree1.DataChunkEntry) : ArraySection {
        val dataSection = IndexSpace(dataChunk.key.offsets, vinfo.storageDims)
        val section = dataSection.section()

        val bb = if (dataChunk.isMissing()) {
            val sizeBytes = section.computeSize() * elemSize
            val bbmissing = ByteBuffer.allocate(sizeBytes.toInt())
            transferMissing(vinfo.fillValue, datatype, section.computeSize().toInt(), bbmissing)
            bbmissing
        } else {
            state.pos = dataChunk.childAddress
            val chunkData = h5.raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
            filters.apply(chunkData, dataChunk)
        }

        bb.position(0)
        bb.limit(bb.capacity())
        bb.order(h5type.endian)
        val shape = section.shape

        return if (h5type.hdfType == Datatype5.Vlen) {
            ArraySection(h5.processVlenIntoArray(h5type, shape, bb, dataSection.totalElements.toInt(), elemSize), section)
        } else {
            ArraySection(h5.processDataIntoArray(bb, datatype, shape, h5type, elemSize), section)
        }
    }
}
