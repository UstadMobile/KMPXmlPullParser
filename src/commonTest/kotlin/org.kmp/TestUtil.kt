package org.kmp

import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.math.max
import kotlin.test.fail


/**
 * Some common utilities to help with XMLPULL tests.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
open class UtilTest {

    protected var TEST_XML = "<root>\n" +
            "<foo>bar</foo>\r\n" +
            "<hugo xmlns=\"http://www.xmlpull.org/temp\"> \n\r \n" +
            "  <hugochild>This is in a <!-- comment -->new namespace</hugochild>" +
            "</hugo>\t\n" +
            "<bar testattr='123abc' />" +
            "</root>\n" +
            "\n" +
            "<!-- an xml sample document without meaningful content -->\n"


    fun checkParserState(xpp: KMPXmlParser, depth: Int, type: Int, name: String?,
                         text: String?, isEmpty: Boolean, attribCount: Int) {

        javaAssertEquals("PROCESS_NAMESPACES", false, xpp.getFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES))
        javaAssertEquals("TYPES[getType()]", KMPPullParser.TYPES[type], KMPPullParser.TYPES[xpp.getEventType()])
        javaAssertEquals("getType()", type, xpp.getEventType())
        javaAssertEquals("getDepth()", depth, xpp.getDepth())
        javaAssertEquals("getPrefix()", null, xpp.getPrefix())
        javaAssertEquals("getNamespacesCount(getDepth())", 0, xpp.getNamespaceCount(depth))
        if (xpp.getEventType() == KMPPullParser.START_TAG || xpp.getEventType() == KMPPullParser.END_TAG) {
            javaAssertEquals("getNamespace()", "", xpp.getNamespace())
        } else {
            javaAssertEquals("getNamespace()", null, xpp.getNamespace())
        }
        javaAssertEquals("getName()", name, xpp.getName())

        if (xpp.getEventType() != KMPPullParser.START_TAG && xpp.getEventType() != KMPPullParser.END_TAG) {
            javaAssertEquals("getText()", printable(text), printable(xpp.getText()))

            val holderForStartAndLength = IntArray(2)
            val buf = xpp.getTextCharacters(holderForStartAndLength)
            if (buf != null) {
                val s = String(buf, holderForStartAndLength[0], holderForStartAndLength[1])
                javaAssertEquals("getText(holder)", printable(text), printable(s))
            } else {
                javaAssertEquals("getTextCharacters()", null, text)
            }
        }
        if (type == KMPPullParser.START_TAG) {
            javaAssertEquals("isEmptyElementTag()", isEmpty, xpp.isEmptyElementTag())
        } else {
            try {
                xpp.isEmptyElementTag()
                fail("isEmptyElementTag() must throw exception if parser not on START_TAG")
            } catch (ex: KMPPullParserException) {
            }

        }
        javaAssertEquals("getAttributeCount()", attribCount, xpp.getAttributeCount())
    }

    fun checkParserStateNs(xpp: KMPXmlParser, depth: Int, type: Int, prefix: String?,
        nsCount: Int?, namespace: String?, name: String?, text: String?,
        isEmpty: Boolean?, attribCount: Int?) {
        // this methid can be used with enabled and not enabled namespaces
        //javaAssertEquals("PROCESS_NAMESPACES", true, xpp.getFeature(xpp.FEATURE_PROCESS_NAMESPACES));
        javaAssertEquals("TYPES[getType()]", KMPPullParser.TYPES[type], KMPPullParser.TYPES[xpp.getEventType()])
        javaAssertEquals("getType()", type, xpp.getEventType())
        javaAssertEquals("getName()", name, xpp.getName())

        javaAssertEquals("getDepth()", depth, xpp.getDepth())
        javaAssertEquals("getPrefix()", prefix, xpp.getPrefix())
        javaAssertEquals("getNamespacesCount(getDepth())", nsCount, xpp.getNamespaceCount(depth))
        javaAssertEquals("getNamespace()", namespace, xpp.getNamespace())

        if (xpp.getEventType() != KMPPullParser.START_TAG && xpp.getEventType() != KMPPullParser.END_TAG) {
            javaAssertEquals("getText()", printable(text), printable(xpp.getText()))

            val holderForStartAndLength = IntArray(2)
            val buf = xpp.getTextCharacters(holderForStartAndLength)
            if (buf != null) {
                val s = String(buf, holderForStartAndLength[0], holderForStartAndLength[1])
                javaAssertEquals("getText(holder)", printable(text), printable(s))
            } else {
                javaAssertEquals("getTextCharacters()", null, text)
            }

        }

        if (type == KMPPullParser.START_TAG) {
            javaAssertEquals("isEmptyElementTag()", isEmpty, xpp.isEmptyElementTag())
        } else {
            try {
                xpp.isEmptyElementTag()
                fail("isEmptyElementTag() must throw exception if parser not on START_TAG")
            } catch (ex: KMPPullParserException) {
            }

        }
        javaAssertEquals("getAttributeCount()", attribCount, xpp.getAttributeCount())
    }

    fun checkAttrib(xpp: KMPXmlParser, pos: Int, name: String, value: String) {
        javaAssertEquals("must be on START_TAG", KMPPullParser.START_TAG, xpp.getEventType())
        javaAssertEquals("getAttributePrefix()", null, xpp.getAttributePrefix(pos))
        javaAssertEquals("getAttributeNamespace()", "", xpp.getAttributeNamespace(pos))
        javaAssertEquals("getAttributeName()", name, xpp.getAttributeName(pos))
        javaAssertEquals("getAttributeValue()", value, xpp.getAttributeValue(pos))
        javaAssertEquals("getAttributeValue(name)", value, xpp.getAttributeValue(null, name))
    }

    fun checkAttribNs(xpp: KMPXmlParser, pos: Int, prefix: String?, namespace: String, name: String, value: String) {
        javaAssertEquals("must be on START_TAG", KMPPullParser.START_TAG, xpp.getEventType())
        javaAssertEquals("getAttributePrefix()", prefix, xpp.getAttributePrefix(pos))
        javaAssertEquals("getAttributeNamespace()", namespace, xpp.getAttributeNamespace(pos))
        javaAssertEquals("getAttributeName()", name, xpp.getAttributeName(pos))
        javaAssertEquals("getAttributeValue()", printable(value), printable(xpp.getAttributeValue(pos)))
        javaAssertEquals(
            "getAttributeValue(ns,name)",
            printable(value), printable(xpp.getAttributeValue(namespace, name))
        )
    }

    fun checkNamespace(xpp: KMPXmlParser, pos: Int, prefix: String, uri: String, checkMapping: Boolean) {
        javaAssertEquals("getNamespacePrefix()", prefix, xpp.getNamespacePrefix(pos))
        javaAssertEquals("getNamespaceUri()", uri, xpp.getNamespaceUri(pos))
        if (checkMapping) {
            javaAssertEquals("getNamespace(prefix)", uri, xpp.getNamespace(prefix))
        }
    }

    private fun printable(ch: Char): String {
        if (ch == '\n') {
            return "\\n"
        } else if (ch == '\r') {
            return "\\r"
        } else if (ch == '\t') {
            return "\\t"
        }

        return if (ch.toInt() > 127 || ch.toInt() < 32) {
            "\\u" + convertToHexString(ch.toInt())
        } else "" + ch
    }

    private fun convertToHexString(`val`: Int, shift: Int = 4): String {
        // assert shift > 0 && shift <=5 : "Illegal shift value";
        val mag = 32 - numberOfLeadingZeros(`val`)
        val chars = max((mag + (shift - 1)) / shift, 1)
        val buf = CharArray(chars)

        formatUnsignedInt(`val`, shift, buf, 0, chars)

        return String(buf)
    }

    private fun numberOfLeadingZeros(i: Int): Int {
        var i = i
        // HD, Figure 5-6
        if (i == 0)
            return 32
        var n = 1
        if (i.ushr(16) == 0) {
            n += 16
            i = i shl 16
        }
        if (i.ushr(24) == 0) {
            n += 8
            i = i shl 8
        }
        if (i.ushr(28) == 0) {
            n += 4
            i = i shl 4
        }
        if (i.ushr(30) == 0) {
            n += 2
            i = i shl 2
        }
        n -= i.ushr(31)
        return n
    }

    private val digits = charArrayOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'a',
        'b',
        'c',
        'd',
        'e',
        'f',
        'g',
        'h',
        'i',
        'j',
        'k',
        'l',
        'm',
        'n',
        'o',
        'p',
        'q',
        'r',
        's',
        't',
        'u',
        'v',
        'w',
        'x',
        'y',
        'z'
    )

    /**
     * Format a long (treated as unsigned) into a character buffer.
     * @param val the unsigned int to format
     * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
     * @param buf the character buffer to write to
     * @param offset the offset in the destination buffer to start at
     * @param len the number of characters to write
     * @return the lowest character  location used
     */
    private fun formatUnsignedInt(`val`: Int, shift: Int, buf: CharArray, offset: Int, len: Int): Int {
        var `val` = `val`
        var charPos = len
        val radix = 1 shl shift
        val mask = radix - 1
        do {
            buf[offset + --charPos] = digits[`val` and mask]
            `val` = `val` ushr shift
        } while (`val` != 0 && charPos > 0)

        return charPos
    }


    protected fun printable(s: String?): String? {
        var s: String? = s ?: return null
        val buf = StringBuilder()
        for (i in 0 until s!!.length) {
            buf.append(printable(s[i]))
        }
        s = buf.toString()
        return s
    }

}
