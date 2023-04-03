package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Group
import com.sunya.cdm.array.StructureMember
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.netchdf.hdf4.H4builder.Companion.tagid
import com.sunya.netchdf.hdf4.TagEnum.Companion.obsolete
import java.nio.ByteBuffer
import java.nio.ByteOrder

////////////////////////////////////////////////////////////////////////////////
// Tags
fun readTag(raf : OpenFile, state: OpenFileState): Tag {
    // read just the DD part of the tag. see p 11
    val xtag = raf.readShort(state).toUShort().toInt()
    val btag = xtag and 0x3FFF // 14 bits // basic tags are numbered 0x0001 through 0x3FFF,
    val refno = raf.readShort(state).toUShort().toInt()
    val offset = raf.readInt(state).toLong() // LOOK only 32 bit (!)
    val length = raf.readInt(state)

    when (val tagEnum = TagEnum.byCode(btag)) {
        TagEnum.LINKED -> return TagLinkedBlock(xtag, refno, offset, length)
        TagEnum.VERSION -> return TagVersion(xtag, refno, offset, length)
        TagEnum.COMPRESSED, TagEnum.CHUNK, TagEnum.SD, TagEnum.VS -> return TagData(xtag, refno, offset, length)
        TagEnum.FID, TagEnum.FD, TagEnum.SDC -> return TagText(xtag, refno, offset, length)
        TagEnum.DIL, TagEnum.DIA -> return TagAnnotate(xtag, refno, offset, length)
        TagEnum.NT -> return TagNT(xtag, refno, offset, length)
        TagEnum.ID, TagEnum.LD, TagEnum.MD -> return TagRIDimension(xtag, refno, offset, length)
        TagEnum.LUT -> return TagLookupTable(xtag, refno, offset, length)
        TagEnum.RI -> return TagRasterImage(xtag, refno, offset, length)
        TagEnum.RIG, TagEnum.NDG -> return TagDataGroup(xtag, refno, offset, length)
        TagEnum.SDD -> return TagSDD(xtag, refno, offset, length)
        // TagEnum.SDS -> return TagSDS(xtag, refno, offset, length)
        TagEnum.SDL, TagEnum.SDU, TagEnum.SDF -> return TagTextN(xtag, refno, offset, length)
        TagEnum.SDM -> return TagSDminmax(xtag, refno, offset, length)
        TagEnum.FV -> return TagFV(xtag, refno, offset, length)
        TagEnum.VH -> return TagVH(xtag, refno, offset, length)
        TagEnum.VG -> return TagVGroup(xtag, refno, offset, length)
        // wtf? 17086 -> return TagVGroup(icode, refno, offset, length)
        else -> {
            if ((xtag > 1) and !obsolete.contains(tagEnum)) {
                val wtf = TagEnum.byCode(btag)
                println(" Unknown xtag=$xtag btag=$btag refno=$refno")
            }
            return Tag(xtag, refno, offset, length)
        }
    }
}

// Tag == "Data Descriptor" (DD) and (usually) a "Data Element" that the offset/length points to
open class Tag(xtag: Int, val refno : Int, val offset : Long, val length : Int) {
    val isExtended: Boolean = (xtag and 0x4000) != 0
    val code = (xtag and 0x3FFF) // basic tag
    // var t: TagEnum = TagEnum.byCode(this.code)
    var isUsed = false
    internal var vinfo: Vinfo? = null

    // read the offset/length part of the tag. overridden by subclasses
    open fun readTag(h4 : H4builder) {
    }

    open fun detail(): String {
        return toString()
    }

    override fun toString(): String {
        return "${if (isUsed) " " else "*"} tag=${tagEnum()}  vclass='${vClass()}' refno=$refno ${if (isExtended) " EXTENDED" else ""} offset=$offset" +
                " length=$length"
    }

    fun tagEnum(): TagEnum {
        return TagEnum.byCode(this.code)
    }

    fun tagRefAndCode(): String {
        return "$refno/$code"
    }

    private fun vClass() : String {
        return when (this) {
            is TagVGroup -> this.className
            is TagVH -> this.className
            else -> ""
        }
    }
}

