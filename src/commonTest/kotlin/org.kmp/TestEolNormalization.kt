package org.kmp

import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import kotlin.test.Test


/**
 * Test end-of-line normalization
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestEolNormalization : UtilTest() {

    @Test
    fun testNormalizeLine() {

        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        //-----------------------
        // ---- simple tests for end of line normalization

        val simpleR = "-\n-\r-\r\n-\n\r-"

        // element content EOL normalizaton

        val tagSimpleR = "<test>$simpleR</test>"

        val expectedSimpleN = "-\n-\n-\n-\n\n-"

        parseOneElement(pp, tagSimpleR, true)
        javaAssertEquals(KMPPullParser.TEXT, pp.next())
        javaAssertEquals(printable(expectedSimpleN), printable(pp.getText()))

        // attribute content normalization

        val attrSimpleR = "<test a=\"$simpleR\"/>"

        val normalizedSimpleN = "- - - -  -"

        parseOneElement(pp, attrSimpleR, true)
        var attrVal = pp.getAttributeValue("", "a")

        //TODO Xerces2
        javaAssertEquals(printable(normalizedSimpleN), printable(attrVal))

        //-----------------------
        // --- more complex example with more line engins together

        val firstR = "\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r" + ""

        // element content

        val tagR = "<m:test xmlns:m='Some-Namespace-URI'>" +
                firstR +
                "</m:test>\r\n"

        val expectedN = "\n \n \n\n \n\n \n\n \n\n \n\n \n\n\n\n"

        parseOneElement(pp, tagR, true)
        javaAssertEquals(KMPPullParser.TEXT, pp.next())
        javaAssertEquals(printable(expectedN), printable(pp.getText()))

        // attribute value

        val attrR = "<m:test xmlns:m='Some-Namespace-URI' fifi='$firstR'/>"

        val normalizedN = "                       "

        parseOneElement(pp, attrR, true)
        attrVal = pp.getAttributeValue("", "fifi")
        //System.err.println("attrNormalized.len="+normalizedN.length());
        //System.err.println("attrVal.len="+attrVal.length());

        //TODO Xerces2
        javaAssertEquals(printable(normalizedN), printable(attrVal))


        //-----------------------
        // --- even more complex

        val manyLineBreaks = "fifi\r&amp;\r&amp;\r\n foo &amp;\r bar \n\r\n&quot;$firstR"

        val manyTag = "<m:test xmlns:m='Some-Namespace-URI'>" +
                manyLineBreaks +
                "</m:test>\r\n"

        val manyExpected = "fifi\n&\n&\n foo &\n bar \n\n\"$expectedN"
        //"\r \r\n \n\r \n\n \r\n\r \r\r \r\n\n \n\r\r\n\r";

        parseOneElement(pp, manyTag, true)
        javaAssertEquals(KMPPullParser.TEXT, pp.next())
        javaAssertEquals(manyExpected, pp.getText())

        javaAssertEquals(pp.next(), KMPPullParser.END_TAG)
        javaAssertEquals("test", pp.getName())

        // having \r\n as last characters is the hardest case
        //javaAssertEquals(KMPPullParser.CONTENT, pp.next());
        //javaAssertEquals("\n", pp.readContent());
        javaAssertEquals(pp.next(), KMPPullParser.END_DOCUMENT)


        val manyAttr = "<m:test xmlns:m='Some-Namespace-URI' fifi='$manyLineBreaks'/>"

        val manyNormalized = "fifi & &  foo &  bar   \"$normalizedN"

        parseOneElement(pp, manyAttr, true)
        attrVal = pp.getAttributeValue("", "fifi")
        //TODO Xerces2
        javaAssertEquals(printable(manyNormalized), printable(attrVal))

    }


    fun parseOneElement(pp: KMPPullParser, buf: String, supportNamespaces: Boolean) {
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
