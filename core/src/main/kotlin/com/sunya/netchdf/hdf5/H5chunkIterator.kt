package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.ArraySection
import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.IndexSpace
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

    val chunkIterator : Iterator<BTree1.DataChunkEntry>

    var count = 0
    var transferChunks = 0

    init {
        vinfo = v2.spObject as DataContainerVariable

        h5type = vinfo.h5type
        elemSize = vinfo.storageDims.get(vinfo.storageDims.size - 1) // last one is always the elements size
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

    fun getaPair(dataChunk : BTree1.DataChunkEntry) : ArraySection {
        val dataSection = IndexSpace(dataChunk.key.offsets, vinfo.storageDims)
        val section = dataSection.section()

        val bb = if (dataChunk.isMissing()) {
            val sizeBytes = section.computeSize() * elemSize
            val bbmissing = ByteBuffer.allocate(sizeBytes.toInt())
            transferMissing(vinfo.fillValue, datatype, section.computeSize().toInt(), bbmissing)
            bbmissing
        } else {
            state.pos = dataChunk.childAddress
            h5.raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
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

        /*
        if (h5type.hdfType == Datatype5.Compound) {
            val members = (datatype.typedef as CompoundTypedef).members
            val sdataArray =  ArrayStructureData(shape, bb, elemSize, members)
            return ArraySection(h5.processCompoundData(sdataArray, h5type.endian), section)
        }

        if (h5type.hdfType == Datatype5.Vlen) {
            return ArraySection(h5.processChunkedVlen(h5type, shape, bb, dataSection.totalElements.toInt(), elemSize), section)
        }

        var result = when (datatype) {
            Datatype.BYTE -> ArrayByte(shape, bb)
            Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
            Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
            Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
            Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
            Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
            Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
            Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
            Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
            Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
            Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }
        if (h5type.hdfType == Datatype5.String) {
            result =  (result as ArrayUByte).makeStringsFromBytes()
        }
        if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
            result = ArrayString(shape, h5.convertReferencesToDataObjectName(result as ArrayLong))
        }
        return ArraySection(result, section)

         */
    }
}