// TagEnum.COMPRESSED (40), TagEnum.CHUNK (61), TagEnum.SD (702), TagEnum.VS (1963)
// Combining so we just have one data object
class TagData(icode: Int, refno : Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    private var extendedTag: Int = 0
    internal var linked: SpecialLinked? = null
    internal var compress: SpecialComp? = null
    internal var chunked: SpecialChunked? = null
    var tag_len = 0 // needed?

    override fun readTag(h4 : H4builder) {
        if (isExtended) {
            val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
            extendedTag = h4.raf.readShort(state).toInt()
            when (extendedTag) {
                TagEnum.SPECIAL_COMP -> compress = SpecialComp(h4.raf, state) // TagEnum.COMPRESSED
                TagEnum.SPECIAL_CHUNKED -> chunked = SpecialChunked(h4.raf, state) // TagEnum.CHUNK
                TagEnum.SPECIAL_LINKED -> linked = SpecialLinked(h4.raf, state) // TagEnum.VS
            }
            tag_len = (state.pos - offset).toInt()
        }
    }

    override fun detail(): String {
        return if (linked != null) {
            super.detail() + " ext_tag= " + extendedTag + " tag_len= " + tag_len + " " + linked!!.detail()
        } else if (compress != null) {
            super.detail() + " ext_tag= " + extendedTag + " tag_len= " + tag_len + " " + compress!!.detail()
        } else if (chunked != null) {
            super.detail() + " ext_tag= " + extendedTag + " tag_len= " + tag_len + " " + chunked!!.detail()
        } else super.detail()
    }

    override fun toString(): String {
        return buildString {
            append (super.toString())
            if (extendedTag != 0) {
                append(" linked=$linked, compress=$compress, chunked=$chunked, tag_len=$tag_len)")
            }
        }
    }
}

// 20 p 146 Also used for data blocks, which has no next_ref! (!)
class TagLinkedBlock(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var next_ref: Int = 0
    var block_ref = IntArray(0)
    var n = 0

    fun read2(h4 : H4builder, nb: Int, dataBlocks: MutableList<TagLinkedBlock>) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        next_ref = h4.raf.readShort(state).toUShort().toInt()
        block_ref = IntArray(nb)
        for (i in 0 until nb) {
            block_ref[i] = h4.raf.readShort(state).toUShort().toInt()
            if (block_ref[i] == 0) break
            n++
        }
        for (i in 0 until n) {
            val tagid = tagid(block_ref[i], TagEnum.LINKED.code)
            val tag = h4.tagidMap[tagid] as TagLinkedBlock?
            if (tag != null) {
                tag.isUsed = true
                dataBlocks.add(tag)
            }
        }
    }

    override fun detail(): String {
        return buildString {
            append(super.detail())
            append(" next_ref= ").append(next_ref)
            append(" dataBlks= ")
            for (i in 0 until n) {
                val ref = block_ref[i]
                append(ref).append(" ")
            }
        }
    }
}

// 30
class TagVersion(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var major = 0
    var minor = 0
    var release = 0
    var name: String? = null

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        major = h4.raf.readInt(state)
        minor = h4.raf.readInt(state)
        release = h4.raf.readInt(state)
        name = h4.raf.readString(state, length - 12)
    }

    fun value(): String {
        return "$major.$minor.$release ($name)"
    }

    override fun detail(): String {
        return "${super.detail()} ${value()} ($name)"
    }
}

// 100, 101
class TagText(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var text: String = "null"

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        text = h4.raf.readString(state, length, h4.valueCharset)
    }

    override fun detail(): String {
        val t = if ((text.length < 60)) text else text.substring(0, 59)
        return super.detail() + " text= " + t
    }
}

// 104, 105
class TagAnnotate(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var text: String = "null"
    var obj_tagno: Int = 0
    var obj_refno: Int = 0

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        obj_tagno = h4.raf.readShort(state).toUShort().toInt()
        obj_refno = h4.raf.readShort(state).toUShort().toInt()
        text = h4.raf.readString(state, length - 4, h4.valueCharset).trim()
    }

    override fun detail(): String {
        val t = if ((text.length < 60)) text else text.substring(0, 59)
        return super.detail() + " for=" + obj_refno + "/" + obj_tagno + " text=" + t
    }
}

