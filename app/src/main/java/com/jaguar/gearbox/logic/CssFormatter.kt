package com.jaguar.gearbox.logic

enum class CssFormatErrorKind { INPUT_LIMIT, DEPTH_LIMIT, TOKEN_LIMIT, OUTPUT_LIMIT, UNSUPPORTED }
data class CssFormatResult(val output: String?=null,val error: CssFormatErrorKind?=null)
/** Layout only: insert LF after complete top-level blocks/statements; never rewrite interiors. */
fun evaluateCss(source: String,mode: JsonFormatMode=JsonFormatMode.PRETTY): CssFormatResult {
    fun error(kind: CssFormatErrorKind)=CssFormatResult(error=kind)
    if(source.length>64000) return error(CssFormatErrorKind.INPUT_LIMIT)
    if(mode!=JsonFormatMode.PRETTY) return error(CssFormatErrorKind.UNSUPPORTED)
    var check=0
    while(check<source.length) {
        val c=source[check]
        if(c=='\u0000') return error(CssFormatErrorKind.UNSUPPORTED)
        if(c.isHighSurrogate()) {if(check+1>=source.length || !source[++check].isLowSurrogate()) return error(CssFormatErrorKind.UNSUPPORTED)}
        else if(c.isLowSurrogate()) return error(CssFormatErrorKind.UNSUPPORTED)
        check++
    }
    val output=StringBuilder()
    val stack=ArrayDeque<Char>()
    var i=0
    var tokens=0
    var statement=false
    var atRule=false
    fun boundary() { if(i<source.length && source[i]!='\n' && source[i]!='\r') output.append('\n') }
    // Consume a CSS escape without interpreting or normalizing its spelling.
    fun escape(quoted: Boolean): Boolean {
        output.append(source[i++])
        if(i>=source.length) return false
        if(source[i]=='\n' || source[i]=='\r' || source[i]=='\u000C') {
            if(!quoted) return false
            val c=source[i++];output.append(c)
            if(c=='\r' && i<source.length && source[i]=='\n') output.append(source[i++])
            return true
        }
        if(source[i].digitToIntOrNull(16)!=null) {
            var digits=0
            while(i<source.length && digits<6 && source[i].digitToIntOrNull(16)!=null) {output.append(source[i++]);digits++}
            if(i<source.length && source[i] in " \t\r\n\u000C") {
                val c=source[i++];output.append(c)
                if(c=='\r' && i<source.length && source[i]=='\n') output.append(source[i++])
            }
        } else output.append(source[i++])
        return true
    }
    while(i<source.length) {
        if(++tokens>20000) return error(CssFormatErrorKind.TOKEN_LIMIT)
        val c=source[i]
        if(source.startsWith("/*",i)) {
            val end=source.indexOf("*/",i+2)
            if(end<0) return error(CssFormatErrorKind.UNSUPPORTED)
            output.append(source,i,end+2);i=end+2;continue
        }
        if(c=='\\') {
            if(stack.isEmpty() && !statement) {statement=true;atRule=false}
            if(!escape(false)) return error(CssFormatErrorKind.UNSUPPORTED)
            continue
        }
        if(c=='\'' || c=='"') {
            if(stack.isEmpty() && !statement) {statement=true;atRule=false}
            output.append(c);i++
            var closed=false
            while(i<source.length) {
                if(source[i]==c) {output.append(source[i++]);closed=true;break}
                if(source[i]=='\\') {if(!escape(true)) return error(CssFormatErrorKind.UNSUPPORTED)}
                else {if(source[i] in "\r\n\u000C") return error(CssFormatErrorKind.UNSUPPORTED);output.append(source[i++])}
            }
            if(!closed) return error(CssFormatErrorKind.UNSUPPORTED)
            continue
        }
        if(!c.isWhitespace() && stack.isEmpty() && !statement) {statement=true;atRule=c=='@'}
        when(c) {
            '(', '[', '{' -> {stack.addLast(c);if(stack.size>64) return error(CssFormatErrorKind.DEPTH_LIMIT)}
            ')', ']', '}' -> {
                val expected=when(c){')'->'(';']'->'[';else->'{'}
                if(stack.isEmpty() || stack.removeLast()!=expected) return error(CssFormatErrorKind.UNSUPPORTED)
                output.append(c);i++
                if(stack.isEmpty() && c=='}') {statement=false;atRule=false;boundary()}
                continue
            }
            ';' -> if(stack.isEmpty()) {
                if(!atRule) return error(CssFormatErrorKind.UNSUPPORTED)
                output.append(c);i++;statement=false;atRule=false;boundary();continue
            }
        }
        output.append(c);i++
        if(output.length>256000) return error(CssFormatErrorKind.OUTPUT_LIMIT)
    }
    if(stack.isNotEmpty() || statement) return error(CssFormatErrorKind.UNSUPPORTED)
    return if(output.length>256000) error(CssFormatErrorKind.OUTPUT_LIMIT) else CssFormatResult(output=output.toString())
}
