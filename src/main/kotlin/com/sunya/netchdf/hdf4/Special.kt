package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import java.lang.RuntimeException

// p 147-150
class SpecialChunked(raf : OpenFile, state : OpenFileState) {
    val version: Byte
    val flag: Byte
    val chunk_tbl_tag: Short
    val chunk_tbl_ref: Short
    val head_len : Int
    val elem_tot_length : Int
    val chunk_size : Int
    val nt_size : Int
    val ndims : Int
    val dim_length: IntArray
    val chunk_length: IntArray
    val dim_flag = mutableListOf<ByteArray>() // ndims x 4, 4 flags for each dimension
    val isCompressed = false
    val sp_tag_desc: Short  // SPECIAL_XXX constant
    val fill_value: ByteArray
    val sp_tag_header: ByteArray

    internal var dataChunks: MutableList<DataChunk>? = null

    init {
        head_len = raf.readInt(state)
        version = raf.readByte(state)
        state.pos += 3
        flag = raf.readByte(state)
        elem_tot_length = raf.readInt(state)
        chunk_size = raf.readInt(state)
        nt_size = raf.readInt(state)
        chunk_tbl_tag = raf.readShort(state)
        chunk_tbl_ref = raf.readShort(state)
        state.pos += 4
        ndims = raf.readInt(state)
        dim_length = IntArray(ndims)
        chunk_length = IntArray(ndims)
        for (i in 0 until ndims) {
            dim_flag.add(raf.readBytes(state, 4))
            dim_length[i] = raf.readInt(state)
            chunk_length[i] = raf.readInt(state)
        }
        val fill_val_numtype: Int = raf.readInt(state)
        fill_value = raf.readBytes(state, fill_val_numtype)
        sp_tag_desc = raf.readShort(state)
        val sp_header_len: Int = raf.readInt(state)
        sp_tag_header = raf.readBytes(state, sp_header_len)
    }

    internal fun getDataChunks(h4: H4builder, ncfile: Netcdf): List<DataChunk> {
        if (dataChunks == null) {
            dataChunks = mutableListOf<DataChunk>()

            /* TODO read the chunk table - stored as a Structure in the data
            val chunkTableTag = h4.tagidMap[H4builder.tagid(chunk_tbl_ref, chunk_tbl_tag)] as TagVH
            val s = h4.makeChunkVariable(ncfile, chunkTableTag)
                ?: throw IllegalStateException("cant parse $chunkTableTag")
            val sdataArray: StructureDataArray = s.readArray() as StructureDataArray

            // construct the chunks
            val members: StructureMembers = sdataArray.getStructureMembers()
            val originM: StructureMembers.Member = members.findMember("origin")
            val tagM: StructureMembers.Member = members.findMember("chk_tag")
            val refM: StructureMembers.Member = members.findMember("chk_ref")
            val n = sdataArray.getSize() as Int
            for (i in 0 until n) {
                val sdata: StructureData = sdataArray.get(i)
                val origin = sdata.getMemberData(originM) as Array<Int>
                val tag = sdata.getMemberData(tagM).getScalar() as Short
                val ref = sdata.getMemberData(refM).getScalar() as Short
                val data: TagData? = h4.tagMap[H4builder.tagid(ref, tag)] as TagData?
                dataChunks!!.add(DataChunk(origin, chunk_length, data))
                data.used = true
                if (data.compress != null) isCompressed = true
            }

             */
        }
        return dataChunks!!
    }

    fun detail(): String {
        val sbuff = StringBuilder("SPECIAL_CHUNKED ")
        sbuff.append(" head_len=").append(head_len).append(" version=").append(version.toInt()).append(" special =")
            .append(flag.toInt())
            .append(" elem_tot_length=").append(elem_tot_length)
        sbuff.append(" chunk_size=").append(chunk_size).append(" nt_size=").append(nt_size)
            .append(" chunk_tbl_tag=")
            .append(chunk_tbl_tag.toInt()).append(" chunk_tbl_ref=").append(chunk_tbl_ref.toInt())
        sbuff.append("\n flag  dim  chunk\n")
        for (i in 0 until ndims) sbuff.append(" ").append(dim_flag[i][2].toInt()).append(",")
            .append(dim_flag[i][3].toInt()).append(" ").append(
                dim_length[i]
            )
            .append(" ").append(chunk_length[i]).append("\n")
        sbuff.append(" special=").append(sp_tag_desc.toInt()).append(" val=")
        for (b: Byte in sp_tag_header) {
            sbuff.append(" ").append(b.toInt())
        }
        return sbuff.toString()
    }
}

internal class DataChunk(
    originA: Array<Int>,
    chunk_length: IntArray,
    val data: TagData
) {
    val origin: IntArray

    init {
        // origin is in units of chunks - convert to indices
        require(originA.size == chunk_length.size)
        val origin = IntArray(chunk_length.size)
        for (i in origin.indices) {
            origin[i] = originA[i] * chunk_length[i]
        }
        this.origin = origin
    }
}

// p 151
internal class SpecialComp(raf : OpenFile, state : OpenFileState) {
    val version: Short
    val model_type: Short
    val compress_type: Int
    val data_ref: Short
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
        data_ref = raf.readShort(state)
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
            throw RuntimeException("unknown compress_type $compress_type")
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
            .append(" link_ref=").append(data_ref.toInt())
        sbuff.append(" model_type=").append(model_type.toInt()).append(" compress_type=")
            .append(compress_type.toInt())
        if (compress_type == TagEnum.COMP_CODE_NBIT) {
            sbuff.append(" nt=").append(nt).append(" signFlag=").append(signFlag.toInt()).append(" fillValue=")
                .append(fillValue.toInt())
                .append(" startBit=").append(startBit).append(" bitLength=").append(bitLength)
        } else if (compress_type == TagEnum.COMP_CODE_DEFLATE) {
            sbuff.append(" deflateLevel=").append(deflateLevel.toInt())
        }
        return sbuff.toString()
    }
}

// p 145
internal class SpecialLinked(raf : OpenFile, state : OpenFileState) {
    val length : Int
    val first_len : Int
    val blk_len: Short
    val num_blk: Short
    val link_ref: Short
    var linkedDataBlocks: List<TagLinkedBlock>? = null

    init {
        length = raf.readInt(state)
        first_len = raf.readInt(state)
        blk_len = raf.readShort(state) // note size wrong in doc
        num_blk = raf.readShort(state) // note size wrong in doc
        link_ref = raf.readShort(state)
    }

    fun getLinkedDataBlocks(h4 : H4builder): List<TagLinkedBlock> {
        if (linkedDataBlocks == null) {
            val dataBlocks = mutableListOf<TagLinkedBlock>()
            var next = link_ref // (short) (link_ref & 0x3FFF);
            while (next.toInt() != 0) {
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

    fun detail(): String {
        return ("SPECIAL_LINKED length=" + length + " first_len=" + first_len + " blk_len=" + blk_len + " num_blk="
                + num_blk + " link_ref=" + link_ref)
    }
}