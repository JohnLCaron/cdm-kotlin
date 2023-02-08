package sunya.cdm.dsl

import sunya.cdm.iosp.OpenFile
import sunya.cdm.iosp.OpenFileState
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class StructDsl(val name : String, val bb : ByteBuffer, val flds : List<StructFld>, val startPos : Long) {
    val fldm = flds.associateBy { it.fldName }

    fun dataSize() = bb.limit()

    fun show() {
        println("$name startPos = $startPos")
        flds.filter{ it.fldName.isNotEmpty() }.forEach {
            println("  $it = ${show(it)}")
        }
    }

    fun show(fld : StructFld) : Any {
        if (fld.nelems == 0) {
            return "empty"
        }
        // hmmmm
        if (fld.nelems > 1 && fld.elemSize != 1) {
            Array(fld.nelems) { idx ->
                when (fld.elemSize) {
                    2 -> bb.getShort(fld.pos + 2 * idx)
                    4 -> bb.getInt(fld.pos + 4 * idx)
                    8 -> bb.getLong(fld.pos + 8 * idx)
                    else -> getString(fld.fldName)
                }
            }
        }

        return when(fld.elemSize) {
            1 -> bb.get(fld.pos)
            2 -> bb.getShort(fld.pos)
            4 -> bb.getInt(fld.pos)
            8 -> bb.getLong(fld.pos)
            else -> getString(fld.fldName)
        }
    }

    fun getLong(fldName : String) : Long {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return bb.getLong(fld.pos)
    }
    fun getInt(fldName : String) : Int {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return bb.getInt(fld.pos)
    }
    fun getByte(fldName : String) : Byte {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return bb.get(fld.pos)
    }
    fun getShort(fldName : String) : Short {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return bb.getShort(fld.pos)
    }
    fun getIntArray(fldName : String) : IntArray {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return IntArray(fld.nelems) { idx -> bb.getInt(fld.pos + fld.elemSize * idx) } // LOOK what if long ?
    }
    fun getByteBuffer(fldName : String) : ByteBuffer {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        return bb.slice(fld.pos, fld.nelems * fld.elemSize)
    }
    fun getString(fldName : String) : String {
        val fld = fldm[fldName] ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        for (idx in 0 until fld.nelems * fld.elemSize) {
            if (bb.get(fld.pos + idx).toInt() == 0) { // terminate at 0 byte
                return String(bb.array(), fld.pos, idx, StandardCharsets.UTF_8)
            }
        }
        return String(bb.array(), fld.pos, fld.nelems * fld.elemSize, StandardCharsets.UTF_8)
    }
}

class StructDslBuilder(val name : String, val raf: OpenFile, val state: OpenFileState) {
    val flds = mutableListOf<StructFld>()
    val startPos = state.pos
    var pos = 0

    fun fld(fldName: String, length: Int): StructFld {
        val fld = StructFld(fldName, pos, length)
        flds.add(fld)
        pos += length
        return fld
    }

    fun fld(fldName: String, lambda: StructDslBuilder.() -> Int): StructFld {
        val length = lambda()
        val fld = StructFld(fldName, pos, length)
        flds.add(fld)
        pos += length
        return fld
    }

    fun overlay(fldName: String, offset: Int, overlayName: String) {
        val fld = flds.find { it.fldName == fldName }
            ?: throw IllegalArgumentException("StructDsl $name has no fld '$fldName'")
        val overlay = StructFld(overlayName, fld.pos + offset, 0)
        flds.add(overlay)
    }

    fun array(fldName: String, length: Int, nelemsFld: String) {
        val fld = flds.find { it.fldName == nelemsFld }
            ?: throw IllegalArgumentException("StructDsl $name has no nelemsFld '$fldName'")
        val nelems: Int = eagerRead(fld)
        val overlay = StructFld(fldName, pos, length, nelems)
        pos += length * nelems
        flds.add(overlay)
    }

    fun eagerRead(from: StructFld) : Int {
        val tstate = state.copy(pos = startPos + from.pos)
        val bb = raf.readByteBuffer(tstate, from.elemSize)
        when (from.elemSize) {
            1 -> return bb.get().toInt()
            2 -> return bb.getShort().toInt()
            4 -> return bb.getInt()
            else -> throw IllegalArgumentException("StructDsl $name illegal eager read length=${from.elemSize}")
        }
    }

    fun skip(nbytes: Int) {
        fld("", nbytes)
    }

    fun build(): StructDsl {
        val total = flds.map { it.elemSize * it.nelems }.sum()
        val bb = raf.readByteBuffer(state, total)
        return StructDsl(name, bb, flds, startPos)
    }
}

data class StructFld(val fldName: String, val pos: Int, val elemSize: Int, val nelems : Int) {
    constructor(fldName: String, pos: Int, length: Int) : this(fldName, pos, length, 1)

    override fun toString(): String {
        return String.format("%-20s %2d,%2d", fldName,pos,elemSize)
    }
}

fun structdsl(name : String, raf : OpenFile, state : OpenFileState, lambda: StructDslBuilder.() -> Unit): StructDsl {
    val builder = StructDslBuilder(name, raf, state)
    builder.lambda()
    return builder.build()
}