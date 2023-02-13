package com.sunya.cdm.iosp

import com.sunya.cdm.api.Datatype
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class StructureData(val bb : ByteBuffer, val offset: Int, val members : StructureMembers) {
    override fun toString() : String {
        return buildString {
            append("{")
            members.members.forEach { append("${it.name} = ${it.value(this@StructureData) }, ") }
            append("}")
        }
    }
}

data class StructureMembers(val members : List<StructureMember>) : Iterable<StructureMember> {
    override fun iterator(): Iterator<StructureMember> = members.iterator()
}

open class StructureMember(val name: String, val datatype : Datatype, val offset: Int, val nelems : Int) {

    open fun value(sdata : StructureData) : Any {
        val bb = sdata.bb
        val offset = sdata.offset + this.offset
        return when (datatype) {
            Datatype.BYTE -> bb.get(offset)
            Datatype.SHORT -> bb.getShort(offset)
            Datatype.INT -> bb.getInt(offset)
            Datatype.LONG -> bb.getLong(offset)
            Datatype.UBYTE -> bb.get(offset).toUByte()
            Datatype.USHORT -> bb.getShort(offset).toUShort()
            Datatype.UINT -> bb.getInt(offset).toUInt()
            Datatype.ULONG -> bb.getLong(offset).toULong()
            Datatype.FLOAT -> bb.getFloat(offset)
            Datatype.DOUBLE -> bb.getDouble(offset)
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8)
        }
    }

}
