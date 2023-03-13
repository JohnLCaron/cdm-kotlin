package com.sunya.netchdf.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.sunya.cdm.util.Indent

class OdlStruct(val name : String) {
    val nested = mutableListOf<OdlStruct>()
    override fun toString(): String {
        return toString(Indent(2))
    }
    fun toString(indent : Indent): String {
        return buildString {
            append("$indent$name\n")
            nested.forEach{ append( it.toString(indent.incr())) }
        }
    }
}

object OdlParser : Grammar<OdlStruct>() {
    val GROUP by literalToken("GROUP")
    val END_GROUP by literalToken("END_GROUP")
    val END by literalToken("END")
    val EQUAL by literalToken("=")

    val SwathStructure by regexToken("SwathStructure")
    val GridStructure by regexToken("GridStructure")
    val PointStructure by regexToken("PointStructure")

    val ID by regexToken("\\b[A-Za-z_]+\\b")
    val NUM by regexToken("-?\\d+")
    val NAME by regexToken("\\w+") // LOOK trouble: any chars ??
    val TEXT by regexToken("\\b[a-zA-Z0-9_+-.]*\\b")

    val nameToken by NAME use { text }
    val id by ID use { text }
    val number by NUM use { text.toInt() }

    val groupStart by (GROUP and EQUAL and nameToken).map { (_, _, name) ->
        name
    }

    val groupEnd by (END_GROUP and EQUAL and nameToken).map { (_, _, name) ->
        name
    }

    fun makeGroup() : Parser<OdlStruct> {
        return (groupStart and zeroOrMore(group) and groupEnd).map { (name1 : String, nested : List<OdlStruct>, name2 : String ) ->
            val result = OdlStruct(name1)
            result.nested.addAll(nested)
            result
        }
    }

    val group : Parser<OdlStruct> by (groupStart and zeroOrMore(group) and groupEnd).map {
            (name1 : String, nested : List<OdlStruct>, name2 : String ) ->
        val result = OdlStruct(name1)
        result.nested.addAll(nested)
        result
    }

    val odlstruct : Parser<OdlStruct> by (oneOrMore(group) and END).map { (nested,_) ->
        val result = OdlStruct("root")
        result.nested.addAll(nested)
        result
    }

    override val rootParser by odlstruct
}