// p.114 DFTAG_NT (106)
class TagNT(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var version: Byte = 0
    var numberType: Int = 0
    var nbits: Byte = 0 // Number of bits, all of which are assumed to be significant
    var type_class: Byte = 0 // meaning depends on type: floating point, integer, or character

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        version = h4.raf.readByte(state)
        numberType = h4.raf.readByte(state).toInt()
        nbits = h4.raf.readByte(state)
        type_class = h4.raf.readByte(state)
    }

    override fun detail(): String {
        return super.detail() + " version=" + version + " type=" + numberType + " nbits=" + nbits + " type_class=" + type_class
    }

    override fun toString(): String {
        return super.toString() + " type= ${H4type.getDataType(numberType)} nbits= $nbits"
    }
}

// 300, 307, 308 p119
class TagRIDimension(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var xdim = 0
    var ydim = 0
    var nt_ref: Int = 0
    var nelems: Short = 0
    var interlace: Short = 0
    var compress: Short = 0
    var compress_ref: Short = 0

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        // For GRreadimage, those parameters are expressed in (x,y) or [column,row] order. p 321
        xdim = h4.raf.readInt(state)
        ydim = h4.raf.readInt(state)
        state.pos += 2
        nt_ref = h4.raf.readShort(state).toUShort().toInt()
        nelems = h4.raf.readShort(state)
        interlace = h4.raf.readShort(state)
        compress = h4.raf.readShort(state)
        compress_ref = h4.raf.readShort(state)
    }

    override fun detail(): String {
        return (super.detail() + " xdim=" + xdim + " ydim=" + ydim + " nelems=" + nelems + " nt_ref=" + nt_ref
                + " interlace=" + interlace + " compress=" + compress + " compress_ref=" + compress_ref)
    }
}

// 301 p.124
class TagRasterImage(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var raster = ByteArray(0)

    fun read(h4 : H4builder, nx: Int, ny: Int, ntsize : Int) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        raster = h4.raf.readBytes(state, nx * ny * ntsize)
    }
}

// 302 p.125
class TagLookupTable(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var table: IntArray = IntArray(0)

    // cant read without info from other tags
    fun read(h4 : H4builder, nx: Int, ny: Int) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        table = IntArray(nx * ny) { h4.raf.readInt(state) }
    }
}

// TagEnum.RIG (306), TagEnum.NDG (720) lists of other tags
class TagDataGroup(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    val nelems = length / 4
    var elem_tag = IntArray(0)
    var elem_ref = IntArray(0)

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        elem_tag = IntArray(nelems)
        elem_ref = IntArray(nelems)
        for (i in 0 until nelems) {
            elem_tag[i] = h4.raf.readShort(state).toUShort().toInt()
            elem_ref[i] = h4.raf.readShort(state).toUShort().toInt()
        }
    }

    override fun detail(): String {
        return buildString {
            append(super.detail())
            append("\n")
            append("   tag ref\n   ")
            for (i in 0 until nelems) {
                append(elem_tag[i]).append(" ")
                append(elem_ref[i]).append(" ")
                append("\n   ")
            }
        }
    }
}

// 701 p.133
class TagSDD(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var rank: Short = 0
    var shape = IntArray(0)
    var data_nt_ref: Int = 0 // Reference number of DFTAG_NT for data 
    var scale_nt_ref = ShortArray(0) // Reference number for DFTAG_NT for the scale for the nth dimension
    // LOOK maybe means DFTAG_SDS ??

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        rank = h4.raf.readShort(state)
        shape = IntArray(rank.toInt()) { h4.raf.readInt(state) }
        h4.raf.readShort(state)
        data_nt_ref = h4.raf.readShort(state).toUShort().toInt()
        scale_nt_ref = ShortArray(rank.toInt()) {
            h4.raf.readShort(state)
            h4.raf.readShort(state)
        }
    }

    override fun toString(): String {
        return buildString {
            append(super.toString())
            append(" shape${shape.contentToString()}")
            append(" data_nt_ref = $data_nt_ref")
            append(" scale_nt_ref${scale_nt_ref.contentToString()}")
        }
    }
}


/* 701 p.135
class TagSDS(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var rank: Short = 0
    var hasScale = ShortArray(0)
    var scale = ShortArray(0) // Reference number for DFTAG_NT for the scale for the nth dimension

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
    }
} */

