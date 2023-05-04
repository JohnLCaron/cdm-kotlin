package com.sunya.cdm.api

import com.sunya.cdm.util.CdmFullNames
import org.junit.jupiter.api.Test
import kotlin.test.*

class TestVariable {

    private fun makeDummyGroup() = Group.Builder("dummy").build(null)

    @Test
    fun testBuilder() {
        val vb = Variable.Builder("name")
            .setDatatype(Datatype.FLOAT)
            .addAttribute(Attribute("scoobie", "doo"))

        assertEquals("name", vb.fullname(Group.Builder("root")))
        assertEquals("'name' float, dimensions=[], dimList=null", vb.toString())

        val v = vb.build(makeDummyGroup())
        assertEquals(Datatype.FLOAT, v.datatype)
        assertEquals("name", v.name)
        assertEquals("name", v.orgName)
        assertEquals("name", v.fullname())
        assertEquals("float name[]", v.nameAndShape())
        assertEquals(0, v.rank)
        assertEquals(1, v.nelems)
        assertTrue(v.dimensions.isEmpty())
        assertNotNull(v.group)
        assertNull(v.spObject)

        assertEquals(1, v.attributes.size)
        assertNotNull(v.findAttribute("scoobie"))
        assertEquals(Attribute("scoobie", "doo"), v.findAttribute("scoobie"))
    }

    @Test
    fun testCDL() {
        val x = Dimension("x", 27)
        val xlen = Dimension("xlen", 27)
        val v: Variable.Builder = Variable.Builder("x").setDatatype(Datatype.CHAR)
            .addAttribute(Attribute("name", "value"))
        v.dimensions.addAll( listOf(x, xlen))
        val g: Group = Group.Builder("what").addDimension(x).addDimension(xlen).addVariable(v).build(null)
        val xvar = g.variables.find { it.name == "x" }
        assertNotNull(xvar)
        assertEquals("char x[27, 27]", xvar.nameAndShape())
        assertEquals("""  char x(x, xlen) ;
    :name = "value" ;
"""
            , xvar.cdl())
    }

    @Test
    fun testEquals() {
        val x = Dimension("x", 27)
        val xlen = Dimension("xlen", 27)
        val v: Variable.Builder = Variable.Builder("x").setDatatype(Datatype.CHAR)
            .addDimension(x).addDimension(xlen).addAttribute(Attribute("name", "value"))
        val g: Group = Group.Builder("what").addDimension(x).addDimension(xlen).addVariable(v).build(null)
        val xvar1 = g.variables.find { it.name == "x" }
        assertNotNull(xvar1)

        val var2 = Variable.Builder("x").setDatatype(Datatype.CHAR)
            .addDimension(x).addDimension(xlen).addAttribute(Attribute("name", "value"))
        val g2: Group = Group.Builder("what").addDimension(x).addDimension(xlen).addVariable(var2).build(null)
        val xvar2 = g2.variables.find { it.name == "x" }
        assertNotNull(xvar2)
        assertEquals(xvar1, xvar2)
        assertEquals(xvar1.hashCode(), xvar2.hashCode())
    }

    @Test
    fun noDimNames() {
        // Must set dimensions in group
        val vb = Variable.Builder("name").setDatatype(Datatype.FLOAT)
        vb.dimNames = listOf("x", "y")

        val ex = assertFails {
            vb.build(makeDummyGroup())
        }
        assertEquals("unknown dimension 'x' in Variable 'name'", ex.message)
    }

    @Test
    fun withDimNames() {
        val dim1 = Dimension("dim1", 7)
        val dim2 = Dimension("dim2", 27)
        val vb = Variable.Builder("name").setDatatype(Datatype.FLOAT)
        vb.dimNames = listOf("dim1", "dim2")
        val gb: Group.Builder = Group.Builder("what").addDimension(dim1).addDimension(dim2).addVariable(vb)
        val g = gb.build(null)
        val v = vb.build(g)

        assertEquals(2, v.rank)
        assertEquals(listOf(dim1, dim2), v.dimensions)
        assertTrue(v.shape.contentEquals(longArrayOf(7, 27)))
        assertEquals("float name[7, 27]", v.nameAndShape())
        assertEquals("  float name(dim1, dim2) ;\n", v.cdl())
    }

    @Test
    fun anonymousDims() {
        val shape = intArrayOf(3, 6, 42)
        val v: Variable =
            Variable.Builder("name").setDatatype(Datatype.FLOAT).setDimensionsAnonymous(shape)
                .build(makeDummyGroup())

        assertEquals(3, v.rank)
        v.dimensions.forEach { assertTrue(!it.isShared)}
        assertTrue(v.shape.contentEquals(longArrayOf(3, 6, 42)))
        assertEquals("float name[3, 6, 42]", v.nameAndShape())
        assertEquals("  float name(3, 6, 42) ;\n", v.cdl())
    }

