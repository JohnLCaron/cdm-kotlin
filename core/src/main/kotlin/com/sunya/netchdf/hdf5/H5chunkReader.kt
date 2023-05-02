package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.SectionL
import com.sunya.cdm.api.Variable
import com.sunya.cdm.api.toIntArray
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.nio.ByteBuffer

internal class H5chunkReader(val h5 : H5builder) {
    private val debugChunking = false

    internal fun readChunkedData(v2: Variable, wantSection : SectionL) : ArrayTyped<*> {
        val vinfo = v2.spObject as DataContainerVariable
        val h5type = vinfo.h5type

        val elemSize = vinfo.storageDims[vinfo.storageDims.size - 1].toInt() // last one is always the elements size
        val datatype = vinfo.h5type.datatype()

        val wantSpace = IndexSpace(wantSection)
        val sizeBytes = wantSpace.totalElements * elemSize
        if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
            throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
        }
        val bb = ByteBuffer.allocate(sizeBytes.toInt())
        bb.order(vinfo.h5type.endian)

        val btreeNew =  BTree1(h5, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
        val tiledData = H5TiledData(btreeNew)
        val filters = H5filters(v2.name, vinfo.mfp, vinfo.h5type.endian)
        if (debugChunking) println(" readChunkedData tiles=${tiledData.tiling}")

        var transferChunks = 0
        val state = OpenFileState(0L, vinfo.h5type.endian)
        for ((count, dataChunk : BTree1.DataChunkEntry) in tiledData.dataChunks(wantSpace).withIndex()) { // : Iterable<BTree1New.DataChunkEntry>
            val dataSection = IndexSpace(v2.rank, dataChunk.key.offsets, vinfo.storageDims)
            val chunker = Chunker(dataSection, wantSpace) // each DataChunkEntry has its own Chunker iteration
            if (dataChunk.isMissing()) {
                if (debugChunking) println("   missing ${dataChunk.show(tiledData.tiling)}")
                chunker.transferMissing(vinfo.fillValue, datatype, elemSize, bb)
            } else {
                if (debugChunking) println("   chunk=${dataChunk.show(tiledData.tiling)}")
                state.pos = dataChunk.childAddress
                val chunkData = h5.raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
                val filteredData = filters.apply(chunkData, dataChunk)
                chunker.transfer(filteredData, elemSize, bb)
                transferChunks += chunker.transferChunks
            }
        }

        bb.position(0)
        bb.limit(bb.capacity())
        bb.order(vinfo.h5type.endian)
        val shape = wantSpace.shape.toIntArray()

        return if (h5type.datatype5 == Datatype5.Vlen) {
            h5.processVlenIntoArray(h5type, shape, bb, wantSpace.totalElements.toInt(), elemSize)
        } else {
            h5.processDataIntoArray(bb, datatype, shape, h5type, elemSize)
        }
    }
}

// Chunked data apparently has heapIds directly, not addresses of heapIds. Go figure.
internal fun H5builder.processVlenIntoArray(h5type: H5TypeInfo, shape: IntArray, bb: ByteBuffer, nelems: Int, elemSize : Int): ArrayTyped<*> {
    val h5heap = H5heap(this)

    if (h5type.isVlenString) {
        val sarray = mutableListOf<String>()
        for (i in 0 until nelems) {
            val sval = h5heap.readHeapString(bb, i * elemSize)
            sarray.add(sval ?: "")
        }
        return ArrayString(shape, sarray)

    } else {
        val base = h5type.base!!
        if (base.datatype5 == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            for (i in 0 until nelems) {
                val heapId = h5heap.readHeapIdentifier(bb, i * elemSize)
                val vlenArray = h5heap.getHeapDataArray(heapId, Datatype.LONG, base.endian)
                // LOOK require vlenArray is Array<Long>
                val refsArray = this.convertReferencesToDataObjectName(vlenArray as Array<Long>)
                for (s in refsArray) {
                    refsList.add(s)
                }
            }
            return ArrayString(shape, refsList)
        }

        // general case is to read an array of vlen objects
        // each vlen generates an Array of type baseType
        val listOfArrays = mutableListOf<Array<*>>()
        val readDatatype = base.datatype()
        for (i in 0 until nelems) {
            val heapId = h5heap.readHeapIdentifier(bb, i * elemSize)
            val vlenArray = h5heap.getHeapDataArray(heapId, readDatatype, base.endian)
            // LOOK require vlenArray is Array<T>
            listOfArrays.add(vlenArray)
        }
        return ArrayVlen(shape, listOfArrays.toList(), readDatatype)
    }
}