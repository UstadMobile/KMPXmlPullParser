package org.kmp

import kotlinx.io.StringReader
import org.kmp.JavaAsserts.Companion.javaAssertEquals
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import org.kmp.io.KMPPullParser.Companion.END_DOCUMENT
import org.kmp.io.KMPPullParser.Companion.END_TAG
import org.kmp.io.KMPPullParser.Companion.START_DOCUMENT
import org.kmp.io.KMPPullParser.Companion.START_TAG
import org.kmp.io.KMPPullParser.Companion.TEXT
import org.kmp.io.KMPPullParserException
import org.kmp.io.debugPrint
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

    val SAMPLE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"Story-44201\">\n" +
            "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "    <dc:language>en</dc:language>\n" +
            "    <dc:identifier id=\"Story-44201\">/stories/44201-pishi-and-me</dc:identifier>\n" +
            "    <meta property=\"identifier-type\" refines=\"#Story-44201\">URL</meta>\n" +
            "    <dc:title id=\"story_44201\">\t</dc:title>\n" +
            "    <meta property=\"title-type\" refines=\"#story_44201\">main</meta>\n" +
            "    <dc:creator id=\"creator1\">Timira Gupta</dc:creator>\n" +
            "    <meta property=\"role\" refines=\"#creator1\">aut</meta>\n" +
            "    <dc:date>2017-12-31T18:30:00Z</dc:date>\n" +
            "    <meta property=\"dcterms:modified\">2018-09-06T09:06:54Z</meta>\n" +
            "    <meta name=\"cover\" content=\"item_image_1\"/>\n" +
            "  </metadata>\n" +
            "  <manifest>\n" +
            "    <item id=\"item_image_1\" href=\"image_1.jpg\" media-type=\"image/jpeg\" properties=\"cover-image\"/>\n" +
            "    <item id=\"item_NotoSans-Regular_gdi\" href=\"fonts/NotoSans/Regular/NotoSans-Regular_gdi.woff\" media-type=\"application/font-woff\"/>\n" +
            "    <item id=\"item_NotoSans-Bold_gdi\" href=\"fonts/NotoSans/Bold/NotoSans-Bold_gdi.woff\" media-type=\"application/font-woff\"/>\n" +
            "    <item id=\"item_NotoSans-Italic_gdi\" href=\"fonts/NotoSans/Italic/NotoSans-Italic_gdi.woff\" media-type=\"application/font-woff\"/>\n" +
            "    <item id=\"item_NotoSans-BoldItalic_gdi\" href=\"fonts/NotoSans/BoldItalic/NotoSans-BoldItalic_gdi.woff\" media-type=\"application/font-woff\"/>\n" +
            "    <item id=\"item_image_2\" href=\"image_2.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_3\" href=\"image_3.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_4\" href=\"image_4.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_5\" href=\"image_5.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_6\" href=\"image_6.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_7\" href=\"image_7.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_8\" href=\"image_8.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_9\" href=\"image_9.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_10\" href=\"image_10.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_11\" href=\"image_11.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_12\" href=\"image_12.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_13\" href=\"image_13.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_14\" href=\"image_14.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_image_17\" href=\"image_17.jpg\" media-type=\"image/jpeg\"/>\n" +
            "    <item id=\"item_publisher_logo\" href=\"publisher_logo.png\" media-type=\"image/png\"/>\n" +
            "    <item id=\"item_level_band\" href=\"level_band.png\" media-type=\"image/png\"/>\n" +
            "    <item id=\"item_pb-storyweaver-logo-01\" href=\"pb-storyweaver-logo-01.png\" media-type=\"image/png\"/>\n" +
            "    <item id=\"item_ccby\" href=\"ccby.png\" media-type=\"image/png\"/>\n" +
            "    <item id=\"item_cc0\" href=\"cc0.png\" media-type=\"image/png\"/>\n" +
            "    <item id=\"item_publicdomain\" href=\"publicdomain.svg\" media-type=\"image/svg+xml\"/>\n" +
            "    <item id=\"item_1\" href=\"1.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_2\" href=\"2.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_3\" href=\"3.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_4\" href=\"4.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_5\" href=\"5.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_6\" href=\"6.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_7\" href=\"7.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_8\" href=\"8.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_9\" href=\"9.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_10\" href=\"10.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_11\" href=\"11.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_12\" href=\"12.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_13\" href=\"13.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_14\" href=\"14.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_15\" href=\"15.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_16\" href=\"16.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"item_17\" href=\"17.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" +
            "    <item id=\"nav\" href=\"nav.html\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n" +
            "  </manifest>\n" +
            "  <spine toc=\"ncx\">\n" +
            "    <itemref idref=\"item_1\"/>\n" +
            "    <itemref idref=\"item_2\"/>\n" +
            "    <itemref idref=\"item_3\"/>\n" +
            "    <itemref idref=\"item_4\"/>\n" +
            "    <itemref idref=\"item_5\"/>\n" +
            "    <itemref idref=\"item_6\"/>\n" +
            "    <itemref idref=\"item_7\"/>\n" +
            "    <itemref idref=\"item_8\"/>\n" +
            "    <itemref idref=\"item_9\"/>\n" +
            "    <itemref idref=\"item_10\"/>\n" +
            "    <itemref idref=\"item_11\"/>\n" +
            "    <itemref idref=\"item_12\"/>\n" +
            "    <itemref idref=\"item_13\"/>\n" +
            "    <itemref idref=\"item_14\"/>\n" +
            "    <itemref idref=\"item_15\"/>\n" +
            "    <itemref idref=\"item_16\"/>\n" +
            "    <itemref idref=\"item_17\"/>\n" +
            "  </spine>\n" +
            "</package>\n"

    @Test
    fun debugPrint() {
        val pp = KMPXmlParser()
        pp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        pp.setInput(StringReader(SAMPLE))
        debugPrint(pp)
        javaAssertEquals(0, pp.getDepth())
    }

}
