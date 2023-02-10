package com.sunya.cdm.iosp

import com.sunya.cdm.api.DataType
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

open class StructureMember(val name: String, val dataType : DataType, val offset: Int, val nelems : Int) {

    open fun value(sdata : StructureData) : Any {
        val bb = sdata.bb
        val offset = sdata.offset + this.offset
        return when (dataType.primitiveClass) {
            Byte::class.java -> bb.get(offset)
            Short::class.java -> bb.getShort(offset)
            Int::class.java -> bb.getInt(offset)
            Long::class.java-> bb.getLong(offset)
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8)
        }
    }

}
