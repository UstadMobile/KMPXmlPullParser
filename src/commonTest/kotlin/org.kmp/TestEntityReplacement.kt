package org.kmp

import kotlinx.io.StringReader
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import kotlin.test.Test

/**
 * Test if entity replacement works ok.
 * This test is designe to work bboth for validating and non validating parsers!
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestEntityReplacement : UtilTest() {

    @Test
    fun testEntityReplacement() {
        // taken from http://www.w3.org/TR/REC-xml#sec-entexpand
        val XML_ENTITY_EXPANSION = "<?xml version='1.0'?>\n" +
                "<!DOCTYPE test [\n" +
                "<!ELEMENT test (#PCDATA) >\n" +
                "<!ENTITY % xx '&#37;zz;'>\n" +
                "<!ENTITY % zz '&#60;!ENTITY tricky \"error-prone\" >' >\n" +
                "%xx;\n" +
                "]>" +
                "<test>This sample shows a &tricky; method.</test>"

        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        // default parser must work!!!!
        pp.setInput(StringReader(XML_ENTITY_EXPANSION))
        if (!pp.getFeature(KMPPullParser.FEATURE_PROCESS_DOCDECL)) {
            pp.defineEntityReplacementText("tricky", "error-prone")
        }
        testEntityReplacement(pp)

        // now we try for no FEATURE_PROCESS_DOCDECL
        pp.setInput(StringReader(XML_ENTITY_EXPANSION))
        try {
            pp.setFeature(KMPPullParser.FEATURE_PROCESS_DOCDECL, false)
        } catch (ex: Exception) {
        }

        if (!pp.getFeature(KMPPullParser.FEATURE_PROCESS_DOCDECL)) {
            pp.defineEntityReplacementText("tricky", "error-prone")
            testEntityReplacement(pp)
        }

        // try to use FEATURE_PROCESS_DOCDECL if supported
        pp.setInput(StringReader(XML_ENTITY_EXPANSION))
        try {
            pp.setFeature(KMPPullParser.FEATURE_PROCESS_DOCDECL, true)
        } catch (ex: Exception) {
        }

        if (pp.getFeature(KMPPullParser.FEATURE_PROCESS_DOCDECL)) {
            testEntityReplacement(pp)
        }

        // try to use FEATURE_PROCESS_DOCDECL if supported
        pp.setInput(StringReader(XML_ENTITY_EXPANSION))
        try {
            pp.setFeature(KMPPullParser.FEATURE_VALIDATION, true)
        } catch (ex: Exception) {
        }

        if (pp.getFeature(KMPPullParser.FEATURE_VALIDATION)) {
            testEntityReplacement(pp)
        }

    }

    fun testEntityReplacement(pp: KMPXmlParser) {
        pp.next()
        checkParserStateNs(pp, 1, KMPPullParser.START_TAG, null, 0, "", "test", null, false, 0)
        pp.next()
        checkParserStateNs(
            pp, 1, KMPPullParser.TEXT, null, 0, null, null,
            "This sample shows a error-prone method.", false, -1
        )
        pp.next()
        checkParserStateNs(pp, 0, KMPPullParser.END_TAG, null, 0, "", "test", null, false, -1)
        pp.nextToken()
        checkParserStateNs(pp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)

    }


}
