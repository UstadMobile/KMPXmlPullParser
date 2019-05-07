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

// Contributors: Paul Hackenberger (unterminated entity handling in relaxed mode)

package org.kmp.io


/** A simple, pull based XML parser. This classe replaces the kXML 1
 * XmlParser class and the corresponding event classes.  */

class KMPXmlParser : KMPPullParser {

    private var location: Any? = null

    // general

    private var version: String? = null
    private var standalone: Boolean? = null

    private var processNsp: Boolean = false
    private var relaxed: Boolean = false
    private var entityMap: Hashtable<*, *>? = null
    private var depth: Int = 0
    private var elementStack = arrayOfNulls<String>(16)
    private var nspStack = arrayOfNulls<String>(8)
    private var nspCounts = IntArray(4)

    // source

    private var reader: Reader? = null
    var inputEncoding: String? = null
        private set
    private val srcBuf: CharArray

    private var srcPos: Int = 0
    private var srcCount: Int = 0

    private var line: Int = 0
    private var column: Int = 0

    // txtbuffer

    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var txtBuf = CharArray(128)
    /** Write position   */
    private var txtPos: Int = 0

    // Event-related

    private var type: Int = 0
    private var isWhitespace: Boolean = false
    private var namespace: String? = null
    private var prefix: String? = null
    private var name: String? = null

    private var degenerated: Boolean = false
    private var attributeCount: Int = 0
    private var attributes = arrayOfNulls<String>(16)
    //    private int stackMismatch = 0;
    private var error: String? = null

    /**
     * A separate peek buffer seems simpler than managing
     * wrap around in the first level read buffer  */

    private val peek = IntArray(2)
    private var peekCount: Int = 0
    private var wasCR: Boolean = false

    private var unresolved: Boolean = false
    private var token: Boolean = false

    constructor() {
        srcBuf = CharArray(if (Runtime.getRuntime().freeMemory() >= 1048576) 8192 else 128)
    }

    private fun isProp(n1: String, prop: Boolean, n2: String): Boolean {
        if (!n1.startsWith("http://xmlpull.org/v1/doc/"))
            return false
        return if (prop)
            n1.substring(42) == n2
        else
            n1.substring(40) == n2
    }

    @Throws(XmlPullParserException::class)
    private fun adjustNsp(): Boolean {

        var any = false

        run {
            var i = 0
            while (i < attributeCount shl 2) {
                // * 4 - 4; i >= 0; i -= 4) {

                var attrName: String? = attributes[i + 2]
                val cut = attrName!!.indexOf(':')
                val prefix: String

                if (cut != -1) {
                    prefix = attrName!!.substring(0, cut)
                    attrName = attrName!!.substring(cut + 1)
                } else if (attrName == "xmlns") {
                    prefix = attrName
                    attrName = null
                } else {
                    i += 4
                    continue
                }

                if (prefix != "xmlns") {
                    any = true
                } else {
                    val j = nspCounts[depth]++ shl 1

                    nspStack = ensureCapacity(nspStack, j + 2)
                    nspStack[j] = attrName
                    nspStack[j + 1] = attributes[i + 3]

                    if (attrName != null && attributes[i + 3] == "")
                        error("illegal empty namespace")

                    //  prefixMap = new PrefixMap (prefixMap, attrName, attr.getValue ());

                    //System.out.println (prefixMap);

                    System.arraycopy(
                        attributes,
                        i + 4,
                        attributes,
                        i,
                        (--attributeCount shl 2) - i
                    )

                    i -= 4
                }
                i += 4
            }
        }

        if (any) {
            var i = (attributeCount shl 2) - 4
            while (i >= 0) {

                var attrName = attributes[i + 2]
                val cut = attrName.indexOf(':')

                if (cut == 0 && !relaxed)
                    throw RuntimeException(
                        "illegal attribute name: $attrName at $this"
                    )
                else if (cut != -1) {
                    val attrPrefix = attrName.substring(0, cut)

                    attrName = attrName.substring(cut + 1)

                    val attrNs = getNamespace(attrPrefix)

                    if (attrNs == null && !relaxed)
                        throw RuntimeException(
                            "Undefined Prefix: $attrPrefix in $this"
                        )

                    attributes[i] = attrNs
                    attributes[i + 1] = attrPrefix
                    attributes[i + 2] = attrName

                    /*
                                        if (!relaxed) {
                                            for (int j = (attributeCount << 2) - 4; j > i; j -= 4)
                                                if (attrName.equals(attributes[j + 2])
                                                    && attrNs.equals(attributes[j]))
                                                    exception(
                                                        "Duplicate Attribute: {"
                                                            + attrNs
                                                            + "}"
                                                            + attrName);
                                        }
                        */
                }
                i -= 4
            }
        }

        val cut = name!!.indexOf(':')

        if (cut == 0)
            error("illegal tag name: " + name!!)

        if (cut != -1) {
            prefix = name!!.substring(0, cut)
            name = name!!.substring(cut + 1)
        }

        this.namespace = getNamespace(prefix!!)

        if (this.namespace == null) {
            if (prefix != null)
                error("undefined prefix: " + prefix!!)
            this.namespace = NO_NAMESPACE
        }

        return any
    }