    @Test
    fun testGroupParents() {
        val dim1 = Dimension("dim1", 7)
        val vb = Variable.Builder("name")
            .setDatatype(Datatype.FLOAT)
            .setDimensionsAnonymous(intArrayOf(0, 42))
        vb.dimNames = listOf("dim1")
        val parentg: Group.Builder = Group.Builder("parent").addDimension(dim1).addVariable(vb)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg)
        val root = Group.Builder("root").addGroup(grampsb).build(null)

        val v = CdmFullNames(root).findVariable("/gramps/parent/name")
        assertNotNull(v)
        assertEquals("/gramps/parent/name", v.fullname())
        assertEquals("  float name(dim1) ;\n", v.cdl())

        val parent = CdmFullNames(root).findGroup("/gramps/parent")
        assertNotNull(parent)
        assertNotNull(parent.variables.find{ it.name == "name"})
    }

    @Test
    fun testDimInRoot() {
        val dim1 = Dimension("dim1", 7)
        val vb = Variable.Builder("name")
            .setDatatype(Datatype.FLOAT)
            .setDimensionsAnonymous(intArrayOf(0, 42))
        vb.dimNames = listOf("dim1")
        val parentg: Group.Builder = Group.Builder("parent").addVariable(vb)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg)
        val root = Group.Builder("root").addGroup(grampsb).addDimension(dim1).build(null)

        val v = CdmFullNames(root).findVariable("/gramps/parent/name")
        assertNotNull(v)
        assertEquals("/gramps/parent/name", v.fullname())
        assertEquals("  float name(dim1) ;\n", v.cdl())
    }

    @Test
    fun testDimBelow() {
        val dim1 = Dimension("dim1", 7)
        val vb = Variable.Builder("name")
            .setDatatype(Datatype.FLOAT)
            .setDimensionsAnonymous(intArrayOf(0, 42))
        vb.dimNames = listOf("dim1")
        val parentg: Group.Builder = Group.Builder("parent").addDimension(dim1)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addVariable(vb)
        val rootb = Group.Builder("root").addGroup(grampsb)

        val ex = assertFails {
            rootb.build(null)
        }
        assertEquals("unknown dimension 'dim1' in Variable 'name'", ex.message)
    }

    /*
        @Test
        fun testEnum() {
            val map: Map<Int, String> = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3")
            val typedef1 = EnumTypedef("typename", map)
            val vb: Variable.Builder = Variable.Builder("v").setDatatype(Datatype.ENUM4)
                .setDimensionsAnonymous(intArrayOf(3, 6)).setEnumTypeName("typename")
            val root: Group.Builder = Group.Builder().addEnumTypedef(typedef1).addVariable(vb)
            val ncfile: CdmFile = CdmFile.builder().setRootGroup(root).build()
            val v: Variable = ncfile.findVariable("v")
            assertThat(v).isNotNull()
            assertThat(v.getEnumTypedef()).isEqualTo(typedef1)
            assertThat(v.lookupEnumString(3)).isEqualTo("name3")
            assertThat(v.toString()).startsWith("enum typename v(3, 6);")
            try {
                Variable.Builder("v").setDatatype(Datatype.ENUM4).setDimensionsAnonymous(intArrayOf(3, 6))
                    .build(makeDummyGroup())
                fail()
            } catch (e: Exception) {
                // expected
            }
            try {
                Variable.Builder("v").setDatatype(Datatype.ENUM4).setDimensionsAnonymous(intArrayOf(3, 6))
                    .setEnumTypeName("enum").build(makeDummyGroup())
                fail()
            } catch (e: Exception) {
                // expected
            }
        }


        @Test
        @Throws(IOException::class)
        fun testAutoGen() {
            val x = Dimension("x", 27)
            val v: Variable.Builder = Variable.Builder("x").setDatatype(Datatype.INT)
                .setDimensions(ImmutableList.of(x)).addAttribute(Attribute("name", "value")).setAutoGen(100, 10)
            val g: Group = Group.Builder().addDimensions(ImmutableList.of(x)).addVariable(v).build()
            val xvar: Variable = g.findVariableLocal("x")
            assertThat(xvar).isNotNull()
            val data: Array = xvar.readArray()
            assertThat(data).isEqualTo(Arrays.makeArray(Datatype.INT, x.getLength(), 100, 10))
        }

         */
}