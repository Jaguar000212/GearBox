package com.jaguar.gearbox.logic

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

enum class XmlFormatErrorKind { SYNTAX, UNSAFE_DECLARATION, INPUT_LIMIT, DEPTH_LIMIT, TOKEN_LIMIT, OUTPUT_LIMIT, UNSUPPORTED }
data class XmlFormatError(val kind: XmlFormatErrorKind, val line: Int=1, val column: Int=1)
data class XmlFormatResult(val output: String?=null,val error: XmlFormatError?=null)
internal fun legalXml10(value: String): Boolean {
    var i=0
    while(i<value.length) {
        val c=value[i]
        val point=if(c.isHighSurrogate()) {
            if(i+1>=value.length || !value[i+1].isLowSurrogate()) return false
            Character.toCodePoint(c,value[++i])
        } else {if(c.isLowSurrogate()) return false;c.code}
        if(point!=9 && point!=10 && point!=13 && point !in 0x20..0xD7FF && point !in 0xE000..0xFFFD && point !in 0x10000..0x10FFFF) return false
        i++
    }
    return true
}
/** XML 1.0 only. Platform parsing validates; the second pass only lays out start-tag attributes. */
fun evaluateXml(source: String, mode: JsonFormatMode, indent: Int=2): XmlFormatResult {
    fun error(kind: XmlFormatErrorKind,line:Int=1,column:Int=1)=XmlFormatResult(error=XmlFormatError(kind,line.coerceAtLeast(1),column.coerceAtLeast(1)))
    if(source.length>64000) return error(XmlFormatErrorKind.INPUT_LIMIT)
    if(mode==JsonFormatMode.MINIFY || indent !in listOf(2,4)) return error(XmlFormatErrorKind.UNSUPPORTED)
    if(!legalXml10(source)) return error(XmlFormatErrorKind.SYNTAX)
    if(Regex("<!\\s*(DOCTYPE|ENTITY)",RegexOption.IGNORE_CASE).containsMatchIn(source)) return error(XmlFormatErrorKind.UNSAFE_DECLARATION)
    val version=Regex("^\\uFEFF?<\\?xml\\s+version\\s*=\\s*(['\"])(.*?)\\1").find(source)?.groupValues?.get(2)
    if(version!=null && version!="1.0") return error(XmlFormatErrorKind.UNSUPPORTED)
    val parser=try {
        XmlPullParserFactory.newInstance().newPullParser().also {
            it.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true)
            it.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false)
            // Android defaults validation off; do not accept a provider that enables it.
            check(it.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES))
            check(!it.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL) && !it.getFeature(XmlPullParser.FEATURE_VALIDATION))
        }
    } catch(_:Exception) { return error(XmlFormatErrorKind.UNSUPPORTED) }
    try {
        parser.setInput(StringReader(source))
        var tokens=0
        var roots=0
        while(true) {
            val token=parser.nextToken()
            if(token==XmlPullParser.END_DOCUMENT) break
            if(++tokens>20000) return error(XmlFormatErrorKind.TOKEN_LIMIT)
            if(parser.depth>64) return error(XmlFormatErrorKind.DEPTH_LIMIT)
            if(token==XmlPullParser.DOCDECL) return error(XmlFormatErrorKind.UNSAFE_DECLARATION)
            if(token==XmlPullParser.ENTITY_REF && parser.text==null) return error(XmlFormatErrorKind.UNSAFE_DECLARATION)
            if(parser.text?.let { !legalXml10(it) }==true) return error(XmlFormatErrorKind.SYNTAX)
            if(token==XmlPullParser.START_TAG) {
                if(parser.depth==1 && ++roots>1) return error(XmlFormatErrorKind.SYNTAX)
                tokens+=parser.attributeCount
                if(tokens>20000) return error(XmlFormatErrorKind.TOKEN_LIMIT)
                val names=HashSet<Pair<String,String>>()
                for(i in 0 until parser.attributeCount) {
                    if(!names.add(parser.getAttributeNamespace(i) to parser.getAttributeName(i)) || !legalXml10(parser.getAttributeValue(i))) return error(XmlFormatErrorKind.SYNTAX)
                }
            }
        }
        if(roots!=1) return error(XmlFormatErrorKind.SYNTAX)
    } catch(_:Exception) { return error(XmlFormatErrorKind.SYNTAX,parser.lineNumber,parser.columnNumber) }
    if(mode==JsonFormatMode.VALIDATE) return XmlFormatResult(output="")
    // Not a validator: the document above has already passed the platform parser.
    val output=StringBuilder()
    var i=0
    while(i<source.length) {
        if(source[i]!='<') {output.append(source[i++]);continue}
        val terminal=when {
            source.startsWith("<!--",i) -> "-->"
            source.startsWith("<![CDATA[",i) -> "]]>"
            source.startsWith("<?",i) -> "?>"
            source.startsWith("</",i) -> ">"
            else -> null
        }
        if(terminal!=null) {
            val end=source.indexOf(terminal,i)+terminal.length
            if(end<i+terminal.length) return error(XmlFormatErrorKind.SYNTAX)
            output.append(source,i,end);i=end;continue
        }
        output.append(source[i++])
        while(i<source.length && !source[i].isWhitespace() && source[i]!='>' && source[i]!='/') output.append(source[i++])
        while(i<source.length && source[i]!='>') {
            val whitespaceStart=i
            while(i<source.length && source[i].isWhitespace()) i++
            if(i>=source.length || source[i]=='>' || source[i]=='/') {
                output.append(source,whitespaceStart,i)
                if(i<source.length && source[i]=='/') output.append(source[i++])
                continue
            }
            output.append('\n').append(" ".repeat(indent))
            // Copy the entire attribute verbatim, including whitespace around '=' and quotes.
            val attributeStart=i
            while(i<source.length && source[i]!='\'' && source[i]!='"') i++
            if(i>=source.length) return error(XmlFormatErrorKind.SYNTAX)
            val quote=source[i++]
            while(i<source.length && source[i]!=quote) i++
            if(i>=source.length) return error(XmlFormatErrorKind.SYNTAX)
            i++
            output.append(source,attributeStart,i)
        }
        if(i<source.length) output.append(source[i++])
        if(output.length>256000) return error(XmlFormatErrorKind.OUTPUT_LIMIT)
    }
    return if(output.length>256000) error(XmlFormatErrorKind.OUTPUT_LIMIT) else XmlFormatResult(output=output.toString())
}
