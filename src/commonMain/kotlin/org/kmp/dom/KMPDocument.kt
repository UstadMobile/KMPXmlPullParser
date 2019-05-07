/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */


package org.kmp.dom

import org.kmp.io.KMPSerializerParser
import org.kmp.io.KMPXmlParser
import org.kmp.io.KMPPullParser


/** The document consists of some legacy events and a single root
 * element. This class basically adds some consistency checks to
 * KMPNode.  */

class Document : KMPNode() {

    private var rootIndex = -1
    /** returns "#document"  */

    private lateinit var encoding: String
    var standalone: Boolean? = null


    val name: String
        get() = "#document"

    /** returns the root element of this document.  */

    val rootElement: Element
        get() {
            if (rootIndex == -1)
                throw RuntimeException("Document has no root element!")

            return getChild(rootIndex) as Element
        }

    /** Adds a child at the given index position. Throws
     * an exception when a second root element is added  */

    override fun addChild(index: Int, type: Int, child: Any?) {
        if (type == ELEMENT) {
            //   if (rootIndex != -1)
            //     throw new RuntimeException("Only one document root element allowed");

            rootIndex = index
        } else if (rootIndex >= index)
            rootIndex++

        super.addChild(index, type, child)
    }

    /** reads the document and checks if the last event
     * is END_DOCUMENT. If not, an exception is thrown.
     * The end event is consumed. For parsing partial
     * XML structures, consider using KMPNode.parse ().  */

    override fun parse(parser: KMPXmlParser) {

        parser.require(KMPPullParser.START_DOCUMENT, null, null)
        parser.nextToken()

        encoding = parser.inputEncoding?: ""
        standalone = parser.getProperty("http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone") as Boolean?

        super.parse(parser)

        if (parser.getEventType() != KMPPullParser.END_DOCUMENT)
            throw RuntimeException("Document end expected!")

    }

    override fun removeChild(idx: Int) {
        if (idx == rootIndex)
            rootIndex = -1
        else if (idx < rootIndex)
            rootIndex--

        super.removeChild(idx)
    }


    /** Writes this node to the given XmlWriter. For node and document,
     * this method is identical to writeChildren, except that the
     * stream is flushed automatically.  */

    override fun write(writer: KMPSerializerParser) {

        writer.startDocument(encoding, standalone)
        writeChildren(writer)
        writer.endDocument()
    }


}