    private fun ensureCapacity(arr: Array<String>, required: Int): Array<String> {
        if (arr.size >= required)
            return arr
        val bigger = arrayOfNulls<String>(required + 16)
        System.arraycopy(arr, 0, bigger, 0, arr.size)
        return bigger
    }

    @Throws(XmlPullParserException::class)
    private fun error(desc: String) {
        if (relaxed) {
            if (error == null)
                error = "ERR: $desc"
        } else
            exception(desc)
    }

    @Throws(XmlPullParserException::class)
    private fun exception(desc: String) {
        throw XmlPullParserException(
            if (desc.length < 100) desc else desc.substring(0, 100) + "\n",
            this, null
        )
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun nextImpl() {

        if (reader == null)
            exception("No Input specified")

        if (type == END_TAG)
            depth--

        while (true) {
            attributeCount = -1

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)

            if (degenerated) {
                degenerated = false
                type = END_TAG
                return
            }


            if (error != null) {
                for (i in 0 until error!!.length)
                    push(error!![i].toInt())
                //				text = error;
                error = null
                type = COMMENT
                return
            }


            //            if (relaxed
            //                && (stackMismatch > 0 || (peek(0) == -1 && depth > 0))) {
            //                int sp = (depth - 1) << 2;
            //                type = END_TAG;
            //                namespace = elementStack[sp];
            //                prefix = elementStack[sp + 1];
            //                name = elementStack[sp + 2];
            //                if (stackMismatch != 1)
            //                    error = "missing end tag /" + name + " inserted";
            //                if (stackMismatch > 0)
            //                    stackMismatch--;
            //                return;
            //            }

            prefix = null
            name = null
            namespace = null
            //            text = null;

            type = peekType()

            when (type) {

                ENTITY_REF -> {
                    pushEntity()
                    return
                }

                START_TAG -> {
                    parseStartTag(false)
                    return
                }

                END_TAG -> {
                    parseEndTag()
                    return
                }

                END_DOCUMENT -> return

                TEXT -> {
                    pushText('<', !token)
                    if (depth == 0) {
                        if (isWhitespace)
                            type = IGNORABLE_WHITESPACE
                        // make exception switchable for instances.chg... !!!!
                        //	else
                        //    exception ("text '"+getText ()+"' not allowed outside root element");
                    }
                    return
                }

                else -> {
                    type = parseLegacy(token)
                    if (type != XML_DECL)
                        return
                }
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseLegacy(push: Boolean): Int {
        var push = push

        var req = ""
        val term: Int
        val result: Int
        var prev = 0

        read() // <
        var c = read()

        if (c == '?'.toInt()) {
            if ((peek(0) == 'x'.toInt() || peek(0) == 'X'.toInt()) && (peek(1) == 'm'.toInt() || peek(1) == 'M'.toInt())) {

                if (push) {
                    push(peek(0))
                    push(peek(1))
                }
                read()
                read()

                if ((peek(0) == 'l'.toInt() || peek(0) == 'L'.toInt()) && peek(1) <= ' '.toInt()) {

                    if (line != 1 || column > 4)
                        error("PI must not start with xml")

                    parseStartTag(true)

                    if (attributeCount < 1 || "version" != attributes[2])
                        error("version expected")

                    version = attributes[3]

                    var pos = 1

                    if (pos < attributeCount && "encoding" == attributes[2 + 4]) {
                        inputEncoding = attributes[3 + 4]
                        pos++
                    }

                    if (pos < attributeCount && "standalone" == attributes[4 * pos + 2]) {
                        val st = attributes[3 + 4 * pos]
                        if ("yes" == st)
                            standalone = true
                        else if ("no" == st)
                            standalone = false
                        else
                            error("illegal standalone value: $st")
                        pos++
                    }

                    if (pos != attributeCount)
                        error("illegal xmldecl")

                    isWhitespace = true
                    txtPos = 0

                    return XML_DECL
                }
            }

            /*            int c0 = read ();
                        int c1 = read ();
                        int */

            term = '?'.toInt()
            result = PROCESSING_INSTRUCTION
        } else if (c == '!'.toInt()) {
            if (peek(0) == '-'.toInt()) {
                result = COMMENT
                req = "--"
                term = '-'.toInt()
            } else if (peek(0) == '['.toInt()) {
                result = CDSECT
                req = "[CDATA["
                term = ']'.toInt()
                push = true
            } else {
                result = DOCDECL
                req = "DOCTYPE"
                term = -1
            }
        } else {
            error("illegal: <$c")
            return COMMENT
        }

        for (i in 0 until req.length)
            read(req[i])

        if (result == DOCDECL)
            parseDoctype(push)
        else {
            while (true) {
                c = read()
                if (c == -1) {
                    error(UNEXPECTED_EOF)
                    return COMMENT
                }

                if (push)
                    push(c)

                if ((term == '?'.toInt() || c == term)
                    && peek(0) == term
                    && peek(1) == '>'.toInt()
                )
                    break

                prev = c
            }

            if (term == '-'.toInt() && prev == '-'.toInt() && !relaxed)
                error("illegal comment delimiter: --->")

            read()
            read()

            if (push && term != '?'.toInt())
                txtPos--

        }
        return result
    }

    /** precondition: &lt! consumed  */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseDoctype(push: Boolean) {

        var nesting = 1
        var quoted = false

        // read();

        while (true) {
            val i = read()
            when (i) {

                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }

                '\'' -> quoted = !quoted

                '<' -> if (!quoted)
                    nesting++

                '>' -> if (!quoted) {
                    if (--nesting == 0)
                        return
                }
            }
            if (push)
                push(i)
        }
    }

    /* precondition: &lt;/ consumed */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseEndTag() {

        read() // '<'
        read() // '/'
        name = readName()
        skip()
        read('>')

        val sp = depth - 1 shl 2

        if (depth == 0) {
            error("element stack empty")
            type = COMMENT
            return
        }

        if (!relaxed) {
            if (name != elementStack[sp + 3]) {
                error("expected: /" + elementStack[sp + 3] + " read: " + name)

                // become case insensitive in relaxed mode

                //            int probe = sp;
                //            while (probe >= 0 && !name.toLowerCase().equals(elementStack[probe + 3].toLowerCase())) {
                //                stackMismatch++;
                //                probe -= 4;
                //            }
                //
                //            if (probe < 0) {
                //                stackMismatch = 0;
                //                //			text = "unexpected end tag ignored";
                //                type = COMMENT;
                //                return;
                //            }
            }

            namespace = elementStack[sp]
            prefix = elementStack[sp + 1]
            name = elementStack[sp + 2]
        }
    }

    @Throws(IOException::class)
    private fun peekType(): Int {
        when (peek(0)) {
            -1 -> return END_DOCUMENT
            '&' -> return ENTITY_REF
            '<' -> when (peek(1)) {
                '/' -> return END_TAG
                '?', '!' -> return LEGACY
                else -> return START_TAG
            }
            else -> return TEXT
        }
    }

    private operator fun get(pos: Int): String {
        return String(txtBuf, pos, txtPos - pos)
    }

    /*
    private final String pop (int pos) {
    String result = new String (txtBuf, pos, txtPos - pos);
    txtPos = pos;
    return result;
    }
    */

    private fun push(c: Int) {

        isWhitespace = isWhitespace and (c <= ' '.toInt())

        if (txtPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            val bigger = CharArray(txtPos * 4 / 3 + 4)
            System.arraycopy(txtBuf, 0, bigger, 0, txtPos)
            txtBuf = bigger
        }

        if (c > 0xffff) {
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            txtBuf[txtPos++] = (offset.ushr(10) + 0xd800).toChar() // high surrogate
            txtBuf[txtPos++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            txtBuf[txtPos++] = c.toChar()
        }
    }

    /** Sets name and attributes  */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseStartTag(xmldecl: Boolean) {

        if (!xmldecl)
            read()
        name = readName()
        attributeCount = 0

        while (true) {
            skip()

            val c = peek(0)

            if (xmldecl) {
                if (c == '?'.toInt()) {
                    read()
                    read('>')
                    return
                }
            } else {
                if (c == '/'.toInt()) {
                    degenerated = true
                    read()
                    skip()
                    read('>')
                    break
                }

                if (c == '>'.toInt() && !xmldecl) {
                    read()
                    break
                }
            }

            if (c == -1) {
                error(UNEXPECTED_EOF)
                //type = COMMENT;
                return
            }

            val attrName = readName()

            if (attrName.length == 0) {
                error("attr name expected")
                //type = COMMENT;
                break
            }

            var i = attributeCount++ shl 2

            attributes = ensureCapacity(attributes, i + 4)

            attributes[i++] = ""
            attributes[i++] = null
            attributes[i++] = attrName

            skip()

            if (peek(0) != '='.toInt()) {
                if (!relaxed) {
                    error("Attr.value missing f. $attrName")
                }
                attributes[i] = attrName
            } else {
                read('=')
                skip()
                var delimiter = peek(0)

                if (delimiter != '\''.toInt() && delimiter != '"'.toInt()) {
                    if (!relaxed) {
                        error("attr value delimiter missing!")
                    }
                    delimiter = ' '.toInt()
                } else
                    read()

                val p = txtPos
                pushText(delimiter, true)

                attributes[i] = get(p)
                txtPos = p

                if (delimiter != ' '.toInt())
                    read() // skip endquote
            }
        }

        val sp = depth++ shl 2

        elementStack = ensureCapacity(elementStack, sp + 4)
        elementStack[sp + 3] = name

        if (depth >= nspCounts.size) {
            val bigger = IntArray(depth + 4)
            System.arraycopy(nspCounts, 0, bigger, 0, nspCounts.size)
            nspCounts = bigger
        }

        nspCounts[depth] = nspCounts[depth - 1]

        /*
        		if(!relaxed){
                for (int i = attributeCount - 1; i > 0; i--) {
                    for (int j = 0; j < i; j++) {
                        if (getAttributeName(i).equals(getAttributeName(j)))
                            exception("Duplicate Attribute: " + getAttributeName(i));
                    }
                }
        		}
        */
        if (processNsp)
            adjustNsp()
        else
            namespace = ""

        elementStack[sp] = namespace
        elementStack[sp + 1] = prefix
        elementStack[sp + 2] = name
    }

    /**
     * result: isWhitespace; if the setName parameter is set,
     * the name of the entity is stored in "name"  */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun pushEntity() {

        push(read()) // &


        val pos = txtPos

        while (true) {
            val c = peek(0)
            if (c == ';'.toInt()) {
                read()
                break
            }
            if (c < 128
                && (c < '0'.toInt() || c > '9'.toInt())
                && (c < 'a'.toInt() || c > 'z'.toInt())
                && (c < 'A'.toInt() || c > 'Z'.toInt())
                && c != '_'.toInt()
                && c != '-'.toInt()
                && c != '#'.toInt()
            ) {
                if (!relaxed) {
                    error("unterminated entity ref")
                }

                println("broken entitiy: " + get(pos - 1))

                //; ends with:"+(char)c);
                //                if (c != -1)
                //                    push(c);
                return
            }

            push(read())
        }

        val code = get(pos)
        txtPos = pos - 1
        if (token && type == ENTITY_REF) {
            name = code
        }

        if (code[0] == '#') {
            val c = if (code[1] == 'x')
                Integer.parseInt(code.substring(2), 16)
            else
                Integer.parseInt(code.substring(1))
            push(c)
            return
        }

        val result = entityMap!!.get(code) as String

        unresolved = result == null

        if (unresolved) {
            if (!token)
                error("unresolved: &$code;")
        } else {
            for (i in 0 until result.length)
                push(result[i].toInt())
        }
    }

    /** types:
     * '<': parse to any token (for nextToken ())
     * '"': parse to quote
     * ' ': parse to whitespace or '>'
     */

    @Throws(IOException::class, XmlPullParserException::class)
    private fun pushText(delimiter: Int, resolveEntities: Boolean) {

        var next = peek(0)
        var cbrCount = 0

        while (next != -1 && next != delimiter) { // covers eof, '<', '"'

            if (delimiter == ' '.toInt())
                if (next <= ' '.toInt() || next == '>'.toInt())
                    break

            if (next == '&'.toInt()) {
                if (!resolveEntities)
                    break

                pushEntity()
            } else if (next == '\n'.toInt() && type == START_TAG) {
                read()
                push(' ')
            } else
                push(read())

            if (next == '>'.toInt() && cbrCount >= 2 && delimiter != ']'.toInt())
                error("Illegal: ]]>")

            if (next == ']'.toInt())
                cbrCount++
            else
                cbrCount = 0

            next = peek(0)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun read(c: Char) {
        val a = read()
        if (a != c.toInt())
            error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    @Throws(IOException::class)
    private fun read(): Int {
        val result: Int

        if (peekCount == 0)
            result = peek(0)
        else {
            result = peek[0]
            peek[0] = peek[1]
        }
        //		else {
        //			result = peek[0];
        //			System.arraycopy (peek, 1, peek, 0, peekCount-1);
        //		}
        peekCount--

        column++

        if (result == '\n'.toInt()) {

            line++
            column = 1
        }

        return result
    }

    /** Does never read more than needed  */

    @Throws(IOException::class)
    private fun peek(pos: Int): Int {

        while (pos >= peekCount) {

            val nw: Int

            if (srcBuf.size <= 1)
                nw = reader!!.read()
            else if (srcPos < srcCount)
                nw = srcBuf[srcPos++].toInt()
            else {
                srcCount = reader!!.read(srcBuf, 0, srcBuf.size)
                if (srcCount <= 0)
                    nw = -1
                else
                    nw = srcBuf[0].toInt()

                srcPos = 1
            }

            if (nw == '\r'.toInt()) {
                wasCR = true
                peek[peekCount++] = '\n'.toInt()
            } else {
                if (nw == '\n'.toInt()) {
                    if (!wasCR)
                        peek[peekCount++] = '\n'.toInt()
                } else
                    peek[peekCount++] = nw

                wasCR = false
            }
        }

        return peek[pos]
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(): String {

        val pos = txtPos
        var c = peek(0)
        if ((c < 'a'.toInt() || c > 'z'.toInt())
            && (c < 'A'.toInt() || c > 'Z'.toInt())
            && c != '_'.toInt()
            && c != ':'.toInt()
            && c < 0x0c0
            && !relaxed
        )
            error("name expected")

        do {
            push(read())
            c = peek(0)
        } while (c >= 'a'.toInt() && c <= 'z'.toInt()
            || c >= 'A'.toInt() && c <= 'Z'.toInt()
            || c >= '0'.toInt() && c <= '9'.toInt()
            || c == '_'.toInt()
            || c == '-'.toInt()
            || c == ':'.toInt()
            || c == '.'.toInt()
            || c >= 0x0b7
        )

        val result = get(pos)
        txtPos = pos
        return result
    }

    @Throws(IOException::class)
    private fun skip() {

        while (true) {
            val c = peek(0)
            if (c > ' '.toInt() || c == -1)
                break
            read()
        }
    }

    //  public part starts here...

    @Throws(XmlPullParserException::class)
    fun setInput(reader: Reader?) {
        this.reader = reader

        line = 1
        column = 0
        type = START_DOCUMENT
        name = null
        namespace = null
        degenerated = false
        attributeCount = -1
        inputEncoding = null
        version = null
        standalone = null

        if (reader == null)
            return

        srcPos = 0
        srcCount = 0
        peekCount = 0
        depth = 0

        entityMap = Hashtable()
        entityMap!!.put("amp", "&")
        entityMap!!.put("apos", "'")
        entityMap!!.put("gt", ">")
        entityMap!!.put("lt", "<")
        entityMap!!.put("quot", "\"")
    }

    @Throws(XmlPullParserException::class)
    fun setInput(`is`: InputStream?, _enc: String) {

        srcPos = 0
        srcCount = 0
        var enc: String? = _enc

        if (`is` == null)
            throw IllegalArgumentException()

        try {

            if (enc == null) {
                // read four bytes

                var chk = 0

                while (srcCount < 4) {
                    val i = `is`!!.read()
                    if (i == -1)
                        break
                    chk = chk shl 8 or i
                    srcBuf[srcCount++] = i.toChar()
                }

                if (srcCount == 4) {
                    when (chk) {
                        0x00000FEFF -> {
                            enc = "UTF-32BE"
                            srcCount = 0
                        }

                        -0x20000 -> {
                            enc = "UTF-32LE"
                            srcCount = 0
                        }

                        0x03c -> {
                            enc = "UTF-32BE"
                            srcBuf[0] = '<'
                            srcCount = 1
                        }

                        0x03c000000 -> {
                            enc = "UTF-32LE"
                            srcBuf[0] = '<'
                            srcCount = 1
                        }

                        0x0003c003f -> {
                            enc = "UTF-16BE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcCount = 2
                        }

                        0x03c003f00 -> {
                            enc = "UTF-16LE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcCount = 2
                        }

                        0x03c3f786d -> {
                            while (true) {
                                val i = `is`!!.read()
                                if (i == -1)
                                    break
                                srcBuf[srcCount++] = i.toChar()
                                if (i == '>'.toInt()) {
                                    val s = String(srcBuf, 0, srcCount)
                                    var i0 = s.indexOf("encoding")
                                    if (i0 != -1) {
                                        while (s[i0] != '"' && s[i0] != '\'')
                                            i0++
                                        val deli = s[i0++]
                                        val i1 = s.indexOf(deli.toInt(), i0)
                                        enc = s.substring(i0, i1)
                                    }
                                    break
                                }
                            }
                            if (chk and -0x10000 == -0x1010000) {
                                enc = "UTF-16BE"
                                srcBuf[0] = (srcBuf[2].toInt() shl 8 or srcBuf[3].toInt()).toChar()
                                srcCount = 1
                            } else if (chk and -0x10000 == -0x20000) {
                                enc = "UTF-16LE"
                                srcBuf[0] = (srcBuf[3].toInt() shl 8 or srcBuf[2].toInt()).toChar()
                                srcCount = 1
                            } else if (chk and -0x100 == -0x10444100) {
                                enc = "UTF-8"
                                srcBuf[0] = srcBuf[3]
                                srcCount = 1
                            }
                        }

                        else -> if (chk and -0x10000 == -0x1010000) {
                            enc = "UTF-16BE"
                            srcBuf[0] = (srcBuf[2].toInt() shl 8 or srcBuf[3].toInt()).toChar()
                            srcCount = 1
                        } else if (chk and -0x10000 == -0x20000) {
                            enc = "UTF-16LE"
                            srcBuf[0] = (srcBuf[3].toInt() shl 8 or srcBuf[2].toInt()).toChar()
                            srcCount = 1
                        } else if (chk and -0x100 == -0x10444100) {
                            enc = "UTF-8"
                            srcBuf[0] = srcBuf[3]
                            srcCount = 1
                        }
                    }
                }
            }

            if (enc == null)
                enc = "UTF-8"

            val sc = srcCount
            setInput(InputStreamReader(`is`!!, enc))
            inputEncoding = _enc
            srcCount = sc
        } catch (e: Exception) {
            throw XmlPullParserException(
                "Invalid stream or encoding: $e",
                this,
                e
            )
        }

    }

    override fun getFeature(feature: String): Boolean {
        return if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature))
            processNsp
        else if (isProp(feature, false, "relaxed"))
            relaxed
        else
            false
    }

    @Throws(XmlPullParserException::class)
    override fun defineEntityReplacementText(entity: String, value: String) {
        if (entityMap == null)
            throw RuntimeException("entity replacement text must be defined after setInput!")
        entityMap!!.put(entity, value)
    }

    override fun getProperty(property: String): Any? {
        if (isProp(property, true, "xmldecl-version"))
            return version
        if (isProp(property, true, "xmldecl-standalone"))
            return standalone
        return if (isProp(property, true, "location")) if (location != null) location else reader!!.toString() else null
    }

    override fun getNamespaceCount(depth: Int): Int {
        if (depth > this.depth)
            throw IndexOutOfBoundsException()
        return nspCounts[depth]
    }

    override fun getNamespacePrefix(pos: Int): String? {
        return nspStack[pos shl 1]
    }

    override fun getNamespaceUri(pos: Int): String? {
        return nspStack[(pos shl 1) + 1]
    }

    override fun getNamespace(prefix: String): String? {

        if ("xml" == prefix)
            return "http://www.w3.org/XML/1998/namespace"
        if ("xmlns" == prefix)
            return "http://www.w3.org/2000/xmlns/"

        var i = (getNamespaceCount(depth) shl 1) - 2
        while (i >= 0) {
            if (prefix == null) {
                if (nspStack[i] == null)
                    return nspStack[i + 1]
            } else if (prefix == nspStack[i])
                return nspStack[i + 1]
            i -= 2
        }
        return null
    }

    override fun getDepth(): Int {
        return depth
    }

    override fun getPositionDescription(): String {

        val buf = StringBuffer(if (type < TYPES.length) TYPES[type] else "unknown")
        buf.append(' ')

        if (type == START_TAG || type == END_TAG) {
            if (degenerated)
                buf.append("(empty) ")
            buf.append('<')
            if (type == END_TAG)
                buf.append('/')

            if (prefix != null)
                buf.append("{$namespace}$prefix:")
            buf.append(name)

            val cnt = attributeCount shl 2
            var i = 0
            while (i < cnt) {
                buf.append(' ')
                if (attributes[i + 1] != null)
                    buf.append(
                        "{" + attributes[i] + "}" + attributes[i + 1] + ":"
                    )
                buf.append(attributes[i + 2] + "='" + attributes[i + 3] + "'")
                i += 4
            }

            buf.append('>')
        } else if (type == IGNORABLE_WHITESPACE)
        else if (type != TEXT)
            buf.append(getText())
        else if (isWhitespace)
            buf.append("(whitespace)")
        else {
            var text = getText()
            if (text!!.length > 16)
                text = text.substring(0, 16) + "..."
            buf.append(text)
        }

        buf.append("@$line:$column")
        if (location != null) {
            buf.append(" in ")
            buf.append(location)
        } else if (reader != null) {
            buf.append(" in ")
            buf.append(reader!!.toString())
        }
        return buf.toString()
    }

    override fun getLineNumber(): Int {
        return line
    }

    override fun getColumnNumber(): Int {
        return column
    }

    @Throws(XmlPullParserException::class)
    override fun isWhitespace(): Boolean {
        if (type != TEXT && type != IGNORABLE_WHITESPACE && type != CDSECT)
            exception(ILLEGAL_TYPE)
        return isWhitespace
    }

    override fun getText(): String? {
        return if (type < TEXT || type == ENTITY_REF && unresolved)
            null
        else
            get(0)
    }

    override fun getTextCharacters(poslen: IntArray): CharArray? {
        if (type >= TEXT) {
            if (type == ENTITY_REF) {
                poslen[0] = 0
                poslen[1] = name!!.length
                return name!!.toCharArray()
            }
            poslen[0] = 0
            poslen[1] = txtPos
            return txtBuf
        }

        poslen[0] = -1
        poslen[1] = -1
        return null
    }

    override fun getNamespace(): String? {
        return namespace
    }

    override fun getName(): String? {
        return name
    }

    override fun getPrefix(): String? {
        return prefix
    }

    @Throws(XmlPullParserException::class)
    override fun isEmptyElementTag(): Boolean {
        if (type != START_TAG)
            exception(ILLEGAL_TYPE)
        return degenerated
    }

    override fun getAttributeCount(): Int {
        return attributeCount
    }

    fun getAttributeType(index: Int): String {
        return "CDATA"
    }

    fun isAttributeDefault(index: Int): Boolean {
        return false
    }

    override fun getAttributeNamespace(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[index shl 2]
    }

    override fun getAttributeName(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 2]
    }

    override fun getAttributePrefix(index: Int): String? {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 1]
    }

    override fun getAttributeValue(index: Int): String? {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 3]
    }

    override fun getAttributeValue(namespace: String?, name: String): String? {

        var i = (attributeCount shl 2) - 4
        while (i >= 0) {
            if (attributes[i + 2] == name && (namespace == null || attributes[i] == namespace))
                return attributes[i + 3]
            i -= 4
        }

        return null
    }

    @Throws(XmlPullParserException::class)
    override fun getEventType(): Int {
        return type
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun next(): Int {

        txtPos = 0
        isWhitespace = true
        var minType = 9999
        token = false

        do {
            nextImpl()
            if (type < minType)
                minType = type
            //	    if (curr <= TEXT) type = curr;
        } while (minType > ENTITY_REF // ignorable
            || minType >= TEXT && peekType() >= TEXT
        )

        type = minType
        if (type > TEXT)
            type = TEXT

        return type
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextToken(): Int {

        isWhitespace = true
        txtPos = 0

        token = true
        nextImpl()
        return type
    }

    //
    // utility methods to make XML parsing easier ...

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextTag(): Int {

        next()
        if (type == TEXT && isWhitespace)
            next()

        if (type != END_TAG && type != START_TAG)
            exception("unexpected type")

        return type
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun require(type: Int, namespace: String?, name: String?) {

        if (type != this.type
            || namespace != null && namespace != getNamespace()
            || name != null && name != getName()
        )
            exception(
                "expected: " + TYPES[type] + " {" + namespace + "}" + name
            )
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextText(): String {
        if (type != START_TAG)
            exception("precondition: START_TAG")

        next()

        val result: String?

        if (type == TEXT) {
            result = getText()
            next()
        } else
            result = ""

        if (type != END_TAG)
            exception("END_TAG expected")

        return result
    }

    @Throws(XmlPullParserException::class)
    override fun setFeature(feature: String, value: Boolean) {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature))
            processNsp = value
        else if (isProp(feature, false, "relaxed"))
            relaxed = value
        else
            exception("unsupported feature: $feature")
    }

    @Throws(XmlPullParserException::class)
    override fun setProperty(property: String, value: Any) {
        if (isProp(property, true, "location"))
            location = value
        else
            throw XmlPullParserException("unsupported property: $property")
    }

    /**
     * Skip sub tree that is currently porser positioned on.
     * <br></br>NOTE: parser must be on START_TAG and when funtion returns
     * parser will be positioned on corresponding END_TAG.
     */

    //	Implementation copied from Alek's mail...

    @Throws(XmlPullParserException::class, IOException::class)
    fun skipSubTree() {
        require(START_TAG, null, null)
        var level = 1
        while (level > 0) {
            val eventType = next()
            if (eventType == END_TAG) {
                --level
            } else if (eventType == START_TAG) {
                ++level
            }
        }
    }

    companion object {
        private val UNEXPECTED_EOF = "Unexpected EOF"
        private val ILLEGAL_TYPE = "Wrong event type"
        private val LEGACY = 999
        private val XML_DECL = 998
    }
}