package com.jaguar.gearbox.logic

import org.jsoup.parser.Parser
import org.jsoup.nodes.Node
import java.util.ArrayDeque

enum class HtmlFormatErrorKind { INPUT_LIMIT, DEPTH_LIMIT, NODE_LIMIT, OUTPUT_LIMIT, UNSUPPORTED, PARSE_ERROR }
data class HtmlFormatResult(val output: String?=null,val error: HtmlFormatErrorKind?=null)
private class HtmlOutputLimit : RuntimeException()
private class BoundedHtmlOutput : Appendable {
    private val value=StringBuilder()
    override fun append(c: Char): Appendable {
        if(value.length>=256000) throw HtmlOutputLimit()
        value.append(c);return this
    }
    override fun append(csq: CharSequence?): Appendable {
        val text=csq ?: "null"
        return append(text,0,text.length)
    }
    override fun append(csq: CharSequence?,start: Int,end: Int): Appendable {
        val text=csq ?: "null"
        require(start>=0 && end>=start && end<=text.length)
        if(end-start>256000-value.length) throw HtmlOutputLimit()
        value.append(text,start,end);return this
    }
    override fun toString()=value.toString()
}
/** Offline HTML full-document normalization, NOT validation, rendering or sanitization. */
fun evaluateHtml(source: String,mode: JsonFormatMode=JsonFormatMode.PRETTY): HtmlFormatResult {
    if(source.length>64000) return HtmlFormatResult(error=HtmlFormatErrorKind.INPUT_LIMIT)
    if(mode!=JsonFormatMode.PRETTY) return HtmlFormatResult(error=HtmlFormatErrorKind.UNSUPPORTED)
    try {
        // Empty base URI and String input: no connections, resources or scripts are opened.
        val document=Parser.htmlParser().setMaxDepth(128).parseInput(source,"")
        val stack=ArrayDeque<Pair<Node,Int>>()
        stack.add(document to 0)
        var count=0
        while(stack.isNotEmpty()) {
            val (node,depth)=stack.removeLast()
            if(depth>64) return HtmlFormatResult(error=HtmlFormatErrorKind.DEPTH_LIMIT)
            count+=1+node.attributes().size()
            if(count>20000) return HtmlFormatResult(error=HtmlFormatErrorKind.NODE_LIMIT)
            for(child in node.childNodes()) stack.add(child to depth+1)
        }
        document.outputSettings().prettyPrint(false).charset(Charsets.UTF_8)
        val output=BoundedHtmlOutput()
        document.outerHtml(output)
        return HtmlFormatResult(output=output.toString())
    } catch(_:HtmlOutputLimit) {return HtmlFormatResult(error=HtmlFormatErrorKind.OUTPUT_LIMIT)}
    catch(_:Exception) {return HtmlFormatResult(error=HtmlFormatErrorKind.PARSE_ERROR)}
}