// 704, 705, 706 p 130
class TagTextN(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var texts = mutableListOf<String>()
    var wasRead : Boolean = false

    override fun readTag(h4 : H4builder) { // not Idempotent
        if (wasRead) return
        wasRead = true
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        val ba = h4.raf.readBytes(state, length)
        var count = 0
        var start = 0
        for (pos in 0 until length) {
            if (ba[pos].toInt() == 0) {
                if (pos - start > 0) {
                    texts.add(String(ba, start, pos - start, h4.valueCharset))
                    count++
                }
                start = pos + 1
            }
        }
    }

    override fun toString(): String {
        return "${super.toString()} text=$texts"
    }
}

// 707, p132
class TagSDminmax(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var bb: ByteBuffer? = null

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        bb = h4.raf.readByteBuffer(state, length)
    }

    fun getMin(dataType: Datatype): Number {
        return get(dataType, 1)
    }

    fun getMax(dataType: Datatype): Number {
        return get(dataType, 0)
    }

    operator fun get(dataType: Datatype?, index: Int): Number {
        if (dataType === Datatype.BYTE) return bb!![index]
        if (dataType === Datatype.SHORT) return bb!!.asShortBuffer()[index]
        if (dataType === Datatype.INT) return bb!!.asIntBuffer()[index]
        if (dataType === Datatype.LONG) return bb!!.asLongBuffer()[index]
        if (dataType === Datatype.FLOAT) return bb!!.asFloatBuffer()[index]
        return if (dataType === Datatype.DOUBLE) bb!!.asDoubleBuffer().get(index) else Double.NaN
    }

    fun toString(dt : Datatype): String {
        return "${super.toString()} min=${getMin(dt)} max=${getMax(dt)}"
    }
}


// 732 fill value
class TagFV(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var fillValue : Any? = null

    fun readFillValue(h4 : H4builder, datatype : Datatype): Any? {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        val fillValueBB = h4.raf.readByteBuffer(state, datatype.size)
        fillValue = when (datatype) {
            Datatype.BYTE -> fillValueBB.get()
            Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> fillValueBB.get().toUByte()
            Datatype.SHORT -> fillValueBB.getShort()
            Datatype.USHORT, Datatype.ENUM2 -> fillValueBB.getShort().toUShort()
            Datatype.INT -> fillValueBB.getInt()
            Datatype.UINT, Datatype.ENUM4 -> fillValueBB.getInt().toUInt()
            Datatype.FLOAT -> fillValueBB.getFloat()
            Datatype.DOUBLE -> fillValueBB.getDouble()
            Datatype.LONG -> fillValueBB.getLong()
            Datatype.ULONG -> fillValueBB.getLong().toULong()
            Datatype.OPAQUE -> fillValueBB
            else -> null
        }
        return fillValue
    }

    override fun detail(): String {
        return if (fillValue != null) "$fillValue" else "null"
    }
}

// DFTAG_VG 1965 p 140
// Group tags together to create "user definded objects" such as Variables.
// A Vset is identified by a Vgroup, an object that contains information about the members of the Vset.
class TagVGroup(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var nelems : Int = 0
    var elem_tag = IntArray(0)
    var elem_ref = IntArray(0)

    var extag: Short = 0
    var exref: Short = 0
    var version: Short = 0
    var name: String = "null"
    var className: String = "null"
    var group: Group.Builder? = null

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        nelems = h4.raf.readShort(state).toUShort().toInt()
        elem_tag = IntArray(nelems) { h4.raf.readShort(state).toUShort().toInt() }
        elem_ref = IntArray(nelems) { h4.raf.readShort(state).toUShort().toInt() }
        val len = h4.raf.readShort(state).toInt()
        name = h4.raf.readString(state, len)
        val len2 = h4.raf.readShort(state).toInt()
        className = h4.raf.readString(state, len2)
        extag = h4.raf.readShort(state)
        exref = h4.raf.readShort(state)
        version = h4.raf.readShort(state)
    }

    override fun toString(): String {
        return "${super.toString()} name= '${name}'"
    }

    override fun detail(): String {
        return buildString {
            append(if (isUsed) " " else "*").append("refno=").append(refno).append(" tag= ")
                .append(tagEnum())
                .append(if (isExtended) " EXTENDED" else "").append(" offset=").append(offset).append(" length=")
                .append(length)
                .append(" VV= ${vinfo?.vb?.name ?: ""}")
            append(" class= ").append(className)
            append(" extag= ").append(extag.toInt())
            append(" exref= ").append(exref.toInt())
            append(" version= ").append(version.toInt())
            append("\n")
            append(" name= ").append(name)
            append("\n")
            append("   tag ref\n   ")
            for (i in 0 until nelems) {
                append(elem_tag[i]).append(" ")
                append(elem_ref[i]).append(" ")
                append("\n   ")
            }
        }
    }
}

