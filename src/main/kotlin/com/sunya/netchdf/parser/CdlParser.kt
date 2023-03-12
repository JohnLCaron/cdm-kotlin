package com.sunya.netchdf.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.st.SyntaxTree
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar
import com.sunya.cdm.api.*

object CdlParser : Grammar<Netcdf>() {
    val NETCDF by regexToken("netcdf")
    val DIMENSIONS by literalToken("dimensions:")
    val VARIABLES by literalToken("variables:")
    val ATTRIBUTES by literalToken("attributes:")
    val UNLIMITED by literalToken("UNLIMITED ; //")
    val CURRENTLY by literalToken("currently")

    val BYTE by regexToken("\\bbyte\\b")
    val DOUBLE by regexToken("\\bdouble\\b")
    val FLOAT by regexToken("\\bfloat\\b")
    val INT by regexToken("\\bint\\b")
    val SHORT by regexToken("\\bshort\\b")

    val EQUAL by literalToken("=")
    val COLON by literalToken(":")
    val SEMICOLON by literalToken(";")
    val OPENP by literalToken("(")
    val CLOSEP by literalToken(")")
    val OPENB by literalToken("{")
    val CLOSEB by literalToken("}")
    val QUOTE by literalToken("\"")
    val NOTQUOTE by regexToken("^\"")

    val ATTNAME by regexToken("[A-Za-z_]+:[A-Za-z_]+") // LOOK trouble: any chars ??
    val ID by regexToken("\\b[A-Za-z_]+\\b")
    val NUM by regexToken("-?\\d+")
    val NAME by regexToken("\\w+") // LOOK trouble: any chars ??
    val TEXT by regexToken("\\b[a-zA-Z0-9_+-.]*\\b")

    val COMMA by literalToken(",", ignore = true)
    val WS by regexToken("\\s+", ignore = true)

    // val NEWLINE by regexToken("(\\r\\n|\\r|\\n)", ignore = true)

    val name by NAME use { text }
    val id by ID use { text }
    val number by NUM use { text.toInt() }

    val regularDimension by (id and EQUAL and number and SEMICOLON).map { (name, _, length) ->
        Dimension(name, length)
    }
    val unlimitedDimension by (id and EQUAL and UNLIMITED and OPENP and number and CURRENTLY and CLOSEP).map {
            (name, _, _, _, length) ->
        Dimension(name, length, true, true)
    }
    val dimensions by (DIMENSIONS and zeroOrMore(regularDimension or unlimitedDimension)).map { (_, dims) ->
        dims
    }

    val attributeQuoted by (ATTNAME and EQUAL and QUOTE and ID and QUOTE and SEMICOLON).map {
            (attname, _, _, value) ->
        val name = attname.text.substringAfter(':')
        Attribute(name, value.text)
    }
    val attribute by (ATTNAME and EQUAL and TEXT and SEMICOLON).map {
            (attname, _, _, value) ->
        val name = attname.text.substringAfter(':')
        Attribute(name, value.text)
    }
    val attributes by (zeroOrMore(attribute or attributeQuoted)).map { atts ->
        atts
    }

    val typename by BYTE or DOUBLE or FLOAT or INT or SHORT
    val dimList by OPENP and zeroOrMore(id) and CLOSEP and SEMICOLON
    val variable by (typename and id and dimList and attributes).map {
            (type, name, dimList, atts) ->
        val vb = Variable.Builder()
        vb.name = name
        vb.datatype = Datatype.from(type.text.trim())
        vb.dimList = dimList.t2.map { it -> it }
        atts.forEach { vb.attributes.add(it) }
        vb
    }
    val variables by (VARIABLES and zeroOrMore(variable)).map { (_, variables) ->
        variables
    }

    val group : Parser<Group> by (dimensions and variables and attributes).map { (dims, variables, atts) ->
        val gb = Group.Builder("")
        dims.forEach { gb.addDimension(it)}
        variables.forEach { gb.addVariable(it)}
        gb.build(null)
    }

    val netcdf : Parser<Netcdf> by  (NETCDF and (name or id) and OPENB and group and CLOSEB).map {
        (_, location, _, rootGroup, _) ->
        NetcdfCdl(location, rootGroup)
    }

    override val rootParser by netcdf
}

fun printSyntaxTree(cdl: String) {
    val cdlGrammer = CdlParser.liftToSyntaxTreeGrammar()
    when (val result = cdlGrammer.tryParseToEnd(cdl)) {
        is ErrorResult -> println("Could not parse expression: $result")
        is Parsed<SyntaxTree<Netcdf>> -> printSyntaxTree(cdl, result.value)
    }
}

fun printSyntaxTree(expr: String, syntaxTree: SyntaxTree<*>) {
    println("printSyntaxTree:")
    var currentLayer: List<SyntaxTree<*>> = listOf(syntaxTree)
    while (currentLayer.isNotEmpty()) {
        val underscores = currentLayer.flatMap { t ->
            t.range.map { index -> index to charByTree(index, t) }
        }.toMap()
        // expr.indices.forEach{ println("u  $it -> ${underscores[it]}")}
        val underscoreStr = expr.indices.map { underscores[it] ?: ' ' }.joinToString("")
        println(underscoreStr)
        currentLayer = currentLayer.flatMap { it.children }
    }
}

fun charByTree(index : Int, tree: SyntaxTree<*>) : Char {
    with (CdlParser) {
        return when (tree.parser) {
            NETCDF -> 'N'
            VARIABLES -> 'V'
            DIMENSIONS -> 'D'

            BYTE -> 'b'
            DOUBLE -> 'd'
            FLOAT -> 'f'
            INT -> 'i'
            SHORT -> 's'
            ATTNAME -> 'a'

            OPENP -> '('
            CLOSEP -> ')'
            OPENB -> '{'
            CLOSEB -> '}'
            EQUAL -> '='
            QUOTE -> '"'
            COLON -> ':'
            SEMICOLON -> ';'
            COMMA -> ','

            id -> 'i'
            name -> 'i'
            NAME -> 'i'
            number -> '$'
            NUM -> '$'
            netcdf -> 'N'
            attribute -> 'A'
            dimensions -> 'D'
            group -> 'G'
            variables -> 'V'
            variable -> 'v'
            typename -> 't'
            dimList -> 'd'
            else -> 'x'
        }
    }
}