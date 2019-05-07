package org.kmp

import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// see LICENSE_TESTS.txt in distribution for copyright and license information


/**
 * Simple test ot verify pull parser factory
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestSimple : UtilTest() {

    @Test
    fun testSimple() {
        val xpp = KMPXmlParser()
        javaAssertEquals(false, xpp.getFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES))

        // this SHOULD always be OK
        javaAssertEquals("START_DOCUMENT", KMPPullParser.TYPES[KMPPullParser.START_DOCUMENT])
        javaAssertEquals("END_DOCUMENT", KMPPullParser.TYPES[KMPPullParser.END_DOCUMENT])
        javaAssertEquals("START_TAG", KMPPullParser.TYPES[KMPPullParser.START_TAG])
        javaAssertEquals("END_TAG", KMPPullParser.TYPES[KMPPullParser.END_TAG])
        javaAssertEquals("TEXT", KMPPullParser.TYPES[KMPPullParser.TEXT])
        javaAssertEquals("CDSECT", KMPPullParser.TYPES[KMPPullParser.CDSECT])
        javaAssertEquals("ENTITY_REF", KMPPullParser.TYPES[KMPPullParser.ENTITY_REF])
        javaAssertEquals("IGNORABLE_WHITESPACE", KMPPullParser.TYPES[KMPPullParser.IGNORABLE_WHITESPACE])
        javaAssertEquals("PROCESSING_INSTRUCTION", KMPPullParser.TYPES[KMPPullParser.PROCESSING_INSTRUCTION])
        javaAssertEquals("COMMENT", KMPPullParser.TYPES[KMPPullParser.COMMENT])
        javaAssertEquals("DOCDECL", KMPPullParser.TYPES[KMPPullParser.DOCDECL])

        // check setInput semantics
        javaAssertEquals(KMPPullParser.START_DOCUMENT, xpp.getEventType())
        try {
            xpp.next()
            fail("exception was expected of next() if no input was set on parser")
        } catch (ex: KMPPullParserException) {
        }

        xpp.setInput(null)
        javaAssertEquals(KMPPullParser.START_DOCUMENT, xpp.getEventType())
        try {
            xpp.next()
            fail("exception was expected of next() if no input was set on parser")
        } catch (ex: KMPPullParserException) {
        }

        javaAssertEquals(1, xpp.getLineNumber())
        javaAssertEquals(0, xpp.getColumnNumber())


        // check the simplest possible XML document - just one root element
        for (i in 1..2) {
            xpp.setInput(StringReader(if (i == 1) "<foo/>" else "<foo></foo>"))
            javaAssertEquals(1, xpp.getLineNumber())
            javaAssertEquals(0, xpp.getColumnNumber())
            val empty = i == 1
            checkParserState(xpp, 0, KMPPullParser.START_DOCUMENT, null, null, false, -1)
            xpp.next()
            checkParserState(xpp, 1, KMPPullParser.START_TAG, "foo", null, empty, 0)
            xpp.next()
            checkParserState(xpp, 0, KMPPullParser.END_TAG, "foo", null, false, -1)
            xpp.next()
            checkParserState(xpp, 0, KMPPullParser.END_DOCUMENT, null, null, false, -1)
        }

        // one step further - it has content ...


        xpp.setInput(StringReader("<foo attrName='attrVal'>bar<p:t>\r\n\t </p:t></foo>"))
        checkParserState(xpp, 0, KMPPullParser.START_DOCUMENT, null, null, false, -1)
        xpp.next()
        checkParserState(xpp, 1, KMPPullParser.START_TAG, "foo", null, false, 1)
        checkAttrib(xpp, 0, "attrName", "attrVal")
        xpp.next()
        checkParserState(xpp, 1, KMPPullParser.TEXT, null, "bar", false, -1)
        javaAssertEquals(false, xpp.isWhitespace())
        xpp.next()
        checkParserState(xpp, 2, KMPPullParser.START_TAG, "p:t", null, false, 0)
        xpp.next()
        checkParserState(xpp, 2, KMPPullParser.TEXT, null, "\n\t ", false, -1)
        assertTrue(xpp.isWhitespace())
        xpp.next()
        checkParserState(xpp, 1, KMPPullParser.END_TAG, "p:t", null, false, -1)
        xpp.next()
        checkParserState(xpp, 0, KMPPullParser.END_TAG, "foo", null, false, -1)
        xpp.next()
        checkParserState(xpp, 0, KMPPullParser.END_DOCUMENT, null, null, false, -1)


    }

}
