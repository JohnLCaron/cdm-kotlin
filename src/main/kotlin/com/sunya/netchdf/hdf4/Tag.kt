package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Dimension
import com.sunya.cdm.api.Group
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.netchdf.hdf4.H4builder.Companion.tagid
import java.nio.ByteBuffer
import java.nio.ByteOrder

////////////////////////////////////////////////////////////////////////////////
// Tags
fun readTag(raf : OpenFile, state: OpenFileState): Tag {
    // read just the DD part of the tag. see p 11
    val code = raf.readShort(state).toInt()
    val refno = raf.readShort(state)
    val offset = raf.readInt(state).toUInt().toLong()
    val length = raf.readInt(state)

    val ccode = code and 0x3FFF
    when (ccode) {
        20 -> return TagLinkedBlock(code, refno, offset, length)
        30 -> return TagVersion(code, refno, offset, length)
        40, 61, 702, 1963 -> return TagData(code, refno, offset, length)
        100, 101, 708 -> return TagText(code, refno, offset, length)
        104, 105 -> return TagAnnotate(code, refno, offset, length)
        106 -> return TagNumberType(code, refno, offset, length)
        300, 307, 308 -> return TagRIDimension(code, refno, offset, length)
        301 -> return TagRIPalette(code, refno, offset, length)
        306, 720 -> return TagGroup(code, refno, offset, length)
        701 -> return TagSDDimension(code, refno, offset, length)
        704, 705, 706 -> return TagTextN(code, refno, offset, length)
        707 -> return TagSDminmax(code, refno, offset, length)
        1962 -> return TagVH(code, refno, offset, length)
        1965 -> return TagVGroup(code, refno, offset, length)
        else -> return Tag(code, refno, offset, length)
    }
}

// Tag == "Data Descriptor" (DD) and (usually) a "Data Element" that the offset/length points to
open class Tag(icode: Int, val refno : Short, val offset : Long, val length : Int) {
    val code = (icode and 0x3FFF).toShort() // overkill ??
    val isExtended: Boolean = (icode and 0x4000) != 0
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
        return "${if (isUsed) " " else "*"} refno=$refno tag=${tagName()} ${if (isExtended) " EXTENDED" else ""} offset=$offset" +
                " length=$length"
    }

    fun tag(): String {
        return "$refno/$code"
    }

    fun tagName() : String {
        val test = TagEnum.NONE
        return TagEnum.byCode(this.code).toString()
    }

    fun getVinfo(): String {
        return if ((vinfo == null)) "" else vinfo.toString()
    }

    fun vClass() : String {
        return when (this) {
            is TagVGroup -> this.className?: ""
            is TagVH -> this.className?: ""
            else -> ""
        }
    }
}

// 40 (not documented), 702 p 129
class TagData(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var ext_type: Int = 0
    internal var linked: SpecialLinked? = null
    internal var compress: SpecialComp? = null
    internal var chunked: SpecialChunked? = null
    var tag_len = 0

    override fun readTag(h4 : H4builder) {
        if (isExtended) {
            val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
            ext_type = h4.raf.readShort(state).toInt() // note size wrong in doc
            when (ext_type) {
                TagEnum.SPECIAL_LINKED -> linked = SpecialLinked(h4.raf, state)
                TagEnum.SPECIAL_COMP -> compress = SpecialComp(h4.raf, state)
                TagEnum.SPECIAL_CHUNKED -> chunked = SpecialChunked(h4.raf, state)
            }
            tag_len = (state.pos - offset).toInt()
        }
    }

    override fun detail(): String {
        if (linked != null) {
            return super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + linked!!.detail()
        } else if (compress != null) {
            return super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + compress!!.detail()
        } else return if (chunked != null) {
            super.detail() + " ext_tag= " + ext_type + " tag_len= " + tag_len + " " + chunked!!.detail()
        } else super.detail()
    }

    override fun toString(): String {
        return buildString {
            append (super.toString())
            if (ext_type != 0) {
                append(" linked=$linked, compress=$compress, chunked=$chunked, tag_len=$tag_len)")
            }
        }
    }
}

// 20 p 146 Also used for data blocks, which has no next_ref! (!)
class TagLinkedBlock(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var next_ref: Short = 0
    var block_ref = ShortArray(0)
    var n = 0

    fun read2(h4 : H4builder, nb: Int, dataBlocks: MutableList<TagLinkedBlock>) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        next_ref = h4.raf.readShort(state)
        block_ref = ShortArray(nb)
        for (i in 0 until nb) {
            block_ref[i] = h4.raf.readShort(state)
            if (block_ref[i].toInt() == 0) break
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
        if (block_ref == null) return super.detail()
        val sbuff: StringBuilder = StringBuilder(super.detail())
        sbuff.append(" next_ref= ").append(next_ref.toInt())
        sbuff.append(" dataBlks= ")
        for (i in 0 until n) {
            val ref = block_ref[i]
            sbuff.append(ref.toInt()).append(" ")
        }
        return sbuff.toString()
    }
}

