package com.sunya.cdm.api

import org.junit.jupiter.api.Test
import kotlin.test.*

/** Test [dev.ucdm.core.api.Group]  */
class TestGroup() {
    
    @Test
    fun testBuilder() {
        val att = Attribute("attName", "value")
        val dim = Dimension("dimName", 42)
        val nested: Group.Builder = Group.Builder("child")
        val vb: Variable.Builder = Variable.Builder("varName").setDatatype(Datatype.STRING)
        val group = Group.Builder("name").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb).build(null)
        assertEquals(group.name, "name")
        assertTrue(group.attributes.isNotEmpty())
        assertEquals(1, group.attributes.size)
        assertEquals(att, group.attributes.find {it.name == "attName"})
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
        val vb  = Variable.Builder("varName")
        val groupb: Group.Builder = Group.Builder("name").addVariable(vb)
        groupb.addVariable(vb)
        assertEquals(1, groupb.variables.size)
    }

    @Test
    fun testAttributes() {
        val att1 = Attribute("attName", "value")
        val att2 = Attribute("attName2", "value2")
        val groupb = Group.Builder("name").addAttribute(att1).addAttribute(att2)
        assertEquals(2, groupb.attributes.size)
        assertEquals(att1, groupb.attributes.find { it.name == "attName" })
        assertEquals(att2, groupb.attributes.find { it.name == "attName2" })
        assertNull(groupb.attributes.find { it.name == "bad" })
    }

    /*
    @Test
    fun testGroupParents() {
        val parentg: Group.Builder = Group.Builder().setName("parent")
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val root: Group = Group.Builder().addGroup(grampsb).addGroup(uncleb).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(parent.commonParent(gramps), gramps)
        assertEquals(gramps.commonParent(parent), gramps)
        assertEquals(root.commonParent(parent), root)
        assertEquals(uncle.commonParent(parent), root)
        assertEquals(parent.commonParent(uncle), root)
        assertEquals(uncle.commonParent(gramps), root)
        assertEquals(gramps.commonParent(uncle), root)
        assertEquals(root.isParent(parent)).isTrue()
        assertEquals(root.isParent(uncle)).isTrue()
        assertEquals(root.isParent(gramps)).isTrue()
        assertEquals(gramps.isParent(parent)).isTrue()
        assertEquals(parent.isParent(gramps)).isFalse()
        assertEquals(uncle.isParent(gramps)).isFalse()
        assertEquals(gramps.isParent(uncle)).isFalse()
        assertEquals(uncle.isParent(parent)).isFalse()
        assertEquals(parent.isParent(uncle)).isFalse()
    }

    @Test
    fun testGroupParentMistake() {
        val parentg: Group.Builder = Group.Builder().setName("parent")
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg)
        val uncleb: Group.Builder = Group.Builder().setName("uncle").addGroup(parentg) // ooops added twice
        try {
            Group.Builder().addGroup(grampsb).addGroup(uncleb).build()
            fail()
        } catch (e: Exception) {
            // expected
        }
    }

    @Test
    fun testFindDimension() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder().setName("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val root: Group = Group.Builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(parent.findDimension("low").isPresent()).isTrue()
        assertEquals(parent.findDimension("mid").isPresent()).isTrue()
        assertEquals(parent.findDimension("high").isPresent()).isTrue()
        assertEquals(gramps.findDimension("low").isPresent()).isFalse()
        assertEquals(gramps.findDimension("mid").isPresent()).isTrue()
        assertEquals(gramps.findDimension("high").isPresent()).isTrue()
        assertEquals(root.findDimension("low").isPresent()).isFalse()
        assertEquals(root.findDimension("mid").isPresent()).isFalse()
        assertEquals(root.findDimension("high").isPresent()).isTrue()
        assertEquals(uncle.findDimension("low").isPresent()).isFalse()
        assertEquals(uncle.findDimension("mid").isPresent()).isFalse()
        assertEquals(uncle.findDimension("high").isPresent()).isTrue()
        assertEquals(parent.findDimension(low) != null).isTrue()
        assertEquals(parent.findDimension(mid) != null).isTrue()
        assertEquals(parent.findDimension(high) != null).isTrue()
        assertEquals(gramps.findDimension(low) != null).isFalse()
        assertEquals(gramps.findDimension(mid) != null).isTrue()
        assertEquals(gramps.findDimension(high) != null).isTrue()
        assertEquals(root.findDimension(low) != null).isFalse()
        assertEquals(root.findDimension(mid) != null).isFalse()
        assertEquals(root.findDimension(high) != null).isTrue()
        assertEquals(uncle.findDimension(low) != null).isFalse()
        assertEquals(uncle.findDimension(mid) != null).isFalse()
        assertEquals(uncle.findDimension(high) != null).isTrue()
        assertEquals(uncle.findDimension(null as Dimension?) == null).isTrue()
        assertEquals(uncle.findDimension((null as String?)!!).isPresent()).isFalse()
        assertEquals(parent.findDimensionLocal(null) != null).isFalse()
    }

    @Test
    fun testFindEnum() {
        val map: Map<Int, String> = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3")
        val typedef1 = EnumTypedef("low", map)
        val typedef2 = EnumTypedef("high", map)
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder().setName("parent").addDimension(low).addEnumTypedef(typedef1)
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val root: Group =
            Group.Builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).addEnumTypedef(typedef2).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(parent.findEnumeration("high") != null).isTrue()
        assertEquals(gramps.findEnumeration("high") != null).isTrue()
        assertEquals(uncle.findEnumeration("high") != null).isTrue()
        assertEquals(root.findEnumeration("high") != null).isTrue()
        assertEquals(parent.findEnumeration("low") != null).isTrue()
        assertEquals(gramps.findEnumeration("low") != null).isFalse()
        assertEquals(uncle.findEnumeration("low") != null).isFalse()
        assertEquals(root.findEnumeration("low") != null).isFalse()
        assertEquals(root.findEnumeration(null) != null).isFalse()
    }

    @Test
    fun testFindVariable() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder().setName("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val vb: Variable.Builder<*> = Variable.Builder().setName("v").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("low")
        val vattb: Variable.Builder<*> = Variable.Builder().setName("vatt").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(Attribute("findme", "findmevalue"))
        parentg.addVariable(vb)
        grampsb.addVariable(vattb)
        val root: Group = Group.Builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(parent.findVariableByAttribute("findme", "findmevalue") != null).isFalse()
        assertEquals(uncle.findVariableByAttribute("findme", "findmevalue") != null).isFalse()
        assertEquals(gramps.findVariableByAttribute("findme", "findmevalue") != null).isTrue()
        assertEquals(root.findVariableByAttribute("findme", "findmevalue") != null).isTrue()
        assertEquals(parent.findVariableLocal("v") != null).isTrue()
        assertEquals(parent.findVariableLocal("findme") != null).isFalse()
        assertEquals(gramps.findVariableLocal("v") != null).isFalse()
        assertEquals(gramps.findVariableLocal("vatt") != null).isTrue()
        assertEquals(gramps.findVariableLocal(null) != null).isFalse()
        assertEquals(parent.findVariableOrInParent("v") != null).isTrue()
        assertEquals(parent.findVariableOrInParent("vatt") != null).isTrue()
        assertEquals(uncle.findVariableOrInParent("v") != null).isFalse()
        assertEquals(root.findVariableOrInParent("vatt") != null).isFalse()
        assertEquals(root.findVariableOrInParent(null) != null).isFalse()
    }

    @Test
    fun testGetters() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder().setName("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val vb: Variable.Builder<*> = Variable.Builder().setName("v").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("low")
        val vattb: Variable.Builder<*> = Variable.Builder().setName("vatt").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(Attribute("findme", "findmevalue"))
        parentg.addVariable(vb)
        grampsb.addVariable(vattb)
        val map: Map<Int, String> = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3")
        val typedef1 = EnumTypedef("low", map)
        grampsb.addEnumTypedef(typedef1)
        val root: Group = Group.Builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(gramps.dimensions).isNotEmpty()
        assertEquals(uncle.dimensions).isEmpty()
        assertEquals(gramps.getEnumTypedefs()).isNotEmpty()
        assertEquals(root.getEnumTypedefs()).isEmpty()
        assertEquals(uncle.groups).isEmpty()
        assertEquals(root.groups).isNotEmpty()
        assertEquals(gramps.variables).isNotEmpty()
        assertEquals(uncle.variables).isEmpty()
        assertEquals(root.variables).isEmpty()
        assertEquals(parent.parent, gramps)
        assertEquals(uncle.parent, root)
        assertEquals(gramps.parent, root)
        assertEquals(root.parent).isNull()
        assertEquals(gramps.isRoot()).isFalse()
        assertEquals(root.isRoot()).isTrue()
    }

    @Test
    fun testWriteCDL() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parentg: Group.Builder = Group.Builder().setName("parent").addDimension(low)
        val grampsb: Group.Builder = Group.Builder().setName("gramps").addGroup(parentg).addDimension(mid)
        val uncleb: Group.Builder = Group.Builder().setName("uncle")
        val vb: Variable.Builder<*> = Variable.Builder().setName("v").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("low")
        val vattb: Variable.Builder<*> = Variable.Builder().setName("vatt").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(Attribute("findme", "findmevalue"))
        parentg.addVariable(vb).addAttribute(Attribute("groupAtt", "groupVal"))
        grampsb.addVariable(vattb)
        val map: Map<Int, String> = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3")
        val typedef1 = EnumTypedef("low", map)
        parentg.addEnumTypedef(typedef1)
        val root: Group = Group.Builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build()
        val parent: Group = root.findGroupNested("parent").orElse(null)
        assertEquals(parent).isNotNull()
        val gramps: Group = root.findGroupNested("gramps").orElse(null)
        assertEquals(gramps).isNotNull()
        val uncle: Group = root.findGroupNested("uncle").orElse(null)
        assertEquals(uncle).isNotNull()
        assertEquals(gramps.toString()).startsWith(
            String.format(
                "dimensions:%n" + "  mid = 1;%n" + "variables:%n"
                        + "  string vatt(mid=1);%n" + "    :findme = \"findmevalue\"; // string%n" + "%n" + "group: parent {%n"
                        + "  types:%n" + "    enum low { 'name1' = 1, 'name2' = 2, 'name3' = 3};%n" + "%n" + "  dimensions:%n"
                        + "    low = 1;%n" + "  variables:%n" + "    string v(low=1);%n" + "%n" + "  // group attributes:%n"
                        + "  :groupAtt = \"groupVal\";%n" + "}"
            )
        )
        assertEquals(uncle.toString(), "") // really ?
    }

    @Test
    fun testEquals() {
        val good: Group = Group.Builder().setName("good").build()
        val good2: Group = Group.Builder().setName("good").build()
        val bad: Group = Group.Builder().setName("bad").build()
        assertEquals(good, good2)
        assertEquals(good).isNotEqualTo(bad)
        assertEquals(good.hashCode(), good2.hashCode())
        assertEquals(good.hashCode()).isNotEqualTo(bad.hashCode())
    }

    @Test
    fun testEqualsWithParent() {
        val goodb: Group.Builder = Group.Builder().setName("good")
        val parent: Group = Group.Builder().setName("parent").addGroup(goodb).build()
        val goodb2: Group.Builder = Group.Builder().setName("good")
        val parent2: Group = Group.Builder().setName("parent").addGroup(goodb2).build()
        val good: Group = parent.findGroupNested("good").orElse(null)
        assertEquals(good).isNotNull()
        val good2: Group = parent2.findGroupNested("good").orElse(null)
        assertEquals(good2).isNotNull()
        val bad: Group = Group.Builder().setName("good").build()
        assertEquals(good, good2)
        assertEquals(good).isNotEqualTo(bad)
        assertEquals(good.hashCode(), good2.hashCode())
        assertEquals(good.hashCode()).isNotEqualTo(bad.hashCode())
    }

    @Test
    fun testToBuilder() {
        val goodb: Group.Builder = Group.Builder().setName("good")
        val parent: Group = Group.Builder().setName("parent").addGroup(goodb).build()
        val good: Group = parent.findGroupNested("good").orElse(null)
        assertEquals(good).isNotNull()
        val good2: Group = good.toBuilder().build(parent)
        assertEquals(good, good2)
        assertEquals(good.hashCode(), good2.hashCode())
    }

    @Test
    fun testAddAndRemoveDimension() {
        val dim = Dimension("dim1", 42)
        val gb: Group.Builder = Group.Builder().setName("name")
        assertEquals(gb.addDimensionIfNotExists(dim)).isTrue()
        assertEquals(gb.addDimensionIfNotExists(dim)).isFalse()
        assertEquals(gb.findDimension("dim1").isPresent()).isTrue()
        assertEquals(gb.dimensions).isNotEmpty()
        assertEquals(gb.removeDimension("dim1")).isTrue()
        assertEquals(gb.findDimension("dim1").isPresent()).isFalse()
        assertEquals(gb.dimensions).isEmpty()
        assertEquals(gb.addDimensionIfNotExists(dim)).isTrue()
        assertEquals(gb.findDimension("dim1").isPresent()).isTrue()
    }

    @Test
    fun testNestedGroupBuilders() {
        val vroot: Variable.Builder<*> = Variable.Builder().setName("vroot").setDatatype(Datatype.STRING)
        val vleaf: Variable.Builder<*> = Variable.Builder().setName("vleaf").setDatatype(Datatype.STRING)
        val voff: Variable.Builder<*> = Variable.Builder().setName("voff").setDatatype(Datatype.STRING)
        val parent: Group.Builder = Group.Builder().setName("parent").addVariable(vleaf)
        val gramps: Group.Builder = Group.Builder().setName("gramps").addGroup(parent)
        val uncle: Group.Builder = Group.Builder().setName("uncle").addVariable(voff)
        val root: Group.Builder = Group.Builder().addGroup(gramps).addGroup(uncle).addVariable(vroot)
        var found: Group.Builder = root.findGroupNested("/gramps/parent").orElse(null)
        assertEquals(found).isNotNull()
        assertEquals(found, parent)
        found = root.findGroupNested("/uncle").orElse(null)
        assertEquals(found).isNotNull()
        assertEquals(found, uncle)
        found = root.findGroupNested("/").orElse(null)
        assertEquals(found).isNotNull()
        assertEquals(found, root)

        // TODO The leading "/" is optional.
        assertEquals(root.findGroupNested("gramps").isPresent()).isTrue()
        assertEquals(root.findGroupNested("gramps/parent").isPresent()).isTrue()
        assertEquals(root.findGroupNested("/random").isPresent()).isFalse()
        assertEquals(root.findGroupNested("random").isPresent()).isFalse()
        assertEquals(root.findGroupNested("parent").isPresent()).isFalse()
        assertEquals(root.findGroupNested(null).isPresent()).isTrue()
        assertEquals(root.findGroupNested("").isPresent()).isTrue()
        assertEquals(gramps.findGroupNested("").isPresent()).isFalse()
        assertEquals(root.findVariableNested("gramps/parent/vleaf").isPresent()).isTrue()
        assertEquals(root.findVariableNested("/gramps/parent/vleaf").isPresent()).isTrue()
        assertEquals(root.findVariableNested("vroot").isPresent()).isTrue()
        assertEquals(root.findVariableNested("/vroot").isPresent()).isTrue()
        assertEquals(root.findVariableNested("/uncle/voff").isPresent()).isTrue()
        assertEquals(root.findVariableNested("uncle/voff").isPresent()).isTrue()
        assertEquals(root.findVariableNested("/random").isPresent()).isFalse()
        assertEquals(root.findVariableNested("/gramps/voff").isPresent()).isFalse()
        assertEquals(root.findVariableNested("/gramps/parent/voff.m").isPresent()).isFalse()
        assertEquals(gramps.findVariableNested("parent/vleaf").isPresent()).isTrue()
        assertEquals(uncle.findVariableNested("voff").isPresent()).isTrue()
        assertEquals(uncle.findVariableNested("vleaf").isPresent()).isFalse()
        assertEquals(uncle.findVariableNested("vroot").isPresent()).isFalse()
    }

    @Test
    fun testCommonParentBuilders() {
        val parent: Group.Builder = Group.Builder().setName("parent")
        val gramps: Group.Builder = Group.Builder().setName("gramps").addGroup(parent)
        val uncle: Group.Builder = Group.Builder().setName("uncle")
        val root: Group.Builder = Group.Builder().addGroup(gramps).addGroup(uncle)
        assertEquals(parent.commonParent(gramps), gramps)
        assertEquals(gramps.commonParent(parent), gramps)
        assertEquals(root.commonParent(parent), root)
        assertEquals(uncle.commonParent(parent), root)
        assertEquals(parent.commonParent(uncle), root)
        assertEquals(uncle.commonParent(gramps), root)
        assertEquals(gramps.commonParent(uncle), root)
        assertEquals(root.isParent(parent)).isTrue()
        assertEquals(root.isParent(uncle)).isTrue()
        assertEquals(root.isParent(gramps)).isTrue()
        assertEquals(gramps.isParent(parent)).isTrue()
        assertEquals(parent.isParent(gramps)).isFalse()
        assertEquals(uncle.isParent(gramps)).isFalse()
        assertEquals(gramps.isParent(uncle)).isFalse()
        assertEquals(uncle.isParent(parent)).isFalse()
        assertEquals(parent.isParent(uncle)).isFalse()

        // TODO leading, trailing "/" ??
        assertEquals(parent.makeFullName(), "gramps/parent/")
        assertEquals(gramps.makeFullName(), "gramps/")
        assertEquals(root.makeFullName(), "")
        assertEquals(uncle.makeFullName(), "uncle/")
    }

    @Test
    fun testFindEnumBuilders() {
        val map: Map<Int, String> = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3")
        val typedef1 = EnumTypedef("low", map)
        val typedef2 = EnumTypedef("high", map)
        val parent: Group.Builder = Group.Builder().setName("parent").addEnumTypedef(typedef1)
        val gramps: Group.Builder = Group.Builder().setName("gramps").addGroup(parent)
        val uncle: Group.Builder = Group.Builder().setName("uncle")
        val root: Group.Builder = Group.Builder().addGroups(ImmutableList.of(gramps, uncle)).addEnumTypedef(typedef2)
        assertEquals(parent.findEnumeration("high").isPresent()).isFalse()
        assertEquals(gramps.findEnumeration("high").isPresent()).isFalse()
        assertEquals(uncle.findEnumeration("high").isPresent()).isFalse()
        assertEquals(root.findEnumeration("high").isPresent()).isTrue()
        assertEquals(parent.findEnumeration("low").isPresent()).isTrue()
        assertEquals(gramps.findEnumeration("low").isPresent()).isFalse()
        assertEquals(uncle.findEnumeration("low").isPresent()).isFalse()
        assertEquals(root.findEnumeration("low").isPresent()).isFalse()
        assertEquals(root.findEnumeration(null).isPresent()).isFalse()
        assertEquals(root.findOrAddEnumTypedef("high", null), typedef2)
        val another = EnumTypedef("another", map)
        assertEquals(root.findOrAddEnumTypedef("another", map), another)
    }

    @Test
    fun testFindVariableBuilders() {
        val low = Dimension("low", 1)
        val mid = Dimension("mid", 1)
        val high = Dimension("high", 1)
        val parent: Group.Builder = Group.Builder().setName("parent").addDimensions(ImmutableList.of(low, high))
        val gramps: Group.Builder = Group.Builder().setName("gramps").addGroup(parent).addDimension(mid)
        val uncle: Group.Builder = Group.Builder().setName("uncle")
        val vb: Variable.Builder<*> = Variable.Builder().setName("v").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parent).setDimensionsByName("low")
        val vattb: Variable.Builder<*> = Variable.Builder().setName("vatt").setDatatype(Datatype.STRING)
            .setParentGroupBuilder(parent).setDimensionsByName("mid").addAttribute(Attribute("findme", "findmevalue"))
        parent.addVariables(ImmutableList.of(vb, vattb))
        parent.replaceVariable(vb)
        val root: Group.Builder = Group.Builder().addGroup(gramps).addGroup(uncle)
        assertEquals(parent.findVariableLocal("v").isPresent()).isTrue()
        assertEquals(parent.findVariableOrInParent("v").isPresent()).isTrue()
        assertEquals(root.findVariableLocal("v").isPresent()).isFalse()
        val vhigh: Variable.Builder<*> =
            Variable.Builder().setName("vhigh").setDatatype(Datatype.STRING).setParentGroupBuilder(gramps)
        gramps.replaceVariable(vhigh)
        assertEquals(parent.findVariableOrInParent("vhigh").isPresent()).isTrue()
        assertEquals(gramps.removeVariable("vhigh")).isTrue()
        assertEquals(parent.findVariableOrInParent("vhigh").isPresent()).isFalse()
        assertEquals(gramps.removeVariable("vhigh")).isFalse()
    }
    
     */
}