package org.kmp

import kotlinx.io.StringReader
import kotlinx.io.StringWriter
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Simple test for minimal XML tokenizing
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestSimpleToken : UtilTest() {

    @Test
    fun testSimpleToken() {
        val xpp = KMPXmlParser()
        xpp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        javaAssertEquals(true, xpp.getFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES))

        // check setInput semantics
        javaAssertEquals(KMPPullParser.START_DOCUMENT, xpp.getEventType())
        try {
            xpp.nextToken()
            fail("exception was expected of nextToken() if no input was set on parser")
        } catch (ex: KMPPullParserException) {
        }

        xpp.setInput(null)
        javaAssertEquals(KMPPullParser.START_DOCUMENT, xpp.getEventType())
        try {
            xpp.nextToken()
            fail("exception was expected of next() if no input was set on parser")
        } catch (ex: KMPPullParserException) {
        }

        xpp.setInput(null) //reset parser
        val FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip"
        // attempt to set roundtrip
        try {
            xpp.setFeature(FEATURE_XML_ROUNDTRIP, true)
        } catch (ex: Exception) {
        }

        // did we succeeded?
        val roundtripSupported = xpp.getFeature(FEATURE_XML_ROUNDTRIP)


        // check the simplest possible XML document - just one root element
        for (i in 1..2) {
            xpp.setInput(StringReader(if (i == 1) "<foo/>" else "<foo></foo>"))
            val empty = i == 1
            checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)
            xpp.nextToken()
            checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 0, "", "foo", null, empty, 0)
            if (roundtripSupported) {
                if (empty) {
                    //              System.out.println("tag='"+xpp.getText()+"'");
                    //              String foo ="<foo/>";
                    //              String foo2 = xpp.getText();
                    //              System.out.println(foo.equals(foo2));
                    javaAssertEquals(
                        "empty tag roundtrip",
                        printable("<foo/>"),
                        printable(xpp.getText())
                    )
                } else {
                    javaAssertEquals(
                        "start tag roundtrip",
                        printable("<foo>"),
                        printable(xpp.getText())
                    )
                }
            }
            xpp.nextToken()
            checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "", "foo", null, false, -1)
            if (roundtripSupported) {
                if (empty) {
                    javaAssertEquals(
                        "empty tag roundtrip",
                        printable("<foo/>"),
                        printable(xpp.getText())
                    )
                } else {
                    javaAssertEquals(
                        "end tag roundtrip",
                        printable("</foo>"),
                        printable(xpp.getText())
                    )
                }
            }
            xpp.nextToken()
            checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)
        }

        // one step further - it has content ...

        val MISC_XML = "\n \r\n \n\r<!DOCTYPE titlepage SYSTEM \"http://www.foo.bar/dtds/typo.dtd\"" +
                "[<!ENTITY % active.links \"INCLUDE\">" +
                "  <!ENTITY   test \"This is test! Do NOT Panic!\" >" +
                "]>" +
                "<!--c-->  \r\n<foo attrName='attrVal'>bar<!--comment-->" +
                "&test;&lt;&#32;" +
                "<?pi ds?><![CDATA[ vo<o ]]></foo> \r\n"
        xpp.setInput(StringReader(MISC_XML))
        checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(
            xpp, 0, KMPPullParser.IGNORABLE_WHITESPACE, null, 0, null, null,
            "\n \r\n \n\r", false, -1
        )
        assertTrue(xpp.isWhitespace())

        xpp.nextToken()
        checkParserStateNs(
            xpp, 0, KMPPullParser.DOCDECL, null, 0, null, null,
            " titlepage SYSTEM \"http://www.foo.bar/dtds/typo.dtd\"" +
                    "[<!ENTITY % active.links \"INCLUDE\">" +
                    "  <!ENTITY   test \"This is test! Do NOT Panic!\" >]", false, -1
        )
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 0, KMPPullParser.COMMENT, null, 0, null, null, "c", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 0, KMPPullParser.IGNORABLE_WHITESPACE, null, 0, null, null, "  \r\n", false, -1)
        assertTrue(xpp.isWhitespace())

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 0, "", "foo", null, false, 1)
        if (roundtripSupported) {
            javaAssertEquals("start tag roundtrip", "<foo attrName='attrVal'>", xpp.getText())
        }
        checkAttribNs(xpp, 0, null, "", "attrName", "attrVal")
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.TEXT, null, 0, null, null, "bar", false, -1)
        javaAssertEquals(false, xpp.isWhitespace())

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.COMMENT, null, 0, null, null, "comment", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.ENTITY_REF, null, 0, null, null, "test", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.ENTITY_REF, null, 0, null, null, "lt", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.ENTITY_REF, null, 0, null, null, "#32", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.PROCESSING_INSTRUCTION, null, 0, null, null, "pi ds", false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(xpp, 1, KMPPullParser.CDSECT, null, 0, null, null, " vo<o ", false, -1)
        javaAssertEquals(false, xpp.isWhitespace())

        xpp.nextToken()
        checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "", "foo", null, false, -1)
        if (roundtripSupported) {
            javaAssertEquals("end tag roundtrip", "</foo>", xpp.getText())
        }
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        xpp.nextToken()
        checkParserStateNs(
            xpp, 0, KMPPullParser.IGNORABLE_WHITESPACE, null, 0, null, null,
            " \r\n", false, -1
        )
        assertTrue(xpp.isWhitespace())

        xpp.nextToken()
        checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)
        try {
            xpp.isWhitespace()
            fail("whitespace function must fail for START_DOCUMENT")
        } catch (ex: KMPPullParserException) {
        }

        // reset parser
        xpp.setInput(null)

        if (roundtripSupported) {
            val sw = StringWriter()
            var s: String
            //StringWriter st = new StringWriter();
            xpp.setInput(StringReader(MISC_XML))
            val holderForStartAndLength = IntArray(2)
            var buf: CharArray?
            while (xpp.nextToken() != KMPPullParser.END_DOCUMENT) {
                when (xpp.getEventType()) {
                    //case xpp.START_DOCUMENT:
                    //case xpp.END_DOCUMENT:
                    //  break LOOP;
                    KMPPullParser.START_TAG -> {
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip START_TAG", xpp.getText(), s)
                        sw.write(s)
                    }
                    KMPPullParser.END_TAG -> {
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip END_TAG", xpp.getText(), s)
                        sw.write(s)
                    }
                    KMPPullParser.TEXT -> {
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip TEXT", xpp.getText(), s)
                        sw.write(s)
                    }
                    KMPPullParser.IGNORABLE_WHITESPACE -> {
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip IGNORABLE_WHITESPACE", xpp.getText(), s)
                        sw.write(s)
                    }
                    KMPPullParser.CDSECT -> {
                        sw.write("<![CDATA[")
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip CDSECT", xpp.getText(), s)
                        sw.write(s)
                        sw.write("]]>")
                    }
                    KMPPullParser.PROCESSING_INSTRUCTION -> {
                        sw.write("<?")
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip PROCESSING_INSTRUCTION", xpp.getText(), s)
                        sw.write(s)
                        sw.write("?>")
                    }
                    KMPPullParser.COMMENT -> {
                        sw.write("<!--")
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip COMMENT", xpp.getText(), s)
                        sw.write(s)
                        sw.write("-->")
                    }
                    KMPPullParser.ENTITY_REF -> {
                        sw.write("&")
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip ENTITY_REF", xpp.getText(), s)
                        sw.write(s)
                        sw.write(";")
                    }
                    KMPPullParser.DOCDECL -> {
                        sw.write("<!DOCTYPE")
                        buf = xpp.getTextCharacters(holderForStartAndLength)
                        s = String(buf!!, holderForStartAndLength[0], holderForStartAndLength[1])
                        javaAssertEquals("roundtrip DOCDECL", xpp.getText(), s)
                        sw.write(s)
                        sw.write(">")
                    }
                    else -> throw RuntimeException("unknown token type")
                }
            }
            sw.close()
            val RESULT_XML_BUF = sw.toString()
            javaAssertEquals("rountrip XML", printable(MISC_XML), printable(RESULT_XML_BUF))
        }
    }

}