// 30
class TagVersion(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
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
        return super.detail() + " version= " + major + "." + minor + "." + release + " (" + name + ")"
    }
}

// 100, 101
class TagText(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var text: String = "null"

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        text = h4.raf.readString(state, length, h4.valueCharset)
    }

    override fun detail(): String {
        val t = if ((text!!.length < 60)) text else text!!.substring(0, 59)
        return super.detail() + " text= " + t
    }
}

// 104, 105
class TagAnnotate(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var text: String = "null"
    var obj_tagno: Short = 0
    var obj_refno: Short = 0

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        obj_tagno = h4.raf.readShort(state)
        obj_refno = h4.raf.readShort(state)
        text = h4.raf.readString(state, length - 4, h4.valueCharset).trim()
    }

    override fun detail(): String {
        val t = if ((text!!.length < 60)) text else text!!.substring(0, 59)
        return super.detail() + " for=" + obj_refno + "/" + obj_tagno + " text=" + t
    }
}

// 106
class TagNumberType(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var version: Byte = 0
    var numberType: Int = 0
    var nbits: Byte = 0
    var type_class: Byte = 0

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
class TagRIDimension(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var xdim = 0
    var ydim = 0
    var nt_ref: Short = 0
    var nelems: Short = 0
    var interlace: Short = 0
    var compress: Short = 0
    var compress_ref: Short = 0
    var dims = mutableListOf<Dimension>()

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        xdim = h4.raf.readInt(state)
        ydim = h4.raf.readInt(state)
        state.pos += 2
        nt_ref = h4.raf.readShort(state)
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

// 301 p121
class TagRIPalette(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var table: IntArray = IntArray(0)

    // cant read without info from other tags
    fun H4builder.read(h4 : H4builder, nx: Int, ny: Int) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        table = IntArray(nx * ny) { h4.raf.readInt(state) }
    }
}

// 701 p128
class TagSDDimension(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var rank: Short = 0
    var nt_ref: Short = 0
    var shape = IntArray(0)
    var nt_ref_scale = ShortArray(0)

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        rank = h4.raf.readShort(state)
        shape = IntArray(rank.toInt()) { h4.raf.readInt(state) }
        state.pos += 2
        nt_ref = h4.raf.readShort(state)
        nt_ref_scale = ShortArray(rank.toInt()) {
            state.pos += 2
            h4.raf.readShort(state)
        }
    }

    override fun detail(): String {
        val sbuff: StringBuilder = StringBuilder(super.detail())
        sbuff.append("   dims= ")
        for (i in 0 until rank) sbuff.append(shape[i]).append(" ")
        sbuff.append("   nt= ").append(nt_ref.toInt()).append(" nt_scale=")
        for (i in 0 until rank) sbuff.append(nt_ref_scale[i].toInt()).append(" ")
        return sbuff.toString()
    }

    override fun toString(): String {
        return buildString {
            append(super.toString())
            append(" shape[${shape.contentToString()}")
            append(" nt_ref = $nt_ref")
            append(" nt_ref_scale[${nt_ref_scale.contentToString()}")
        }
    }
}

// 704, 705, 706 p 130
class TagTextN(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var text = mutableListOf<String>()

    fun readTag(h4 : H4builder, n: Int) { // LOOK this is fishy
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        val ba = h4.raf.readBytes(state, length)
        var count = 0
        var start = 0
        for (i in 0 until length) {
            if (ba[i].toInt() == 0) {
                text.add(String(ba, start, i - start, h4.valueCharset))
                count++
                if (count == n) break
                start = i + 1
            }
        }
    }
}

// 707, p132
class TagSDminmax(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var bb: ByteBuffer? = null
    var dt: Datatype? = null

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        bb = h4.raf.readByteBuffer(state, length)
    }

    fun getMin(dataType: Datatype?): Number {
        dt = dataType
        return get(dataType, 1)
    }

    fun getMax(dataType: Datatype?): Number {
        dt = dataType
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

    override fun detail(): String {
        return super.detail() + "   min= " + getMin(dt) + "   max= " + getMax(dt)
    }
}

// 306 p118; 720 p 127
class TagGroup(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    val nelems = length / 4
    var elem_tag = ShortArray(0)
    var elem_ref = ShortArray(0)

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        elem_tag = ShortArray(nelems)
        elem_ref = ShortArray(nelems)
        for (i in 0 until nelems) {
            elem_tag[i] = h4.raf.readShort(state)
            elem_ref[i] = h4.raf.readShort(state)
        }
    }

    override fun detail(): String {
        val sbuff: StringBuilder = StringBuilder(super.detail())
        sbuff.append("\n")
        sbuff.append("   tag ref\n   ")
        for (i in 0 until nelems) {
            sbuff.append(elem_tag[i].toInt()).append(" ")
            sbuff.append(elem_ref[i].toInt()).append(" ")
            sbuff.append("\n   ")
        }
        return sbuff.toString()
    }
}

