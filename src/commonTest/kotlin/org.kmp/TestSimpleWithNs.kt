package org.kmp

import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.test.Test
import kotlin.test.fail

/**
 * Simple test for minimal XML parsing with namespaces
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestSimpleWithNs : UtilTest() {

    @Test
    fun testSimpleWithNs() {
        val xpp = KMPXmlParser()
        xpp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        javaAssertEquals(true, xpp.getFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES))

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


        // check the simplest possible XML document - just one root element
        for (i in 1..2) {
            xpp.setInput(StringReader(if (i == 1) "<foo/>" else "<foo></foo>"))
            val empty = i == 1
            checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)
            xpp.next()
            checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 0, "", "foo", null, empty, 0)
            xpp.next()
            checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "", "foo", null, false, -1)
            xpp.next()
            checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)
        }

        // one step further - it has content ...


        xpp.setInput(StringReader("<foo attrName='attrVal'>bar</foo>"))
        checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 0, "", "foo", null, false, 1)
        checkAttribNs(xpp, 0, null, "", "attrName", "attrVal")
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.TEXT, null, 0, null, null, "bar", false, -1)
        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "", "foo", null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)


        xpp.setInput(
            StringReader(
                "<foo xmlns='n' xmlns:ns1='n1' xmlns:ns2='n2'>" +
                        "<ns1:bar xmlns:ns1='x1' xmlns:ns3='n3' xmlns='n1'>" +
                        "<ns2:gugu a1='v1' ns2:a2='v2' xml:lang='en' ns1:a3=\"v3\"/>" +
                        "<baz xmlns:ns1='y1'></baz>" +
                        "</ns1:bar></foo>"
            )
        )

        checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)

        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 2, "n", "foo", null, false, 0)
        javaAssertEquals(0, xpp.getNamespaceCount(0))
        javaAssertEquals(2, xpp.getNamespaceCount(1))
        checkNamespace(xpp, 0, "ns1", "n1", true)
        checkNamespace(xpp, 1, "ns2", "n2", true)

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.START_TAG, "ns1", 4, "x1", "bar", null, false, 0)
        javaAssertEquals(0, xpp.getNamespaceCount(0))
        javaAssertEquals(2, xpp.getNamespaceCount(1))
        javaAssertEquals(4, xpp.getNamespaceCount(2))
        checkNamespace(xpp, 2, "ns1", "x1", true)
        checkNamespace(xpp, 3, "ns3", "n3", true)

        xpp.next()
        checkParserStateNs(xpp, 3, KMPPullParser.START_TAG, "ns2", 4, "n2", "gugu", null, true, 4)
        javaAssertEquals(4, xpp.getNamespaceCount(2))
        javaAssertEquals(4, xpp.getNamespaceCount(3))
        javaAssertEquals("x1", xpp.getNamespace("ns1"))
        javaAssertEquals("n2", xpp.getNamespace("ns2"))
        javaAssertEquals("n3", xpp.getNamespace("ns3"))
        checkAttribNs(xpp, 0, null, "", "a1", "v1")
        checkAttribNs(xpp, 1, "ns2", "n2", "a2", "v2")
        checkAttribNs(xpp, 2, "xml", "http://www.w3.org/XML/1998/namespace", "lang", "en")
        checkAttribNs(xpp, 3, "ns1", "x1", "a3", "v3")

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.END_TAG, "ns2", 4, "n2", "gugu", null, false, -1)

        xpp.next()
        checkParserStateNs(xpp, 3, KMPPullParser.START_TAG, null, 5, "n1", "baz", null, false, 0)
        javaAssertEquals(0, xpp.getNamespaceCount(0))
        javaAssertEquals(2, xpp.getNamespaceCount(1))
        javaAssertEquals(4, xpp.getNamespaceCount(2))
        javaAssertEquals(5, xpp.getNamespaceCount(3))
        checkNamespace(xpp, 4, "ns1", "y1", true)
        javaAssertEquals("y1", xpp.getNamespace("ns1"))
        javaAssertEquals("n2", xpp.getNamespace("ns2"))
        javaAssertEquals("n3", xpp.getNamespace("ns3"))

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.END_TAG, null, 4, "n1", "baz", null, false, -1)
        javaAssertEquals("x1", xpp.getNamespace("ns1"))
        javaAssertEquals("n2", xpp.getNamespace("ns2"))
        javaAssertEquals("n3", xpp.getNamespace("ns3"))

        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.END_TAG, "ns1", 2, "x1", "bar", null, false, -1)
        javaAssertEquals("n1", xpp.getNamespace("ns1"))
        javaAssertEquals("n2", xpp.getNamespace("ns2"))
        javaAssertEquals(null, xpp.getNamespace("ns3"))

        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "n", "foo", null, false, -1)

        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)
        javaAssertEquals(null, xpp.getNamespace("ns1"))
        javaAssertEquals(null, xpp.getNamespace("ns2"))
        javaAssertEquals(null, xpp.getNamespace("ns3"))

    }

}