// DFTAG_VH 1962 Vdata header p 141. Describes a Structure.
class TagVH(icode: Int, refno: Int, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var interlace: Short = 0 // Constant indicating interlace scheme used
    var nelems = 0 // number of entries in Vdata
    var ivsize = 0 // Size of one Vdata record
    var nfields: Short = 0 // Number of fields per entry in the Vdata: so one dimensional Structure(nelems)
    var fld_type = ShortArray(0) // Constant indicating the data type of the nth field of the Vdata
    var fld_isize = IntArray(0) // Size in bytes of the nth field of the Vdata
    var fld_offset = IntArray(0) // Offset of the nth field within the Vdata
    var fld_nelems = ShortArray(0) // Order of the nth field of the Vdata: so, a one dimensional fld(fld_nelems)
    var fld_name = mutableListOf<String>()
    var name: String = "null"
    var className: String = "null"
    var extag: Short = 0 // Extension tag
    var exref: Short = 0 // Extension reference number
    var version: Short = 0 // Version number of DFTAG_VH information

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        interlace = h4.raf.readShort(state)
        nelems = h4.raf.readInt(state)
        ivsize = h4.raf.readShort(state).toUShort().toInt()
        nfields = h4.raf.readShort(state)
        fld_type = ShortArray(nfields.toInt()) { h4.raf.readShort(state) }
        fld_isize = IntArray(nfields.toInt()) { h4.raf.readShort(state).toUShort().toInt() }
        fld_offset = IntArray(nfields.toInt()) { h4.raf.readShort(state).toUShort().toInt() }
        fld_nelems = ShortArray(nfields.toInt()) { h4.raf.readShort(state) }
        for (i in 0 until nfields) {
            val slen = h4.raf.readShort(state).toInt()
            fld_name.add(h4.raf.readString(state, slen))
        }
        var len = h4.raf.readShort(state).toInt()
        name = h4.raf.readString(state, len)
        len = h4.raf.readShort(state).toInt()
        className = h4.raf.readString(state, len)
        extag = h4.raf.readShort(state)
        exref = h4.raf.readShort(state)
        version = h4.raf.readShort(state)
    }

    override fun toString(): String {
        return super.toString() + " name= '$name'"
    }

    override fun detail(): String {
        return buildString {
            append(super.detail())
            append(" class= ").append(className)
            append(" interlace= ").append(interlace.toInt())
            append(" nvert= ").append(nelems)
            append(" ivsize= ").append(ivsize)
            append(" extag= ").append(extag.toInt())
            append(" exref= ").append(exref.toInt())
            append(" version= ").append(version.toInt())
            append("\n")
            append(" name= ").append(name)
            append("\n")
            append("   name    type  isize  offset  order\n   ")
            for (i in 0 until nfields) {
                append(fld_name[i]).append(" ")
                append(fld_type[i].toInt()).append(" ")
                append(fld_isize[i]).append(" ")
                append(fld_offset[i]).append(" ")
                append(fld_nelems[i].toInt()).append(" ")
                append("\n   ")
            }
        }
    }

    // fld_type fld_name(fld_order), so 1 dimensional of length fld_order
    fun readStructureMembers(): List<StructureMember> {
        val members = mutableListOf<StructureMember>()
        for (fld in 0 until this.nfields) {
            val type = this.fld_type[fld].toInt()
            val fdatatype = H4type.getDataType(type)
            val nelems = this.fld_nelems[fld].toInt()
            // val name: String, val datatype : Datatype, val offset: Int, val dims : IntArray
            val m = StructureMember(this.fld_name[fld], fdatatype, this.fld_offset[fld], intArrayOf(nelems))
            members.add(m)
        }
        return members
    }
}