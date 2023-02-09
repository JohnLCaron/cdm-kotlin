package sunya.cdm.api

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

    fun value(sdata : StructureData) : String {
        val bb = sdata.bb
        val offset = sdata.offset + this.offset
        return when (dataType.primitiveClass) {
            Byte::class.java -> bb.get(offset).toString()
            Short::class.java -> bb.getShort(offset).toString()
            Int::class.java -> bb.getInt(offset).toString()
            Long::class.java-> bb.getLong(offset).toString()
            else -> String(bb.array(), offset, nelems, StandardCharsets.UTF_8)
        }
    }

}
