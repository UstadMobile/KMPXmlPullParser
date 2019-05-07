package org.kmp

import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream
import kotlinx.io.StringReader
import kotlinx.io.StringWriter
import kotlinx.serialization.toUtf8Bytes
import org.kmp.io.KMPSerializerParser
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Test case contributed by Andy Bailey

class TestXMLParser {

    @Test
    fun testParseBeyondBmpFromReader() {
        val xpp = KMPXmlParser()
        xpp.setInput(StringReader(XML_TO_PARSE))
        checkParseBeyondBmp(xpp)
    }

    @Test
    fun testParseBeyondBmpInputStream() {
        val xpp = KMPXmlParser()
        xpp.setInput(ByteArrayInputStream(XML_TO_PARSE.toUtf8Bytes()), "UTF-8")
        checkParseBeyondBmp(xpp)
    }

    @Test
    fun testSerializeBeyondBmpToOutputStream() {
        val serializer = KMPSerializerParser()
        val os = ByteArrayOutputStream()
        serializer.setOutput(os, "utf-8")
        checkSerializeBeyondBmp(serializer)

        assertEquals(EXPECTED_XML_SERIALIZATION, os.toString())
    }

    @Test
    fun testSerializeBeyondBmpToWriter() {
        val serializer = KMPSerializerParser()

        val writer = StringWriter()
        serializer.setOutput(writer)

        checkSerializeBeyondBmp(serializer)

        assertEquals(EXPECTED_XML_SERIALIZATION, writer.toString())
    }

    companion object {

        // Using hex code units to be sure that the system charset does not affect the behavior
        private const val EMOJI_CHAR = "\ud83d\ude48"

        private const val XML_TO_PARSE = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources attr='" + EMOJI_CHAR + "' attr_hex='&#x1f648;' attr_dec='&#128584;'>\n"
                + "  <![CDATA[This is CDATA, with " + EMOJI_CHAR + ".]]>\n"
                + "  <!-- This is a comment, with " + EMOJI_CHAR + ", to see how it goes -->\n"
                + "  <string>Emoji: " + EMOJI_CHAR + "&#x1f648;&#128584;</string>\n"
                + "</resources>\n")


        private fun checkParseBeyondBmp(xpp: KMPXmlParser) {
            while (xpp.getEventType() != KMPPullParser.END_DOCUMENT) {
                when (xpp.getEventType()) {
                    KMPPullParser.CDSECT -> assertTrue(xpp.getText()!!.contains(EMOJI_CHAR))
                    KMPPullParser.COMMENT -> assertTrue(xpp.getText()!!.contains(EMOJI_CHAR))
                    KMPPullParser.TEXT -> {
                        val text = xpp.getText()!!.replace("[\\n\\r\\t ]+".toRegex(), "")
                        if (text.isNotEmpty()) {
                            assertTrue(xpp.getText()!!.contains(EMOJI_CHAR))
                        }
                    }
                    KMPPullParser.ENTITY_REF -> assertEquals(EMOJI_CHAR, xpp.getText())
                    KMPPullParser.START_TAG -> for (i in 0 until xpp.getAttributeCount()) {
                        assertEquals(EMOJI_CHAR, xpp.getAttributeValue(i))
                    }
                }
                xpp.nextToken()
            }
        }

        private val EXPECTED_XML_SERIALIZATION = (""
                + "<!--Emoji: " + EMOJI_CHAR + "-->\n"
                + "<![CDATA[Emoji: " + EMOJI_CHAR + "]]>\n"
                + "<string attr=\"&#128584;\">Emoji: &#128584;</string>")


        private fun checkSerializeBeyondBmp(serializer: KMPSerializerParser) {
            val text = "Emoji: $EMOJI_CHAR"

            serializer.comment(text)
            serializer.text("\n")
            serializer.cdsect(text)
            serializer.text("\n")
            serializer.startTag(null, "string")
            serializer.attribute(null, "attr", EMOJI_CHAR)
            serializer.text(text)
            serializer.endTag(null, "string")
            serializer.endDocument()
        }
    }
}
