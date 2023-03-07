package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Group
import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.Iosp
import com.sunya.cdm.iosp.OpenFile
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class Hdf4File(val filename : String) : Iosp, Netcdf {
    private val raf: OpenFile = OpenFile(filename)
    private val header: H4builder
    private val rootGroup : Group
    var valueCharset : Charset = StandardCharsets.UTF_8

    init {
        header = H4builder(raf, valueCharset)
        rootGroup = header.rootBuilder.build(null)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl(strict: Boolean) = com.sunya.cdm.api.cdl(this, strict)
    override fun type() = "hdf4   "

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val filledSection = Section.fill(section, v2.shape)
        //if (v2 is Structure) {
       //     return readStructureDataArray(v 2as Structure?, filledSection)
        //}

        return readDataObject(v2, filledSection)
    }

    private fun readDataObject(v: Variable, section: Section): ArrayTyped<*> {
        val vinfo = v.spObject as Vinfo
        val dataType = v.datatype
        vinfo.setLayoutInfo(header, this) // make sure needed info is present

        /*
        if (vinfo.hasNoData) {
            return if (vinfo.fillValue == null) IospArrayHelper.makePrimitiveArray(
                section.computeSize().toInt(),
                dataType
            ) else IospArrayHelper.makePrimitiveArray(
                section.computeSize().toInt(), dataType, vinfo.fillValue
            )
        }
        if (!vinfo.isCompressed) {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val layout: Layout = LayoutRegular(vinfo.start, vinfo.getElementSize(), v.getShape(), section)
                return IospArrayHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, null)
            } else if (vinfo.isLinked) {
                val layout: Layout =
                    LayoutSegmented(vinfo.segPos, vinfo.segSize, vinfo.getElementSize(), v.getShape(), section)
                return IospArrayHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, null)
            } else if (vinfo.isChunked) {
                val chunkIterator: ucar.nc2.internal.iosp.hdf4.H4iosp.H4ChunkIterator =
                    ucar.nc2.internal.iosp.hdf4.H4iosp.H4ChunkIterator(vinfo)
                val layout: Layout = LayoutTiled(chunkIterator, vinfo.chunkSize, vinfo.getElementSize(), section)
                return IospArrayHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, null)
            }
        } else {
            if (!vinfo.isLinked && !vinfo.isChunked) {
                val index: Layout = LayoutRegular(0, vinfo.getElementSize(), v.getShape(), section)
                val `is`: InputStream = getCompressedInputStream(vinfo)
                val dataSource = PositioningDataInputStream(`is`)
                return IospArrayHelper.readDataFill(dataSource, index, dataType, vinfo.fillValue)
            } else if (vinfo.isLinked) {
                val index: Layout = LayoutRegular(0, vinfo.getElementSize(), v.getShape(), section)
                val `is`: InputStream = getLinkedCompressedInputStream(vinfo)
                val dataSource = PositioningDataInputStream(`is`)
                return IospArrayHelper.readDataFill(dataSource, index, dataType, vinfo.fillValue)
            } else if (vinfo.isChunked) {
                val chunkIterator: LayoutBBTiled.DataChunkIterator =
                    ucar.nc2.internal.iosp.hdf4.H4iosp.H4CompressedChunkIterator(vinfo)
                val layout: LayoutBB = LayoutBBTiled(chunkIterator, vinfo.chunkSize, vinfo.getElementSize(), section)
                return IospArrayHelper.readDataFill(layout, dataType, vinfo.fillValue)
            }
        } */
        throw IllegalStateException()
    }

    /*
     * Structures must be fixed sized.
     *
     * @param s the record structure
     * @param section the record range to read
     * @return an Array of StructureData, with all the data read in.
     * @throws IOException on error
     * @throws InvalidRangeException if invalid section
    @Throws(IOException::class, InvalidRangeException::class)
    private fun readStructureDataArray(s: Structure, section: Section): Array<StructureData?>? {
        val vinfo: H4header.Vinfo = s.getSPobject() as H4header.Vinfo
        vinfo.setLayoutInfo(this.ncfile) // make sure needed info is present
        val recsize: Int = vinfo.elemSize

        // create the StructureMembers
        val membersb: ucar.array.StructureMembers.Builder = s.makeStructureMembersBuilder()
        for (m in membersb.getStructureMembers()) {
            val v2: Variable = s.findVariable(m.getName())
            val minfo: Minfo = v2.getSPobject() as Minfo
            m.setOffset(minfo.offset)
        }
        membersb.setStructureSize(recsize)
        val nrecs = section.computeSize().toInt()
        val result = ByteArray((nrecs * recsize))
        if (!vinfo.isLinked && !vinfo.isCompressed) {
            val layout: Layout = LayoutRegular(vinfo.start, recsize, s.getShape(), section)
            IospArrayHelper.readData(raf, layout, ArrayType.STRUCTURE, result, null)

            // option 2
        } else if (vinfo.isLinked && !vinfo.isCompressed) {
            val `is`: InputStream = ucar.nc2.internal.iosp.hdf4.H4iosp.LinkedInputStream(vinfo)
            val dataSource = PositioningDataInputStream(`is`)
            val layout: Layout = LayoutRegular(0, recsize, s.getShape(), section)
            IospArrayHelper.readData(dataSource, layout, ArrayType.STRUCTURE, result)
        } else if (!vinfo.isLinked && vinfo.isCompressed) {
            val `is`: InputStream = getCompressedInputStream(vinfo)
            val dataSource = PositioningDataInputStream(`is`)
            val layout: Layout = LayoutRegular(0, recsize, s.getShape(), section)
            IospArrayHelper.readData(dataSource, layout, ArrayType.STRUCTURE, result)
        } else if (vinfo.isLinked && vinfo.isCompressed) {
            val `is`: InputStream = getLinkedCompressedInputStream(vinfo)
            val dataSource = PositioningDataInputStream(`is`)
            val layout: Layout = LayoutRegular(0, recsize, s.getShape(), section)
            IospArrayHelper.readData(dataSource, layout, ArrayType.STRUCTURE, result)
        } else {
            throw IllegalStateException()
        }
        val members: StructureMembers = membersb.build()
        val storage: Storage<StructureData> = StructureDataStorageBB(
            members, ByteBuffer.wrap(result),
            section.computeSize().toInt()
        )
        return StructureDataArray(members, section.getShape(), storage)
    }

    @Throws(IOException::class)
    private fun getCompressedInputStream(vinfo: H4header.Vinfo): InputStream? {
        // probably could construct an input stream from a channel from a raf for now, just read it in.
        val buffer = ByteArray(vinfo.length)
        raf.seek(vinfo.start)
        raf.readFully(buffer)
        val `in` = ByteArrayInputStream(buffer)
        return InflaterInputStream(`in`)
    }

    private fun getLinkedCompressedInputStream(vinfo: H4header.Vinfo): InputStream? {
        return InflaterInputStream(LinkedInputStream(vinfo))
    }

    private class LinkedInputStream : InputStream {
        var buffer: ByteArray
        var nsegs: Int
        var segPosA: LongArray
        var segSizeA: IntArray
        var segno = -1
        var segpos = 0
        var segSize = 0

        // H4header.Vinfo vinfo;
        internal constructor(vinfo: H4header.Vinfo) {
            segPosA = vinfo.segPos
            segSizeA = vinfo.segSize
            nsegs = segSizeA.size
        }

        internal constructor(linked: SpecialLinked) {
            val linkedBlocks = linked.getLinkedDataBlocks()
            nsegs = linkedBlocks!!.size
            segPosA = LongArray(nsegs)
            segSizeA = IntArray(nsegs)
            var count = 0
            for (tag in linkedBlocks) {
                segPosA[count] = tag.offset.toLong()
                segSizeA[count] = tag.length
                count++
            }
        }

        @Throws(IOException::class)
        private fun readSegment(): Boolean {
            segno++
            if (segno == nsegs) return false
            segSize = segSizeA[segno]
            while (segSize == 0) { // for some reason may have a 0 length segment
                segno++
                if (segno == nsegs) return false
                segSize = segSizeA[segno]
            }
            buffer = ByteArray(segSize) // Look: could do this in buffer size 4096 to save memory
            raf.seek(segPosA[segno])
            raf.readFully(buffer)
            segpos = 0
            return true
        }

        @Throws(IOException::class)
        override fun read(): Int {
            if (segpos == segSize) {
                val ok = readSegment()
                if (!ok) return -1
            }
            val b = buffer[segpos].toInt() and 0xff
            segpos++
            return b
        }
    }

    private class H4ChunkIterator internal constructor(vinfo: H4header.Vinfo) : LayoutTiled.DataChunkIterator {
        val chunks: List<H4header.DataChunk>
        var chunkNo = 0

        init {
            chunks = vinfo.chunks
        }

        operator fun hasNext(): Boolean {
            return chunkNo < chunks.size
        }

        operator fun next(): LayoutTiled.DataChunk {
            val chunk: H4header.DataChunk = chunks[chunkNo]
            val chunkData: TagData = chunk.data
            chunkNo++
            return DataChunk(chunk.origin, chunkData.offset)
        }
    }

    private class H4CompressedChunkIterator internal constructor(vinfo: H4header.Vinfo) :
        LayoutBBTiled.DataChunkIterator {
        val chunks: List<H4header.DataChunk>
        var chunkNo = 0

        init {
            chunks = vinfo.chunks
        }

        operator fun hasNext(): Boolean {
            return chunkNo < chunks.size
        }

        operator fun next(): LayoutBBTiled.DataChunk {
            val chunk: H4header.DataChunk = chunks[chunkNo]
            val chunkData: TagData = chunk.data
            Preconditions.checkArgument(chunkData.ext_type === TagEnum.SPECIAL_COMP)
            chunkNo++
            return DataChunk(chunk.origin, chunkData.compress)
        }
    }

    private class DataChunk internal constructor(// offset index of this chunk, reletive to entire array
        val offset: IntArray, private val compress: SpecialComp?
    ) : LayoutBBTiled.DataChunk {
        private var bb: ByteBuffer? = null // the data is placed into here

        @Throws(IOException::class)
        fun getByteBuffer(expectedSizeBytes: Int): ByteBuffer? {
            if (bb == null) {
                // read compressed data in
                val cdata = compress!!.getDataTag()
                val `in`: InputStream

                // compressed data stored in one place
                `in` = if (cdata.linked == null) {
                    val cbuffer = ByteArray(cdata.length)
                    raf.seek(cdata.offset)
                    raf.readFully(cbuffer)
                    ByteArrayInputStream(cbuffer)
                } else { // or compressed data stored in linked storage
                    LinkedInputStream(cdata.linked)
                }

                // uncompress it
                bb = if (compress.compress_type === TagEnum.COMP_CODE_DEFLATE) {
                    // read the stream in and uncompress
                    val zin: InputStream = InflaterInputStream(`in`)
                    val out = ByteArrayOutputStream(compress.uncomp_length)
                    IO.copy(zin, out)
                    val buffer = out.toByteArray()
                    ByteBuffer.wrap(buffer)
                } else if (compress.compress_type === TagEnum.COMP_CODE_NONE) {
                    // just read the stream in
                    val out = ByteArrayOutputStream(compress.uncomp_length)
                    IO.copy(`in`, out)
                    val buffer = out.toByteArray()
                    ByteBuffer.wrap(buffer)
                } else {
                    throw IllegalStateException("unknown compression type =" + compress.compress_type)
                }
            }
            return bb
        }
    }

     */
}
