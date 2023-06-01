package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import com.sunya.testdata.propTestSlowConfig
import com.sunya.testdata.runTest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.test.*

class TestArrayStructureData {

    @Test
    fun TestStructureMember() {
        // open class StructureMember<T>(val orgName: String, val datatype : Datatype<T>, val offset: Int, val dims : IntArray, val endian : ByteOrder? = null) {
        val test = StructureMember("org name", Datatype.LONG, 42, intArrayOf())
        assertEquals("org_name", test.name)
        assertEquals(Datatype.LONG, test.datatype)
        assertEquals(42, test.offset)
        assertEquals(1, test.nelems)
        assertContains(
            test.toString(),
            "StructureMember(name='org_name', datatype=int64, offset=42, dims=[], nelems=1)"
        )

        assertEquals(test, test)
        val test2 = StructureMember("org name", Datatype.LONG, 42, intArrayOf(1))
        assertNotEquals(test, test2)
        assertNotEquals(test.hashCode(), test2.hashCode())
    }

    @Test
    fun TestReadStringZ() {
        val bb = ByteBuffer.allocate(60)
        repeat(60) { bb.put((48 + it).toByte()) }
        assertEquals("0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijk", makeStringZ(bb, 0, 60))
        assertEquals(30, makeStringZ(bb, 10, 30).length)
        assertEquals(":;<=>?@ABCDEFGHIJKLMNOPQRSTUVW", makeStringZ(bb, 10, 30))

        bb.put(20, 0.toByte())
        assertEquals(20, makeStringZ(bb, 0, 30).length)
        assertEquals("0123456789:;<=>?@ABC", makeStringZ(bb, 0, 30))
    }

    fun makeArrayStructureData(recnums: Int): ArrayStructureData {
        val members = mutableListOf<StructureMember<*>>()
        var offset = 0
        var count = 1
        for (datatype in StructureMember.datatypes()) {
            val member = StructureMember("m${datatype.cdlName}-$count", datatype, offset, intArrayOf(1))
            members.add(member)
            offset += datatype.size
            count++
        }
        for (datatype in StructureMember.datatypes()) {
            val member = StructureMember("m${datatype.cdlName}-$count", datatype, offset, intArrayOf(2))
            members.add(member)
            offset += datatype.size * 2
            count++
        }
        val recsize = offset
        val bb = ByteBuffer.allocate(recsize * recnums)
        repeat(recnums) { bb.put(recsize * it, it.toByte()) } // recnum is first byte value
        return ArrayStructureData(intArrayOf(recnums), bb, recsize, members)
    }

    @Test
    fun testArrayStructureData() {
        val arraySD = makeArrayStructureData(1)
        assertEquals(arraySD, arraySD)

        var count = 0
        for (sdata in arraySD) {
            assertEquals(arraySD.members, sdata.members)
            println(sdata.toString())
            assertContains(sdata.toString(), "muint_enum-13 = 0, mchar-14 = 0, mstring-15 = \"\", mbyte-16 = [0,0], mubyte-17 = [0,0]")
            assertEquals(sdata, arraySD.get(count))
            assertEquals(sdata.hashCode(), arraySD.get(count).hashCode())
            count++
        }
        println(arraySD.toString())

        val arraySDp = makeArrayStructureData(1)
        assertEquals(arraySD, arraySDp)

        val arraySD2 = makeArrayStructureData(2)
        assertNotEquals(arraySD, arraySD2)
    }

    @Test
    fun testStringVlen() {
        val members = mutableListOf<StructureMember<*>>()
        var offset = 0
        var count = 1
        for (datatype in StructureMember.datatypes()) {
            val member = StructureMember("m${datatype.cdlName}-$count", datatype, offset, intArrayOf(1))
            members.add(member)
            offset += datatype.size
            count++
        }

        val svMember = StructureMember("svMember", Datatype.STRING.withVlen(true), offset, intArrayOf(1))
        members.add(svMember)
        val recsize = offset + Datatype.STRING.size

        val recnums = 3
        val arraySD = ArrayStructureData(intArrayOf(recnums), ByteBuffer.allocate(recsize * recnums), recsize, members)
        var recno = 0
        for (sdata in arraySD) {
            sdata.putOnHeap(svMember, "hoottenanny-$recno") // not  list
            recno++
        }

        recno = 0
        for (sdata in arraySD) {
            val wtf: Any = svMember.value(sdata)
            assertEquals("hoottenanny-$recno", svMember.value(sdata))
            recno++
        }
    }

    @Test
    fun testStringVlenList() {
        val members = mutableListOf<StructureMember<*>>()
        var offset = 0
        var count = 1
        for (datatype in StructureMember.datatypes()) {
            val member = StructureMember("m${datatype.cdlName}-$count", datatype, offset, intArrayOf(1))
            members.add(member)
            offset += datatype.size
            count++
        }

        // really an array of two vlens. but no way to sdata.putOnHeap, other than idx = 0
        // note each vlen can be any number
        val svMember = StructureMember("svMember", Datatype.STRING.withVlen(true), offset, intArrayOf(2))
        members.add(svMember)
        val recsize = offset + Datatype.STRING.size * 2

        val recnums = 3
        val arraySD = ArrayStructureData(intArrayOf(recnums), ByteBuffer.allocate(recsize * recnums), recsize, members)
        var recno = 0
        for (sdata in arraySD) {
            sdata.putOnHeap(svMember, listOf("hoot-$recno", "holler-$recno"))
            recno++
        }

        recno = 0
        for (sdata in arraySD) {
            val wtf: Any = svMember.value(sdata)
            assertEquals(listOf("hoot-$recno", "holler-$recno"), wtf)
            recno++
        }
    }

