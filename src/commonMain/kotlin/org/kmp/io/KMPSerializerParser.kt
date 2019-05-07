/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */


package org.kmp.io

import kotlinx.io.IOException
import kotlinx.io.OutputStream
import kotlinx.io.Writer
import org.kmp.OutputStreamWriter

class KMPSerializerParser : KMPXmlSerializer {

    //    static final String UNDEFINED = ":";

    private var writer: Writer? = null

    private var pending: Boolean = false
    private var auto: Int = 0
    private var depth: Int = 0

    private var elementStack = arrayOfNulls<String>(12)
    //nsp/prefix/name
    private var nspCounts = IntArray(4)
    private var nspStack = arrayOfNulls<String>(8)
    //prefix/nsp; both empty are ""
    private var indent = BooleanArray(4)
    private var unicode: Boolean = false
    private var encoding: String? = null

    private fun check(close: Boolean) {
        if (!pending)
            return

        depth++
        pending = false

        if (indent.size <= depth) {
            val hlp = BooleanArray(depth + 4)
            System.arraycopy(indent, 0, hlp, 0, depth)
            indent = hlp
        }
        indent[depth] = indent[depth - 1]

        for (i in nspCounts[depth - 1] until nspCounts[depth]) {
            writer!!.write(' ')
            writer!!.write("xmlns")
            if ("" != nspStack[i * 2]) {
                writer!!.write(':')
                writer!!.write(nspStack[i * 2])
            } else if ("" == namespace && "" != nspStack[i * 2 + 1])
                throw IllegalStateException("Cannot set default namespace for elements in no namespace")
            writer!!.write("=\"")
            writeEscaped(nspStack[i * 2 + 1], '"')
            writer!!.write('"')
        }

        if (nspCounts.size <= depth + 1) {
            val hlp = IntArray(depth + 8)
            System.arraycopy(nspCounts, 0, hlp, 0, depth + 1)
            nspCounts = hlp
        }

        nspCounts[depth + 1] = nspCounts[depth]
        //   nspCounts[depth + 2] = nspCounts[depth];

        writer!!.write(if (close) " />" else ">")
    }


    private fun writeEscaped(s: String, quot: Int) {

        var i = 0
        while (i < s.length) {
            val c = s[i]
            when (c) {
                '\n', '\r', '\t' -> if (quot == -1)
                    writer!!.write(c.toInt())
                else
                    writer!!.write("&#" + c.toInt() + ';'.toString())
                '&' -> writer!!.write("&amp;")
                '>' -> writer!!.write("&gt;")
                '<' -> writer!!.write("&lt;")
                '"', '\'' -> {
                    if (c.toInt() == quot) {
                        writer!!.write(
                            if (c == '"') "&quot;" else "&apos;"
                        )
                        break
                    }
                    //if(c < ' ')
                    //	throw new IllegalArgumentException("Illegal control code:"+((int) c));

                    if (i < s.length - 1) {
                        val cLow = s[i + 1]
                        // c is high surrogate and cLow is low surrogate
                        if (c.toInt() >= 0xd800 && c.toInt() <= 0xdbff && cLow.toInt() >= 0xdc00 && cLow.toInt() <= 0xdfff) {
                            // write surrogate pair as single code point
                            val n = (c.toInt() - 0xd800 shl 10) + (cLow.toInt() - 0xdc00) + 0x010000
                            writer!!.write("&#$n;")
                            i++ // Skip the low surrogate
                            break
                        }
                        // Does nothing smart about orphan surrogates, just output them "as is"
                    }
                    if (c >= ' ' && c != '@' && (c.toInt() < 127 || unicode)) {
                        writer!!.write(c.toInt())
                    } else {
                        writer!!.write("&#" + c.toInt() + ";")
                    }
                }
                else -> {
                    if (i < s.length - 1) {
                        val cLow = s[i + 1]
                        if (c.toInt() >= 0xd800 && c.toInt() <= 0xdbff && cLow.toInt() >= 0xdc00 && cLow.toInt() <= 0xdfff) {
                            val n = (c.toInt() - 0xd800 shl 10) + (cLow.toInt() - 0xdc00) + 0x010000
                            writer!!.write("&#$n;")
                            i++
                            break
                        }
                    }
                    if (c >= ' ' && c != '@' && (c.toInt() < 127 || unicode)) {
                        writer!!.write(c.toInt())
                    } else {
                        writer!!.write("&#" + c.toInt() + ";")
                    }
                }
            }
            i++
        }
    }

    /*
    	private final void writeIndent() throws IOException {
    		writer.write("\r\n");
    		for (int i = 0; i < depth; i++)
    			writer.write(' ');
    	}*/

