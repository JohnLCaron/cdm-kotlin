package com.sunya.cdm.util

import com.sunya.cdm.api.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestCdmFullNames {

    @Test
    fun testFindFromFullname() {
        val vroot = Variable.Builder("vroot", Datatype.STRING)
        val vleaf = Variable.Builder("vleaf", Datatype.STRING)
        val voff = Variable.Builder("voff", Datatype.STRING)
        val parent: Group.Builder = Group.Builder("parent").addVariable(vleaf).addDimension(Dimension("dim", 1))
        val gramps: Group.Builder = Group.Builder("gramps").addGroup(parent)
        val uncle: Group.Builder = Group.Builder("uncle").addVariable(voff).addDimension(Dimension("dim", 2))
        val root : Group = Group.Builder("")
            .addGroup(gramps)
            .addGroup(uncle)
            .addVariable(vroot)
            .addDimension(Dimension("wit", 3))
            .build(null)
        println(root.cdl(true))

        val fullNames = CdmFullNames(root)

        var found = fullNames.findGroup("/gramps/parent")
        assertNotNull(found)
        found = fullNames.findGroup("/uncle")
        assertNotNull(found)
        found = fullNames.findGroup("/")
        assertNotNull(found)
        assertEquals(found, root)

        assertNotNull(fullNames.findGroup("gramps"))
        assertNotNull(fullNames.findGroup("/gramps"))
        assertNotNull(fullNames.findGroup("gramps/parent"))
        assertNotNull(fullNames.findGroup("/gramps/parent"))
        assertNull(fullNames.findGroup("/gramps/parent/bad"))
        assertNull(fullNames.findGroup("/gramps/bad"))
        assertNull(fullNames.findGroup("/random"))
        assertNull(fullNames.findGroup("random"))
        assertNull(fullNames.findGroup("parent"))
        assertNotNull(fullNames.findGroup(""))
        assertNotNull(fullNames.findGroup("/"))

        assertNotNull(fullNames.findVariable("/gramps/parent/vleaf"))
        assertNotNull(fullNames.findVariable("gramps/parent/vleaf"))
        assertNotNull(fullNames.findVariable("/vroot"))
        assertNotNull(fullNames.findVariable("vroot"))
        assertNotNull(fullNames.findVariable("/uncle/voff"))
        assertNotNull(fullNames.findVariable("/uncle/voff"))
        assertNull(fullNames.findVariable("/bad"))
        assertNull(fullNames.findVariable("/uncle/bad"))
        assertNull(fullNames.findVariable("/"))
        assertNull(fullNames.findVariable(""))
        assertNull(fullNames.findVariable("bad"))

        assertNotNull(fullNames.findVariable("vroot"))
        assertNotNull(fullNames.findVariable("/uncle/voff"))
        assertNotNull(fullNames.findVariable("uncle/voff"))
        assertNull(fullNames.findVariable("parent/vleaf"))
        assertNull(fullNames.findVariable("/random"))
        assertNull(fullNames.findVariable("/gramps/voff"))
        assertNull(fullNames.findVariable("/gramps/parent/voff"))
        assertNull(fullNames.findVariable("/gramps/parent/voff"))
        assertNull(fullNames.findVariable("/voff"))
        assertNull(fullNames.findVariable("voff"))
        assertNull(fullNames.findVariable("vleaf"))

        assertNotNull(fullNames.findDimension("/gramps/parent/dim"))
        assertNotNull(fullNames.findDimension("/uncle/dim"))
        assertNull(fullNames.findDimension("/gramps/dim"))
        assertNull(fullNames.findDimension("/dim"))
        assertNull(fullNames.findDimension("dim"))
        assertNotNull(fullNames.findDimension("wit"))
        assertNotNull(fullNames.findDimension("/wit"))
        assertNull(fullNames.findDimension("/"))
        assertNull(fullNames.findDimension(""))
        assertNull(fullNames.findDimension("nope"))

        assertEquals(1, fullNames.findDimension("/gramps/parent/dim")?.length)
        assertEquals(2, fullNames.findDimension("/uncle/dim")?.length)
        assertEquals(3, fullNames.findDimension("/wit")?.length)
    }

    @Test
    fun testFindAttributes() {
        val vroot = Variable.Builder("vroot", Datatype.STRING)
        val vleaf = Variable.Builder("vleaf", Datatype.STRING)
        val voff = Variable.Builder("voff", Datatype.STRING)
            .addAttribute(Attribute.from("zoom", "schwartz"))
        val parent: Group.Builder = Group.Builder("parent").addVariable(vleaf).addDimension(Dimension("dim", 1))
        val gramps: Group.Builder = Group.Builder("gramps").addGroup(parent).addAttribute(Attribute.from("zoom", "pafigliano"))
        val uncle: Group.Builder = Group.Builder("uncle").addVariable(voff).addDimension(Dimension("dim", 2))
        val root: Group = Group.Builder("")
            .addGroup(gramps)
            .addGroup(uncle)
            .addVariable(vroot)
            .addDimension(Dimension("wit", 3))
            .addAttribute(Attribute.from("wit", "tee"))
            .build(null)
        println(root.cdl(true))
        val fullNames = CdmFullNames(root)

        assertNull(fullNames.findAttribute("/gramps/parent/vleaf"))
        assertNull(fullNames.findAttribute("/wittee"))
        assertNotNull(fullNames.findAttribute("@wit"))
        assertNotNull(fullNames.findAttribute("/@wit"))
        assertNotNull(fullNames.findAttribute("/uncle/voff@zoom"))
        assertNotNull(fullNames.findAttribute("/gramps/@zoom"))
        assertNotNull(fullNames.findAttribute("/gramps@zoom"))
        assertNotNull(fullNames.findAttribute("/gramps/zoom"))
        assertNull(fullNames.findAttribute("/gramps/voff@zoom"))
        assertNull(fullNames.findAttribute("/gramps/voff@schwartz"))
        assertNull(fullNames.findAttribute("/"))
        assertNull(fullNames.findAttribute("/wtf/is/this"))
    }

}