    @Test
    fun testSection() {
        val arraySD3 = makeArrayStructureData(3)
        val fullShape = arraySD3.shape.toLongArray()

        val sectionStart = intArrayOf(1)
        val sectionLength = intArrayOf(1)
        val section = Section(sectionStart, sectionLength, fullShape)
        val sectionArray = arraySD3.section(section)

        assertEquals(Datatype.COMPOUND, sectionArray.datatype)
        assertEquals(sectionLength.computeSize(), sectionArray.nelems)

        val firstMember = arraySD3.members[0]
        val full = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), fullShape)
        val odo = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), fullShape)
        odo.forEachIndexed { idx, index ->
            val sdata = sectionArray.get(idx)
            val wtf: Any = firstMember.value(sdata)
            assertEquals(full.element(index).toByte(), wtf)
        }
    }


    fun makeArrayStructureDataWithObjects(recnums: Int, addVStrings: Boolean, addVlens: Boolean): ArrayStructureData {
        val members = mutableListOf<StructureMember<*>>()
        var offset = 0

        members.add(StructureMember("recnum", Datatype.BYTE, offset, intArrayOf(1)))
        offset++

        if (addVStrings) {
            var stringDatatype = Datatype.STRING.withVlen(true)
            members.add(StructureMember("stringDatatype1", stringDatatype, offset, intArrayOf(1)))
            offset += stringDatatype.size

            members.add(StructureMember("stringDatatype2", stringDatatype, offset, intArrayOf(2)))
            offset += stringDatatype.size * 2
        }

        if (addVlens) {
            val vlenDatatype = Datatype.VLEN.withTypedef(VlenTypedef("vlenInt", Datatype.INT))
            members.add(StructureMember("vlenDatatype1", vlenDatatype, offset, intArrayOf(1)))
            offset += vlenDatatype.size

            members.add(StructureMember("vlenDatatype2", vlenDatatype, offset, intArrayOf(2)))
            offset += vlenDatatype.size * 2
        }

        val recsize = offset
        val bb = ByteBuffer.allocate(recsize * recnums)
        repeat(recnums) { bb.put(recsize * it, it.toByte()) } // recnum is first byte value
        return ArrayStructureData(intArrayOf(recnums), bb, recsize, members)
    }

    @Test
    fun putStringsOnHeap() {
        val arraySD3 = makeArrayStructureDataWithObjects(3, true, false)

        arraySD3.putStringsOnHeap { member, offset ->
            val result = mutableListOf<String>()
            repeat(member.nelems) {
                val sval = "sm-$offset"
                result.add(sval)
            }
            result
        }
        println(arraySD3.toString())

        val sMember = arraySD3.members.find { it.name == "stringDatatype1"}!!
        var recno = 0
        for (sdata in arraySD3) {
            val wtf: Any = sMember.value(sdata)
            val sval = "sm-${1+recno*arraySD3.recsize}"
            assertEquals(listOf(sval), wtf)
            recno++
        }

        val sMember2 = arraySD3.members.find { it.name == "stringDatatype2"}!!
        var recno2 = 0
        for (sdata in arraySD3) {
            val wtf: Any = sMember2.value(sdata)
            val sval = "sm-${5+recno2*arraySD3.recsize}"
            assertEquals(listOf(sval, sval), wtf)
            recno2++
        }
    }

    @Test
    fun putVlensOnHeap() {
        val arraySD3 = makeArrayStructureDataWithObjects(3, false, true)

        arraySD3.putVlensOnHeap { member, offset ->
            // List<Array<T>>
            val listOfVlen = mutableListOf<Array<Int>>()
            repeat(member.nelems) {
                val vlen : Array<Int> = Array(2) {offset}
                listOfVlen.add(vlen)
            }
            // ArrayVlen<T>(shape : IntArray, val values : List<Array<T>>, val baseType : Datatype<T>)
            ArrayVlen(intArrayOf(member.nelems), listOfVlen, Datatype.INT)
        }
        println(arraySD3.toString())

        val sMember = arraySD3.members.find { it.name == "vlenDatatype1"}!!
        var recno = 0
        for (sdata in arraySD3) {
            val wtf: Any = sMember.value(sdata)
            val expect = ArrayVlen(intArrayOf(1), listOf(Array(2) {1+recno*arraySD3.recsize}), Datatype.INT)
            assertEquals(expect, wtf)
            recno++
        }

        // TODO
        //   val expect = ArrayVlen(intArrayOf(1), listOf(Array(2) {1+recno2*arraySD3.recsize}), Datatype.INT)
        //   assertEquals(listOf(expect, expect), wtf)
        // vs
        //             val expect1 = Array(2) {1+recno2*arraySD3.recsize}
        //            val expect = ArrayVlen(intArrayOf(1), listOf(expect1, expect1), Datatype.INT)

        val sMember2 = arraySD3.members.find { it.name == "vlenDatatype2"}!!
        var recno2 = 0
        for (sdata in arraySD3) {
            val wtf: Any = sMember2.value(sdata)
            val expect1 = Array(2) {5+recno2*arraySD3.recsize}
            val expect = ArrayVlen(intArrayOf(2), listOf(expect1, expect1), Datatype.INT)
            assertEquals(expect, wtf)
            recno2++
        }
    }
}
