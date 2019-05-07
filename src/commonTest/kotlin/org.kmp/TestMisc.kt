package org.kmp

import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.test.Test

/**
 * Tests checking miscellaneous features.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestMisc : UtilTest() {

    @Test
    fun testReadText() {
        val INPUT_XML = "<test>foo</test>"
        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        pp.setInput(StringReader(INPUT_XML))
        javaAssertEquals("", pp.readText())
        pp.next()
        javaAssertEquals("", pp.readText())
        pp.next()
        javaAssertEquals("foo", pp.readText())
        javaAssertEquals(KMPPullParser.TYPES[KMPPullParser.END_TAG], KMPPullParser.TYPES[pp.getEventType()])
    }

    @Test
    fun testRequire() {
        //public void require (int type, String namespace, String name)
        val INPUT_XML = "<test><t>foo</t><m:s xmlns:m='URI'>\t</m:s></test>"
        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        pp.setInput(StringReader(INPUT_XML))
        pp.require(KMPPullParser.START_DOCUMENT, null, null)
        pp.next()
        pp.require(KMPPullParser.START_TAG, null, "test")
        pp.require(KMPPullParser.START_TAG, "", null)
        pp.require(KMPPullParser.START_TAG, "", "test")
        pp.next()
        pp.require(KMPPullParser.START_TAG, "", "t")
        pp.next()
        pp.require(KMPPullParser.TEXT, null, null)
        pp.next()
        pp.require(KMPPullParser.END_TAG, "", "t")

        pp.next()
        pp.require(KMPPullParser.START_TAG, "URI", "s")

        pp.next()
        // this call will skip white spaces
        pp.require(KMPPullParser.END_TAG, "URI", "s")

        pp.next()
        pp.require(KMPPullParser.END_TAG, "", "test")
        pp.next()
        pp.require(KMPPullParser.END_DOCUMENT, null, null)

    }

    @Test
    fun testReportNamespaceAttributes() {
        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        javaAssertEquals(true, pp.getFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES))

        try {
            pp.setFeature(KMPPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, true)
        } catch (ex: KMPPullParserException) {
            // skip rest of test if parser does nto support reporting
            return
        }

        // see XML Namespaces spec for namespace URIs for 'xml' and 'xmlns'
        //   xml is bound to http://www.w3.org/XML/1998/namespace
        //   "(...) The prefix xmlns is used only for namespace bindings
        //     and is not itself bound to any namespace name. (...)
        // however it is typically bound to "http://www.w3.org/2000/xmlns/"
        //   in some contexts such as DOM
        // http://www.w3.org/TR/REC-xml-names/#ns-using
        val XML_MISC_ATTR = "<test xmlns='Some-Namespace-URI' xmlns:n='Some-Other-URI'" +
                " a='a' b='b' xmlns:m='Another-URI' m:a='c' n:b='d' n:x='e' xml:lang='en'" +
                "/>\n" +
                ""
        pp.setInput(StringReader(XML_MISC_ATTR))
        pp.next()
        //pp.readStartTag(stag);
        javaAssertEquals("test", pp.getName())
        javaAssertEquals("Some-Namespace-URI", pp.getNamespace())

        javaAssertEquals("a", pp.getAttributeValue("", "a"))
        javaAssertEquals("b", pp.getAttributeValue("", "b"))
        javaAssertEquals(null, pp.getAttributeValue("", "m:a"))
        javaAssertEquals(null, pp.getAttributeValue("", "n:b"))
        javaAssertEquals(null, pp.getAttributeValue("", "n:x"))

        javaAssertEquals("c", pp.getAttributeValue("Another-URI", "a"))
        javaAssertEquals("d", pp.getAttributeValue("Some-Other-URI", "b"))
        javaAssertEquals("e", pp.getAttributeValue("Some-Other-URI", "x"))
        javaAssertEquals("en", pp.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang"))


        checkAttribNs(pp, 0, null, "", "xmlns", "Some-Namespace-URI")
        checkAttribNs(pp, 1, "xmlns", "http://www.w3.org/2000/xmlns/", "n", "Some-Other-URI")
        checkAttribNs(pp, 2, null, "", "a", "a")
        checkAttribNs(pp, 3, null, "", "b", "b")
        checkAttribNs(pp, 4, "xmlns", "http://www.w3.org/2000/xmlns/", "m", "Another-URI")
        checkAttribNs(pp, 5, "m", "Another-URI", "a", "c")
        checkAttribNs(pp, 6, "n", "Some-Other-URI", "b", "d")
        checkAttribNs(pp, 7, "n", "Some-Other-URI", "x", "e")
        checkAttribNs(pp, 8, "xml", "http://www.w3.org/XML/1998/namespace", "lang", "en")
    }

}