    override fun docdecl(dd: String) {
        writer!!.write("<!DOCTYPE")
        writer!!.write(dd)
        writer!!.write(">")
    }

    override fun endDocument() {
        while (depth > 0) {
            endTag(
                elementStack[depth * 3 - 3],
                elementStack[depth * 3 - 1]
            )
        }
        flush()
    }


    override fun entityRef(name: String) {
        check(false)
        writer!!.write('&')
        writer!!.write(name)
        writer!!.write(';')
    }

    override fun getFeature(name: String): Boolean {
        //return false;
        return if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name)
            indent[depth]
        else
            false
    }

    override fun getPrefix(namespace: String, create: Boolean): String? {
        try {
            return getPrefix(namespace, false, create)
        } catch (e: IOException) {
            throw RuntimeException(e.toString())
        }

    }

    override fun getNamespace(): String? {
        return if (getDepth() == 0) null else elementStack[getDepth() * 3 - 3]
    }


    override fun getName(): String? {
        return if (getDepth() == 0) null else elementStack[getDepth() * 3 - 1]
    }

    private fun getPrefix(namespace: String, includeDefault: Boolean, create: Boolean): String? {

        run {
            var i = nspCounts[depth + 1] * 2 - 2
            while (i >= 0) {
                if (nspStack[i + 1] == namespace && (includeDefault || nspStack[i] != "")) {
                    var cand: String? = nspStack[i]
                    for (j in i + 2 until nspCounts[depth + 1] * 2) {
                        if (nspStack[j] == cand) {
                            cand = null
                            break
                        }
                    }
                    if (cand != null)
                        return cand
                }
                i -= 2
            }
        }

        if (!create)
            return null

        var prefix: String?

        if ("" == namespace)
            prefix = ""
        else {
            do {
                prefix = "n" + auto++
                var i = nspCounts[depth + 1] * 2 - 2
                while (i >= 0) {
                    if (prefix == nspStack[i]) {
                        prefix = null
                        break
                    }
                    i -= 2
                }
            } while (prefix == null)
        }

        val p = pending
        pending = false
        setPrefix(prefix, namespace)
        pending = p
        return prefix
    }

    override fun getProperty(name: String): Any {
        throw RuntimeException("Unsupported property")
    }


    override fun ignorableWhitespace(s: String) {
        text(s)
    }

    override fun setFeature(name: String, value: Boolean) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name) {
            indent[depth] = value
        } else
            throw RuntimeException("Unsupported Feature")
    }

    override fun setProperty(name: String, value: Any) {
        throw RuntimeException(
            "Unsupported Property:$value"
        )
    }

    override fun setPrefix(prefix: String?, namespace: String?) {
        var prefix = prefix
        var namespace = namespace

        check(false)
        if (prefix == null)
            prefix = ""
        if (namespace == null)
            namespace = ""

        val defined = getPrefix(namespace, true, false)

        // boil out if already defined

        if (prefix == defined)
            return

        var pos = nspCounts[depth + 1]++ shl 1

        if (nspStack.size < pos + 1) {
            val hlp = arrayOfNulls<String>(nspStack.size + 16)
            System.arraycopy(nspStack, 0, hlp, 0, pos)
            nspStack = hlp
        }

        nspStack[pos++] = prefix
        nspStack[pos] = namespace
    }

    override fun setOutput(writer: Writer) {
        this.writer = writer

        // elementStack = new String[12]; //nsp/prefix/name
        //nspCounts = new int[4];
        //nspStack = new String[8]; //prefix/nsp
        //indent = new boolean[4];

        nspCounts[0] = 2
        nspCounts[1] = 2
        nspStack[0] = ""
        nspStack[1] = ""
        nspStack[2] = "xml"
        nspStack[3] = "http://www.w3.org/XML/1998/namespace"
        pending = false
        auto = 0
        depth = 0

        unicode = false
    }


    override fun setOutput(os: OutputStream?, encoding: String?) {
        if (os == null)
            throw IllegalArgumentException()
        setOutput(
            if (encoding == null)
                OutputStreamWriter(os!!)
            else
                OutputStreamWriter(os!!, encoding)
        )
        this.encoding = encoding
        if (encoding != null && encoding.toLowerCase().startsWith("utf"))
            unicode = true
    }


    override fun startDocument(encoding: String?, standalone: Boolean?) {
        writer!!.write("<?xml version='1.0' ")

        if (encoding != null) {
            this.encoding = encoding
            if (encoding.toLowerCase().startsWith("utf"))
                unicode = true
        }

        if (this.encoding != null) {
            writer!!.write("encoding='")
            writer!!.write(this.encoding!!)
            writer!!.write("' ")
        }

        if (standalone != null) {
            writer!!.write("standalone='")
            writer!!.write(
                if (standalone.booleanValue()) "yes" else "no"
            )
            writer!!.write("' ")
        }
        writer!!.write("?>")
    }


    override fun startTag(namespace: String?, name: String): KMPXmlSerializer {
        check(false)

        //        if (namespace == null)
        //            namespace = "";

        if (indent[depth]) {
            writer!!.write("\r\n")
            for (i in 0 until depth)
                writer!!.write("  ")
        }

        var esp = depth * 3

        if (elementStack.size < esp + 3) {
            val hlp = arrayOfNulls<String>(elementStack.size + 12)
            System.arraycopy(elementStack, 0, hlp, 0, esp)
            elementStack = hlp
        }

        val prefix = if (namespace == null)
            ""
        else
            getPrefix(namespace, true, true)

        if ("" == namespace) {
            for (i in nspCounts[depth] until nspCounts[depth + 1]) {
                if ("" == nspStack[i * 2] && "" != nspStack[i * 2 + 1]) {
                    throw IllegalStateException("Cannot set default namespace for elements in no namespace")
                }
            }
        }

        elementStack[esp++] = namespace
        elementStack[esp++] = prefix
        elementStack[esp] = name

        writer!!.write('<')
        if ("" != prefix) {
            writer!!.write(prefix!!)
            writer!!.write(':')
        }

        writer!!.write(name)

        pending = true

        return this
    }


    override fun attribute(namespace: String?, name: String, value: String): KMPXmlSerializer {
        var namespace = namespace
        if (!pending)
            throw IllegalStateException("illegal position for attribute")

        //        int cnt = nspCounts[depth];

        if (namespace == null)
            namespace = ""

        //		depth--;
        //		pending = false;

        val prefix = if ("" == namespace)
            ""
        else
            getPrefix(namespace, false, true)

        //		pending = true;
        //		depth++;

        /*        if (cnt != nspCounts[depth]) {
                    writer.write(' ');
                    writer.write("xmlns");
                    if (nspStack[cnt * 2] != null) {
                        writer.write(':');
                        writer.write(nspStack[cnt * 2]);
                    }
                    writer.write("=\"");
                    writeEscaped(nspStack[cnt * 2 + 1], '"');
                    writer.write('"');
                }
                */

        writer!!.write(' ')
        if ("" != prefix) {
            writer!!.write(prefix!!)
            writer!!.write(':')
        }
        writer!!.write(name)
        writer!!.write('=')
        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer!!.write(q.toInt())
        writeEscaped(value, q.toInt())
        writer!!.write(q.toInt())

        return this
    }

    override fun flush() {
        check(false)
        writer!!.flush()
    }

    /*
    	public void close() throws IOException {
    		check();
    		writer.close();
    	}
    */

    override fun endTag(namespace: String?, name: String): KMPXmlSerializer {

        if (!pending)
            depth--
        //        if (namespace == null)
        //          namespace = "";

        if (namespace == null && elementStack[depth * 3] != null
            || namespace != null && namespace != elementStack[depth * 3]
            || elementStack[depth * 3 + 2] != name
        )
            throw IllegalArgumentException("</{$namespace}$name> does not match start")

        if (pending) {
            check(true)
            depth--
        } else {
            if (indent[depth + 1]) {
                writer!!.write("\r\n")
                for (i in 0 until depth)
                    writer!!.write("  ")
            }

            writer!!.write("</")
            val prefix = elementStack[depth * 3 + 1]
            if ("" != prefix) {
                writer!!.write(prefix)
                writer!!.write(':')
            }
            writer!!.write(name)
            writer!!.write('>')
        }

        nspCounts[depth + 1] = nspCounts[depth]
        return this
    }

    override fun getDepth(): Int {
        return if (pending) depth + 1 else depth
    }


    override fun text(text: String): KMPXmlSerializer {
        check(false)
        indent[depth] = false
        writeEscaped(text, -1)
        return this
    }


    override fun text(text: CharArray, start: Int, len: Int): KMPXmlSerializer {
        text(String(text, start, len))
        return this
    }


    override fun cdsect(data: String) {
        check(false)
        writer!!.write("<![CDATA[")
        writer!!.write(data)
        writer!!.write("]]>")
    }


    override fun comment(comment: String) {
        check(false)
        writer!!.write("<!--")
        writer!!.write(comment)
        writer!!.write("-->")
    }


    override fun processingInstruction(pi: String) {
        check(false)
        writer!!.write("<?")
        writer!!.write(pi)
        writer!!.write("?>")
    }
}