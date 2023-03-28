package com.sunya.netchdf.hdf4

import com.sunya.cdm.array.ArrayInt
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState

// p 152: chunked element description record
class SpecialChunked(raf : OpenFile, state : OpenFileState) {
    val sp_tag_head_len : Int
    val version: Byte
    val specialnessFlag: Byte
    val elem_tot_length : Int // Valid logical length of the entire element (4 bytes). The logical physical
                            // length is this value multiplied by nt_size. The actual physical length
                            // used for storage can be greater than the dataset size due to the presence of
                            // ghost areas in chunks. Partial chunks are not distinguished from regular chunks.
    val chunk_size : Int // Logical size of data chunks
    val nt_size : Int // Number type size, i.e the size of the data type
    val chunk_tbl_tag: Int // Tag for the chunk table, i.e. the Vdata
    val chunk_tbl_ref: Int // Reference number for the chunk table, i.e. the Vdata
    val ndims : Int // Number type size, i.e the size of the data type
    val dimFlags = mutableListOf<ByteArray>() // 4 bytes for each dimension
    val dimLength: IntArray // Current length of this dimension
    val chunkLength: IntArray // Length of the chunk along this dimension
    val fill_value: ByteArray
    val sp_tag_desc: Short  // SPECIAL_XXX constant
    val sp_tag_header: ByteArray

    var isCompressed = false // dont know if its compressed until we read data
    internal var dataChunks: List<SpecialDataChunk>? = null

    init {
        sp_tag_head_len = raf.readInt(state)
        version = raf.readByte(state)
        state.pos += 3
        specialnessFlag = raf.readByte(state) // Only the bottom 8 bits are currently used.
        elem_tot_length = raf.readInt(state)
        chunk_size = raf.readInt(state)
        nt_size = raf.readInt(state)
        chunk_tbl_tag = raf.readShort(state).toUShort().toInt()
        chunk_tbl_ref = raf.readShort(state).toUShort().toInt()
        state.pos += 4 // sp_taf, sp_ref (not used)
        ndims = raf.readInt(state)
        dimLength = IntArray(ndims)
        chunkLength = IntArray(ndims)
        for (i in 0 until ndims) {
            dimFlags.add(raf.readBytes(state, 4))
            dimLength[i] = raf.readInt(state)
            chunkLength[i] = raf.readInt(state)
        }
        val fill_val_numtype: Int = raf.readInt(state)
        fill_value = raf.readBytes(state, fill_val_numtype)
        sp_tag_desc = raf.readShort(state)
        val sp_header_len: Int = raf.readInt(state)
        sp_tag_header = raf.readBytes(state, sp_header_len)
    }

    internal fun getDataChunks(h4file: Hdf4File): List<SpecialDataChunk> {
        if (dataChunks == null) {
            val chunkTableTag = h4file.header.tagidMap[H4builder.tagid(chunk_tbl_ref, chunk_tbl_tag)] as TagVH
            val vinfo = Vinfo(chunkTableTag.refno) // so we dont have to create a phone variable
            vinfo.tagData = h4file.header.tagidMap[H4builder.tagid(chunkTableTag.refno, TagEnum.VS.code)] as TagData
            vinfo.elemSize = chunkTableTag.ivsize
            vinfo.setLayoutInfo(h4file)
            val shape = intArrayOf (chunkTableTag.nelems)

            val members = chunkTableTag.readStructureMembers()
            val sdataArray = readStructureDataArray(h4file.header, vinfo, shape, members)
            // println(sdataArray)

            // reading in the entire chunkList
            val originM = members.find{ it.name == "origin" }!!
            val tagM = members.find{ it.name == "chk_tag" }!!
            val refM = members.find{ it.name == "chk_ref" }!!
            val chunkList = mutableListOf<SpecialDataChunk>()
            for (sdata in sdataArray) {
                val wtf = originM.value(sdata)
                val origin : IntArray = when (wtf) {
                    is Int -> intArrayOf(wtf)
                    is ArrayInt -> {
                        val iter = wtf.iterator()
                        IntArray(wtf.nelems) { iter.next() }
                    }
                    else -> throw RuntimeException("origin must be Integer")
                }

                val tag = tagM.value(sdata) as UShort
                val ref = refM.value(sdata) as UShort
                val data  = h4file.header.tagidMap[H4builder.tagid(ref.toInt(), tag.toInt())]
                if (data != null) { // missing?
                    val dataAs = data as TagData
                    isCompressed = (dataAs.compress != null) // dont know if its compressed or not until you read the data. barf.
                    chunkList.add(SpecialDataChunk(origin, chunkLength, dataAs))
                    data.isUsed = true
                }
            }
            dataChunks = chunkList
        }
        return dataChunks!!
    }

    fun detail(): String {
        val sbuff = StringBuilder("SPECIAL_CHUNKED ")
        sbuff.append(" version=").append(version.toInt()).append(" special =")
            .append(specialnessFlag.toInt())
            .append(" elem_tot_length=").append(elem_tot_length)
        sbuff.append(" chunk_size=").append(chunk_size).append(" nt_size=").append(nt_size)
            .append(" chunk_tbl_tag=")
            .append(chunk_tbl_tag).append(" chunk_tbl_ref=").append(chunk_tbl_ref)
        sbuff.append("\n flag  dim  chunk\n")
        for (i in 0 until ndims) sbuff.append(" ").append(dimFlags[i][2].toInt()).append(",")
            .append(dimFlags[i][3].toInt()).append(" ").append(
                dimLength[i]
            )
            .append(" ").append(chunkLength[i]).append("\n")
        sbuff.append(" special=").append(sp_tag_desc.toInt()).append(" val=")
        for (b: Byte in sp_tag_header) {
            sbuff.append(" ").append(b.toInt())
        }
        return sbuff.toString()
    }