// 1965 p135
class TagVGroup(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var nelems : Int = 0
    var elem_tag = ShortArray(0)
    var elem_ref = ShortArray(0)

    var extag: Short = 0
    var exref: Short = 0
    var version: Short = 0
    var name: String = "null"
    var className: String = "null"
    var group: Group.Builder? = null

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        nelems = h4.raf.readShort(state).toUShort().toInt()
        elem_tag = ShortArray(nelems) { h4.raf.readShort(state) }
        elem_ref = ShortArray(nelems) { h4.raf.readShort(state) }
        var len = h4.raf.readShort(state).toInt()
        name = h4.raf.readString(state, len)
        len = h4.raf.readShort(state).toInt()
        className = h4.raf.readString(state, len)
        extag = h4.raf.readShort(state)
        exref = h4.raf.readShort(state)
        version = h4.raf.readShort(state)
    }

    override fun toString(): String {
        return super.toString() + " class= " + className + " name= " + name
    }

    override fun detail(): String {
        val sbuff = StringBuilder()
        sbuff.append(if (isUsed) " " else "*").append("refno=").append(refno.toInt()).append(" tag= ")
            .append(tagName())
            .append(if (isExtended) " EXTENDED" else "").append(" offset=").append(offset).append(" length=")
            .append(length)
            .append(" VV= ${vinfo?.vb?.name ?: ""}")
        sbuff.append(" class= ").append(className)
        sbuff.append(" extag= ").append(extag.toInt())
        sbuff.append(" exref= ").append(exref.toInt())
        sbuff.append(" version= ").append(version.toInt())
        sbuff.append("\n")
        sbuff.append(" name= ").append(name)
        sbuff.append("\n")
        sbuff.append("   tag ref\n   ")
        for (i in 0 until nelems) {
            sbuff.append(elem_tag.get(i).toInt()).append(" ")
            sbuff.append(elem_ref.get(i).toInt()).append(" ")
            sbuff.append("\n   ")
        }
        return sbuff.toString()
    }
}

// 1962 p 136
class TagVH(icode: Int, refno : Short, offset : Long, length : Int) : Tag(icode, refno, offset, length) {
    var interlace: Short = 0
    var nfields: Short = 0
    var extag: Short = 0
    var exref: Short = 0
    var version: Short = 0
    var ivsize = 0
    var fld_type = ShortArray(0)
    var fld_order = ShortArray(0)
    var fld_isize = IntArray(0)
    var fld_offset = IntArray(0)
    var fld_name = mutableListOf<String>()
    var nvert = 0 // number of entries in Vdata
    var name: String = "null"
    var className: String = "null"
    var tag_len = 0

    override fun readTag(h4 : H4builder) {
        val state = OpenFileState(offset, ByteOrder.BIG_ENDIAN)
        interlace = h4.raf.readShort(state)
        nvert = h4.raf.readInt(state)
        ivsize = h4.raf.readShort(state).toUShort().toInt()
        nfields = h4.raf.readShort(state)
        fld_type = ShortArray(nfields.toInt()) { h4.raf.readShort(state) }
        fld_isize = IntArray(nfields.toInt()) { h4.raf.readShort(state).toUShort().toInt() }
        fld_offset = IntArray(nfields.toInt()) { h4.raf.readShort(state).toUShort().toInt() }
        fld_order = ShortArray(nfields.toInt()) { h4.raf.readShort(state) }
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
        tag_len = (state.pos - offset).toInt()
    }

    override fun toString(): String {
        return super.toString() + " class= " + className + " name= " + name
    }

    override fun detail(): String {
        val sbuff: StringBuilder = StringBuilder(super.detail())
        sbuff.append(" class= ").append(className)
        sbuff.append(" interlace= ").append(interlace.toInt())
        sbuff.append(" nvert= ").append(nvert)
        sbuff.append(" ivsize= ").append(ivsize)
        sbuff.append(" extag= ").append(extag.toInt())
        sbuff.append(" exref= ").append(exref.toInt())
        sbuff.append(" version= ").append(version.toInt())
        sbuff.append(" tag_len= ").append(tag_len)
        sbuff.append("\n")
        sbuff.append(" name= ").append(name)
        sbuff.append("\n")
        sbuff.append("   name    type  isize  offset  order\n   ")
        for (i in 0 until nfields) {
            sbuff.append(fld_name[i]).append(" ")
            sbuff.append(fld_type[i].toInt()).append(" ")
            sbuff.append(fld_isize[i]).append(" ")
            sbuff.append(fld_offset[i]).append(" ")
            sbuff.append(fld_order[i].toInt()).append(" ")
            sbuff.append("\n   ")
        }
        return sbuff.toString()
    }
}