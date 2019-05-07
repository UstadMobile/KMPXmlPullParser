package org.kmp


/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// see LICENSE_TESTS.txt in distribution for copyright and license information


//import junit.framework.Test;
import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParserException
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test attribute uniqueness is ensured.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestAttributes {

    @Test
    fun testAttribs() {
        val XML_ATTRS = "<event xmlns:xsi='http://www.w3.org/1999/XMLSchema/instance' encodingStyle=\"test\">" +
                "<type>my-event</type>" +
                "<handback xsi:type='ns2:string' xmlns:ns2='http://www.w3.org/1999/XMLSchema' xsi:null='1'/>" +
                "</event>"

        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        pp.setInput(StringReader(XML_ATTRS))

        javaAssertEquals(KMPPullParser.START_TAG, pp.next())
        javaAssertEquals("event", pp.getName())
        javaAssertEquals(KMPPullParser.START_TAG, pp.next())
        javaAssertEquals("type", pp.getName())
        javaAssertEquals(KMPPullParser.TEXT, pp.next())
        javaAssertEquals("my-event", pp.getText())
        javaAssertEquals(pp.next(), KMPPullParser.END_TAG)
        javaAssertEquals("type", pp.getName())
        javaAssertEquals(KMPPullParser.START_TAG, pp.next())
        javaAssertEquals("handback", pp.getName())
        //javaAssertEquals(KMPPullParser.CONTENT, pp.next());
        //javaAssertEquals("", pp.readContent());

        val xsiNull = pp.getAttributeValue(
            "http://www.w3.org/1999/XMLSchema/instance", "null"
        )
        javaAssertEquals("1", xsiNull)

        val xsiType = pp.getAttributeValue(
            "http://www.w3.org/1999/XMLSchema/instance", "type"
        )
        javaAssertEquals("ns2:string", xsiType)


        val typeName = getQNameLocal(xsiType)
        javaAssertEquals("string", typeName)
        val typeNS = getQNameUri(pp, xsiType)
        javaAssertEquals("http://www.w3.org/1999/XMLSchema", typeNS)

        javaAssertEquals(pp.next(), KMPPullParser.END_TAG)
        javaAssertEquals("handback", pp.getName())
        javaAssertEquals(pp.next(), KMPPullParser.END_TAG)
        javaAssertEquals("event", pp.getName())

        javaAssertEquals(pp.next(), KMPPullParser.END_DOCUMENT)

    }

    private fun getQNameLocal(qname: String?): String? {
        if (qname == null) return null
        val pos = qname.indexOf(':')
        return qname.substring(pos + 1)
    }


    private fun getQNameUri(pp: KMPXmlParser, qname: String?): String? {
        if (qname == null) return null
        val pos = qname.indexOf(':')
        if (pos == -1)
            throw KMPPullParserException(
                "qname des not have prefix"
            )
        val prefix = qname.substring(0, pos)
        return pp.getNamespace(prefix)
    }

    @Test
    fun testAttribUniq() {

        val attribsOk = "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'" +
                " a='a' b='b' m:a='c' n:b='d' n:x='e'" +
                "/>\n" +
                ""

        val duplicateAttribs = "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'" +
                " a='a' b='b' m:a='a' n:b='b' a='x'" +
                "/>\n" +
                ""

        val duplicateNsAttribs = "<m:test xmlns:m='Some-Namespace-URI' xmlns:n='Some-Namespace-URI'" +
                " a='a' b='b' m:a='a' n:b='b' n:a='a'" +
                "/>\n" +
                ""

        val duplicateXmlns = "<m:test xmlns:m='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'" +
                "" +
                "/>\n" +
                ""

        val duplicateAttribXmlnsDefault = "<m:test xmlns='Some-Namespace-URI' xmlns:m='Some-Namespace-URI'" +
                " a='a' b='b' m:b='b' m:a='x'" +
                "/>\n" +
                ""

        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parseOneElement(pp, attribsOk, false)
        javaAssertEquals("a", pp.getAttributeValue(null, "a"))
        javaAssertEquals("b", pp.getAttributeValue(null, "b"))
        javaAssertEquals("c", pp.getAttributeValue(null, "m:a"))
        javaAssertEquals("d", pp.getAttributeValue(null, "n:b"))
        javaAssertEquals("e", pp.getAttributeValue(null, "n:x"))

        parseOneElement(pp, attribsOk, true)

        javaAssertEquals("a", pp.getAttributeValue("", "a"))
        javaAssertEquals("b", pp.getAttributeValue("", "b"))
        javaAssertEquals(null, pp.getAttributeValue("", "m:a"))
        javaAssertEquals(null, pp.getAttributeValue("", "n:b"))
        javaAssertEquals(null, pp.getAttributeValue("", "n:x"))

        javaAssertEquals("c", pp.getAttributeValue("Some-Namespace-URI", "a"))
        javaAssertEquals("d", pp.getAttributeValue("Some-Namespace-URI", "b"))
        javaAssertEquals("e", pp.getAttributeValue("Some-Namespace-URI", "x"))

        parseOneElement(pp, duplicateNsAttribs, false)
        parseOneElement(pp, duplicateAttribXmlnsDefault, false)
        parseOneElement(pp, duplicateAttribXmlnsDefault, true)

        var ex: Exception?

        ex = null
        try {
            parseOneElement(pp, duplicateAttribs, true)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

        ex = null
        try {
            parseOneElement(pp, duplicateAttribs, false)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

        ex = null
        try {
            parseOneElement(pp, duplicateXmlns, false)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

        ex = null
        try {
            parseOneElement(pp, duplicateXmlns, true)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

        ex = null
        try {
            parseOneElement(pp, duplicateNsAttribs, true)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

        val declaringEmptyNs = "<m:test xmlns:m='' />"

        // allowed when namespaces disabled
        parseOneElement(pp, declaringEmptyNs, false)

        // otherwise it is error to declare '' for non-default NS as described in
        //   http://www.w3.org/TR/1999/REC-xml-names-19990114/#ns-decl
        ex = null
        try {
            parseOneElement(pp, declaringEmptyNs, true)
        } catch (rex: KMPPullParserException) {
            ex = rex
        }

        assertNotNull(ex)

    }

    private fun parseOneElement(pp: KMPXmlParser, buf: String, supportNamespaces: Boolean) {
        //pp.setInput(buf.toCharArray());
        pp.setInput(StringReader(buf))
        //pp.setNamespaceAware(supportNamespaces);
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, supportNamespaces)
        //pp.setAllowedMixedContent(false);
        pp.next()
        //pp.readStartTag(stag);
        if (supportNamespaces) {
            javaAssertEquals("test", pp.getName())
        } else {
            javaAssertEquals("m:test", pp.getName())
        }
    }

}
