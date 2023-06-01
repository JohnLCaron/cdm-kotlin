package com.sunya.cdm.api

import org.junit.jupiter.api.Test
import kotlin.test.*

/** Test [com.sunya.cdm.api.Group]  */
class TestGroup {
    
    @Test
    fun testBuilder() {
        val att = Attribute.from("attName", "value")
        val dim = Dimension("dimName", 42)
        val nested: Group.Builder = Group.Builder("child")
        val vb = Variable.Builder("varName", Datatype.STRING)
        val group = Group.Builder("name").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb).build(null)
        assertEquals(group.name, "name")
        assertTrue(group.attributes.isNotEmpty())
        assertEquals(1, group.attributes.size)
        assertEquals(att, group.findAttribute("attName"))
        assertTrue(group.dimensions.isNotEmpty())
        assertEquals(1, group.dimensions.size)
        assertNotNull(group.findDimension("dimName"))
        assertEquals(dim, group.findDimension("dimName"))
        assertTrue(group.groups.isNotEmpty())
        assertEquals(1, group.groups.size)
        val child = group.groups.find{ it.name == "child"}
        assertNotNull(child)
        assertEquals(child.parent, group)
        assertEquals(1, group.variables.size)
        val v = group.variables.find{ it.name == "varName" }
        assertNotNull(v)
        assertEquals(v.group, group)
    }

    @Test
    fun testDuplicateDimension() {
        val dim = Dimension("dimName", 42)
        val builder: Group.Builder = Group.Builder("name").addDimension(dim)
        try {
            builder.addDimension(dim)
            fail("should have failed")
        } catch (e: Exception) {
            assertEquals("tried to add duplicate dimension 'dimName'", e.message)
        }
    }

    @Test
    fun testRemoveGroup() {
        val child: Group.Builder = Group.Builder("child")
        val child2: Group.Builder = Group.Builder("child2")
        val builder: Group.Builder = Group.Builder("name").addGroup(child).addGroup(child2)
        assertEquals(2, builder.groups.size)
        assertTrue(builder.removeGroupIfExists("child"))
        assertEquals(1, builder.groups.size)
        assertNull(builder.groups.find{ it.name == "child"})
        assertNotNull(builder.groups.find{ it.name == "child2"})
    }

    @Test
    fun testDuplicateVariable() {
        val vb  = Variable.Builder("varName", Datatype.ULONG)
        val groupb: Group.Builder = Group.Builder("name").addVariable(vb)
        groupb.addVariable(vb)
        assertEquals(1, groupb.variables.size)
    }

    @Test
    fun testAttributes() {
        val att1 = Attribute.from("attName", "value")
        val att2 = Attribute.from("attName2", "value2")
        val groupb = Group.Builder("name").addAttribute(att1).addAttribute(att2)
        assertEquals(2, groupb.attributes.size)
        assertEquals(att1, groupb.attributes.find { it.name == "attName" })
        assertEquals(att2, groupb.attributes.find { it.name == "attName2" })
        assertNull(groupb.attributes.find { it.name == "bad" })
    }
    
    @Test
    fun testGroupParents() {
        val parentg: Group.Builder = Group.Builder("parent")
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val root = Group.Builder("root").addGroup(grampsb).addGroup(uncleb)
        val parent = root.findNestedGroupByShortName("parent")
        assertNotNull(parent)
        val gramps = root.findNestedGroupByShortName("gramps")
        assertNotNull(gramps)
        val uncle = root.findNestedGroupByShortName("uncle")
        assertNotNull(uncle)
        assertEquals(parent.commonParent(gramps), gramps)
        assertEquals(gramps.commonParent(parent), gramps)
        assertEquals(root.commonParent(parent), root)
        assertEquals(uncle.commonParent(parent), root)
        assertEquals(parent.commonParent(uncle), root)
        assertEquals(uncle.commonParent(gramps), root)
        assertEquals(gramps.commonParent(uncle), root)
        assertTrue(root.isParent(parent))
        assertTrue(root.isParent(uncle))
        assertTrue(root.isParent(gramps))
        assertTrue(gramps.isParent(parent))
        assertFalse(parent.isParent(gramps))
        assertFalse(uncle.isParent(gramps))
        assertFalse(gramps.isParent(uncle))
        assertFalse(uncle.isParent(parent))
        assertFalse(parent.isParent(uncle))
    }

    @Test
    fun testGroupParentMistake() {
        val parentg: Group.Builder = Group.Builder("parent")
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg)
        val uncleb: Group.Builder = Group.Builder("uncle").addGroup(parentg) // ooops added twice
        try {
            Group.Builder("root").addGroup(grampsb).addGroup(uncleb).build(null)
            fail()
        } catch (e: Exception) {
            // expected
            assertContains(e.message?: "empty", "Group 'parent' was already built")
        }
    }

    @Test
    fun testFindDimension() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val root = Group.Builder("root").addGroup(grampsb).addGroup(uncleb).addDimension(high).build(null)
        val gramps = root.groups.find { it.name == "gramps" }
        assertNotNull(gramps)
        val parent = gramps.groups.find { it.name == "parent" }
        assertNotNull(parent)
        val uncle = root.groups.find { it.name == "uncle" }
        assertNotNull(uncle)
        assertNotNull(parent.findDimension("low"))
        assertNotNull(parent.findDimension("mid"))
        assertNotNull(parent.findDimension("high"))
        assertNull(gramps.findDimension("low"))
        assertNotNull(gramps.findDimension("mid"))
        assertNotNull(gramps.findDimension("high"))
        assertNull(root.findDimension("low"))
        assertNull(root.findDimension("mid"))
        assertNotNull(root.findDimension("high"))
        assertNull(uncle.findDimension("low"))
        assertNull(uncle.findDimension("mid"))
        assertNotNull(uncle.findDimension("high"))
        assertNotNull(parent.dimensions.find {it == low})
        assertNull(parent.dimensions.find {it == mid})
        assertNull(parent.dimensions.find {it == high})
        assertNull(gramps.dimensions.find {it == low} )
        assertNotNull(gramps.dimensions.find {it == mid} )
        assertNull(gramps.dimensions.find {it == high} )
        assertNull(root.dimensions.find {it == low})
        assertNull(root.dimensions.find {it == mid})
        assertNotNull(root.dimensions.find {it == high})
        assertNull(uncle.dimensions.find {it == low})
        assertNull(uncle.dimensions.find {it == mid})
        assertNull(uncle.dimensions.find {it == high})
    }

    @Test
    fun testFindEnum() {
        val map = mapOf(1 to "name1", 2 to "name2", 3 to "name3")
        val typedef1 = EnumTypedef("low", Datatype.INT, map)
        val typedef2 = EnumTypedef("high", Datatype.INT, map)
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder("parent").addDimension(low).addTypedef(typedef1)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val root: Group =
            Group.Builder("root").addGroup(grampsb).addGroup(uncleb).addDimension(high).addTypedef(typedef2).build(null)
        val gramps = root.groups.find { it.name == "gramps"}
        assertNotNull(gramps)
        val parent = gramps.groups.find { it.name == "parent"}
        assertNotNull(parent)
        val uncle = root.groups.find { it.name == "uncle"}
        assertNotNull(uncle)
        assertNotNull(parent.findTypedef("high"))
        assertNotNull(gramps.findTypedef("high"))
        assertNotNull(uncle.findTypedef("high"))
        assertNotNull(root.findTypedef("high"))
        assertNotNull(parent.findTypedef("low"))
        assertNull(gramps.findTypedef("low"))
        assertNull(uncle.findTypedef("low"))
        assertNull(root.findTypedef("low"))
        assertNull(root.findTypedef("bad"))
    }

    @Test
    fun testFindVariable() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val vb = Variable.Builder("v", Datatype.STRING)
        vb.dimNames = listOf("low")
        val vattb = Variable.Builder("vatt", Datatype.STRING)
            .addAttribute(Attribute.from("findme", "findmevalue"))
        vattb.dimNames = listOf("mid")

        parentg.addVariable(vb)
        grampsb.addVariable(vattb)
        val root: Group = Group.Builder("root").addGroup(grampsb).addGroup(uncleb).addDimension(high).build(null)
        val parent = root.findNestedGroupByShortName("parent")
        assertNotNull(parent)
        val gramps = root.findNestedGroupByShortName("gramps")
        assertNotNull(gramps)
        val uncle = root.findNestedGroupByShortName("uncle")
        assertNotNull(uncle)
        assertNull(parent.findVariableByAttribute("findme", "findmevalue"))
        assertNull(uncle.findVariableByAttribute("findme", "findmevalue"))
        assertNotNull(gramps.findVariableByAttribute("findme", "findmevalue"))
        assertNotNull(root.findVariableByAttribute("findme", "findmevalue"))

        assertNotNull(parent.variables.find{it.name == "v"})
        assertNull(parent.variables.find{it.name == "findme"})
        assertNull(gramps.variables.find{it.name == "v"})
        assertNotNull(gramps.variables.find{it.name == "vatt"})
        assertNull(gramps.variables.find{it.name == ""})
    }

    @Test
    fun testGetters() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val vb = Variable.Builder("v", Datatype.STRING)
        vb.dimNames = listOf("low")
        val vattb = Variable.Builder("vatt", Datatype.STRING)
            .addAttribute(Attribute.from("findme", "findmevalue"))
        vattb.dimNames = listOf("mid")
        parentg.addVariable(vb)
        grampsb.addVariable(vattb)
        val map = mapOf(1 to "name1", 2 to "name2", 3 to "name3")
        val typedef1 = EnumTypedef("low", Datatype.INT, map)
        grampsb.addTypedef(typedef1)
        val root = Group.Builder("root").addGroup(grampsb).addGroup(uncleb).addDimension(high).build(null)

        val parent = root.findNestedGroupByShortName("parent")
        assertNotNull(parent)
        val gramps = root.findNestedGroupByShortName("gramps")
        assertNotNull(gramps)
        val uncle = root.findNestedGroupByShortName("uncle")
        assertNotNull(uncle)
        assertTrue(gramps.dimensions.isNotEmpty())
        assertTrue(uncle.dimensions.isEmpty())
        assertTrue(gramps.typedefs.isNotEmpty())
        assertTrue(root.typedefs.isEmpty())
        assertTrue(uncle.groups.isEmpty())
        assertTrue(root.groups.isNotEmpty())
        assertTrue(gramps.variables.isNotEmpty())
        assertTrue(uncle.variables.isEmpty())
        assertTrue(root.variables.isEmpty())
        assertEquals(parent.parent, gramps)
        assertEquals(uncle.parent, root)
        assertEquals(gramps.parent, root)
        assertNull(root.parent)
        assertNotNull(gramps.parent)
    }

    @Test
    fun testWriteCDL() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder("uncle")
        val vb = Variable.Builder("v", Datatype.STRING)
        vb.dimNames = listOf("low")
        val vattb = Variable.Builder("vatt", Datatype.STRING)
            .addAttribute(Attribute.from("findme", "findmevalue"))
        vattb.dimNames = listOf("mid")
        parentg.addVariable(vb)
        grampsb.addVariable(vattb)
        val map = mapOf(1 to "name1", 2 to "name2", 3 to "name3")
        val typedef1 = EnumTypedef("low", Datatype.INT, map)
        grampsb.addTypedef(typedef1)
        val root = Group.Builder("root").addGroup(grampsb).addGroup(uncleb).addDimension(high).build(null)

        val parent = root.findNestedGroupByShortName("parent")
        assertNotNull(parent)
        val gramps = root.findNestedGroupByShortName("gramps")
        assertNotNull(gramps)
        val uncle = root.findNestedGroupByShortName("uncle")
        assertNotNull(uncle)

        assertEquals("""  types:
    int enum low {1 = name1, 2 = name2, 3 = name3};
  dimensions:
    mid = 1 ;
  variables:
    string vatt(mid) ;
      :findme = "findmevalue" ;

  group: parent {
    dimensions:
      low = 1 ;
    variables:
      string v(low) ;
  }
""",
            gramps.cdl(false)
        )
        println("cdl=${uncle.cdl(false)}")
        assertEquals("", uncle.cdl(false)) // hmmm ?
    }

    @Test
    fun testEquals() {
        val good: Group = Group.Builder("good").build(null)
        val good2: Group = Group.Builder("good").build(null)
        val bad: Group = Group.Builder("bad").build(null)
        assertEquals(good, good2)
        assertNotEquals(good, bad)
        assertEquals(good.hashCode(), good2.hashCode())
        assertNotEquals(good.hashCode(), bad.hashCode())
    }

    @Test
    fun testEqualsWithParent() {
        val goodb: Group.Builder = Group.Builder("good")
        val parent: Group = Group.Builder("parent").addGroup(goodb).build(null)
        val goodb2: Group.Builder = Group.Builder("good")
        val parent2: Group = Group.Builder("parent").addGroup(goodb2).build(null)
        val good = parent.findNestedGroupByShortName("good")
        assertNotNull(good)
        val good2 = parent2.findNestedGroupByShortName("good")
        assertNotNull(good2)
        val bad: Group = Group.Builder("good").build(null)
        assertEquals(good, good2)
        assertNotEquals(good, bad)
        assertEquals(good.hashCode(), good2.hashCode())
        assertNotEquals(good.hashCode(), bad.hashCode())
    }

    @Test
    fun testAddAndReplaceDimension() {
        val dim42 = Dimension("dim1", 42)
        val gb: Group.Builder = Group.Builder("name")
        assertTrue(gb.addDimensionIfNotExists(dim42))
        assertFalse(gb.addDimensionIfNotExists(dim42))
        assertNotNull(gb.dimensions.find { it.name == "dim1"})
        assertTrue(gb.dimensions.isNotEmpty())

        val dim43 = Dimension("dim1", 43)
        assertTrue(gb.replaceDimension( dim43 ))
        assertNull(gb.dimensions.find { it == dim42 } )
        assertFalse(gb.addDimensionIfNotExists(dim42))

        val dim = gb.dimensions.find { it.name == "dim1"}
        assertNotNull(dim)
        assertEquals(43, dim.length)
    }
}