    override fun toString(): String {
        return "SpecialChunked(chunk_size=$chunk_size, ndims=$ndims, dim_length=${dimLength.contentToString()}, chunk_length=${chunkLength.contentToString()}, isCompressed=$isCompressed)"
    }
}

internal class SpecialDataChunk(
    originA: IntArray,
    chunkLength: IntArray,
    val data: TagData,
) {
    val origin: IntArray
    init {
        // origin is in units of chunks - convert to indices
        require(originA.size == chunkLength.size)
        val origin = IntArray(chunkLength.size)
        repeat(chunkLength.size) {
            origin[it] = originA[it] * chunkLength[it]
        }
        this.origin = origin
    }
}

private const val warn = false

// p 151
internal class SpecialComp(raf : OpenFile, state : OpenFileState) {
    val version: Short
    val model_type: Short
    val compress_type: Int
    val data_ref: Int
    val uncomp_length : Int
    var dataTag: TagData? = null

    // compress_type == 2
    val signFlag: Short
    val fillValue: Short
    val nt : Int
    val startBit : Int
    val bitLength : Int

    // compress_type == 4
    val deflateLevel: Short

    init {
        version = raf.readShort(state)
        uncomp_length = raf.readInt(state)
        data_ref = raf.readShort(state).toUShort().toInt()
        model_type = raf.readShort(state)
        compress_type = raf.readShort(state).toInt()
        if (compress_type == TagEnum.COMP_CODE_NBIT) {
            nt = raf.readInt(state)
            signFlag = raf.readShort(state)
            fillValue = raf.readShort(state)
            startBit = raf.readInt(state)
            bitLength = raf.readInt(state)

            deflateLevel = -1
        } else if (compress_type == TagEnum.COMP_CODE_DEFLATE) {
            deflateLevel = raf.readShort(state)

            nt = -1
            signFlag = -1
            fillValue = -1
            startBit = -1
            bitLength = -1
        } else {
            // compress_type = 0 happens
            if (warn) println("unimplemented compress_type=$compress_type")
            deflateLevel = -1
            nt = -1
            signFlag = -1
            fillValue = -1
            startBit = -1
            bitLength = -1
        }
    }

    fun getDataTag(h4 : H4builder): TagData {
        if (dataTag == null) {
            dataTag = h4.tagidMap[H4builder.tagid(data_ref, TagEnum.COMPRESSED.code)] as TagData
            if (dataTag == null) throw IllegalStateException("TagCompress not found for " + detail())
            dataTag!!.isUsed = true
        }
        return dataTag!!
    }

    fun detail(): String {
        val sbuff = StringBuilder("SPECIAL_COMP ")
        sbuff.append(" version=").append(version.toInt()).append(" uncompressed length =").append(uncomp_length)
            .append(" link_ref=").append(data_ref)
        sbuff.append(" model_type=").append(model_type.toInt()).append(" compress_type=")
            .append(compress_type)
        if (compress_type == TagEnum.COMP_CODE_NBIT) {
            sbuff.append(" nt=").append(nt).append(" signFlag=").append(signFlag.toInt()).append(" fillValue=")
                .append(fillValue.toInt())
                .append(" startBit=").append(startBit).append(" bitLength=").append(bitLength)
        } else if (compress_type == TagEnum.COMP_CODE_DEFLATE) {
            sbuff.append(" deflateLevel=").append(deflateLevel.toInt())
        }
        return sbuff.toString()
    }

    override fun toString(): String {
        return "SpecialComp(compress_type=$compress_type, uncomp_length=$uncomp_length, fillValue=$fillValue)"
    }
}

// p 145
internal class SpecialLinked(raf : OpenFile, state : OpenFileState) {
    val length : Int
    val first_len : Int
    val blk_len: Short
    val num_blk: Short
    val link_ref: Int
    var linkedDataBlocks: List<TagLinkedBlock>? = null

    init {
        length = raf.readInt(state)
        first_len = raf.readInt(state)
        blk_len = raf.readShort(state) // note size wrong in doc
        num_blk = raf.readShort(state) // note size wrong in doc
        link_ref = raf.readShort(state).toUShort().toInt()
    }

    fun getLinkedDataBlocks(h4 : H4builder): List<TagLinkedBlock> {
        if (linkedDataBlocks == null) {
            val dataBlocks = mutableListOf<TagLinkedBlock>()
            var next = link_ref // (short) (link_ref & 0x3FFF);
            while (next != 0) {
                val tag: TagLinkedBlock =
                    h4.tagidMap.get(H4builder.tagid(next, TagEnum.LINKED.code)) as TagLinkedBlock?
                        ?: throw IllegalStateException("TagLinkedBlock not found for " + detail())
                tag.isUsed = true
                tag.read2(h4, num_blk.toInt(), dataBlocks)
                next = tag.next_ref // (short) (tag.next_ref & 0x3FFF);
            }
            linkedDataBlocks = dataBlocks
        }
        return linkedDataBlocks!!
    }

    override fun toString(): String {
        return detail()
    }

    fun detail(): String {
        return ("SpecialLinked length=$length first_len=$first_len blk_len=$blk_len num_blk=$num_blk link_ref=$link_ref")
    }
}