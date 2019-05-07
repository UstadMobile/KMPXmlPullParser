package org.kmp

import kotlinx.io.StringReader
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser
import kotlin.test.Test

/**
 * More complete test to verify paring.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class TestEvent : UtilTest() {


    @Test
    fun testEvent() {
        val xpp = KMPXmlParser()
        xpp.setFeature(KMPPullParser.FEATURE_PROCESS_NAMESPACES, true)
        xpp.setInput(StringReader(TEST_XML))

        checkParserStateNs(xpp, 0, KMPPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1)

        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.START_TAG, null, 0, "", "root", null, false, 0)
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.TEXT, null, 0, null, null, "\n", false, -1)

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.START_TAG, null, 0, "", "foo", null, false, 0)
        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.TEXT, null, 0, null, null, "bar", false, -1)
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.END_TAG, null, 0, "", "foo", null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.TEXT, null, 0, null, null, "\n", false, -1)

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.START_TAG, null, 0, "http://www.xmlpull.org/temp", "hugo", null, false, 0)
        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.TEXT, null, 0, null, null, " \n\n \n  ", false, -1)

        xpp.next()
        checkParserStateNs(xpp, 3, KMPPullParser.START_TAG, null, 0, "http://www.xmlpull.org/temp", "hugochild", null, false, 0)
        xpp.next()
        checkParserStateNs(
            xpp, 3, KMPPullParser.TEXT, null, 0, null, null,
            "This is in a new namespace", false, -1
        )
        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.END_TAG, null, 0, "http://www.xmlpull.org/temp", "hugochild", null, false, -1)

        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.END_TAG, null, 0, "http://www.xmlpull.org/temp", "hugo", null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.TEXT, null, 0, null, null, "\t\n", false, -1)

        xpp.next()
        checkParserStateNs(xpp, 2, KMPPullParser.START_TAG, null, 0, "", "bar", null, true, 1)
        checkAttribNs(xpp, 0, null, "", "testattr", "123abc")
        xpp.next()
        checkParserStateNs(xpp, 1, KMPPullParser.END_TAG, null, 0, "", "bar", null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_TAG, null, 0, "", "root", null, false, -1)
        xpp.next()
        checkParserStateNs(xpp, 0, KMPPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1)
    }

}
