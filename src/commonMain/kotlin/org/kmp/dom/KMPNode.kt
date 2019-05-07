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

import java.util.*
import java.io.*

import org.kmp.io.XmlPullParserException
import org.kmp.io.XmlSerializer
import org.xmlpull.v1.*

/** A common base class for Document and Element, also used for
 * storing XML fragments.  */

class Node { //implements XmlIO{

    protected var children: Vector<*>? = null
    protected var types: StringBuffer

    /** Returns the number of child objects  */

    val childCount: Int
        get() = if (children == null) 0 else children!!.size

    /** inserts the given child object of the given type at the
     * given index.  */

    fun addChild(index: Int, type: Int, child: Any?) {

        if (child == null)
            throw NullPointerException()

        if (children == null) {
            children = Vector()
            types = StringBuffer()
        }

        if (type == ELEMENT) {
            if (child !is Element)
                throw RuntimeException("Element obj expected)")

            (child as Element).setParent(this)
        } else if (child !is String)
            throw RuntimeException("String expected")

        children!!.insertElementAt(child, index)
        types.insert(index, type.toChar())
    }

    /** convenience method for addChild (getChildCount (), child)  */

    fun addChild(type: Int, child: Any) {
        addChild(childCount, type, child)
    }

    /** Builds a default element with the given properties. Elements
     * should always be created using this method instead of the
     * constructor in order to enable construction of specialized
     * subclasses by deriving custom Document classes. Please note:
     * For no namespace, please use Xml.NO_NAMESPACE, null is not a
     * legal value. Currently, null is converted to Xml.NO_NAMESPACE,
     * but future versions may throw an exception.  */

    fun createElement(namespace: String?, name: String): Element {

        val e = Element()
        e.namespace = namespace ?: ""
        e.name = name
        return e
    }

    /** Returns the child object at the given index.  For child
     * elements, an Element object is returned. For all other child
     * types, a String is returned.  */

    fun getChild(index: Int): Any {
        return children!!.elementAt(index)
    }

    /** returns the element at the given index. If the node at the
     * given index is a text node, null is returned  */

    fun getElement(index: Int): Element? {
        val child = getChild(index)
        return if (child is Element) child as Element else null
    }

    /** Returns the element with the given namespace and name. If the
     * element is not found, or more than one matching elements are
     * found, an exception is thrown.  */

    fun getElement(namespace: String, name: String): Element? {

        val i = indexOf(namespace, name, 0)
        val j = indexOf(namespace, name, i + 1)

        if (i == -1 || j != -1)
            throw RuntimeException(
                "Element {"
                        + namespace
                        + "}"
                        + name
                        + (if (i == -1) " not found in " else " more than once in ")
                        + this
            )

        return getElement(i)
    }

    /* returns "#document-fragment". For elements, the element name is returned

     public String getName()
     {
         return "#document-fragment";
     }

     / Returns the namespace of the current element. For Node
     and Document, Xml.NO_NAMESPACE is returned.

       public String getNamespace() {
 return "";
 }

 public int getNamespaceCount () {
 return 0;
 }

  returns the text content if the element has text-only
 content. Throws an exception for mixed content

 public String getText() {

 StringBuffer buf = new StringBuffer();
 int len = getChildCount();

 for (int i = 0; i < len; i++) {
 if (isText(i))
 buf.append(getText(i));
 else if (getType(i) == ELEMENT)
 throw new RuntimeException("not text-only content!");
 }

 return buf.toString();
 }
 */

    /** Returns the text node with the given index or null if the node
     * with the given index is not a text node.  */

    fun getText(index: Int): String? {
        return if ((isText(index))) getChild(index) as String else null
    }

    /** Returns the type of the child at the given index. Possible
     * types are ELEMENT, TEXT, COMMENT, and PROCESSING_INSTRUCTION  */

    fun getType(index: Int): Int {
        return types.get(index).toInt()
    }

    /** Convenience method for indexOf (getNamespace (), name,
     * startIndex).
     *
     * public int indexOf(String name, int startIndex) {
     * return indexOf(getNamespace(), name, startIndex);
     * }
     */

    /** Performs search for an element with the given namespace and
     * name, starting at the given start index. A null namespace
     * matches any namespace, please use Xml.NO_NAMESPACE for no
     * namespace).  returns -1 if no matching element was found.  */

    fun indexOf(namespace: String?, name: String, startIndex: Int): Int {

        val len = childCount

        for (i in startIndex until len) {

            val child = getElement(i)

            if ((child != null
                        && name == child!!.getName()
                        && (namespace == null || namespace == child!!.getNamespace()))
            )
                return i
        }
        return -1
    }

    fun isText(i: Int): Boolean {
        val t = getType(i)
        return t == TEXT || t == IGNORABLE_WHITESPACE || t == CDSECT
    }

    /** Recursively builds the child elements from the given parser
     * until an end tag or end document is found.
     * The end tag is not consumed.  */

    @Throws(IOException::class, XmlPullParserException::class)
    fun parse(parser: XmlPullParser) {

        var leave = false

        do {
            val type = parser.eventType

//         System.out.println(parser.getPositionDescription());

            when (type) {

                XmlPullParser.START_TAG -> {
                    val child = createElement(
                        parser.namespace,
                        parser.name
                    )
//    child.setAttributes (event.getAttributes ());
                    addChild(ELEMENT, child)

// order is important here since
// setparent may perform some init code!

                    child.parse(parser)
                }

                XmlPullParser.END_DOCUMENT, XmlPullParser.END_TAG -> leave = true

                else -> {
                    if (parser.text != null)
                        addChild(
                            if (type == XmlPullParser.ENTITY_REF) TEXT else type,
                            parser.text
                        )
                    else if ((type == XmlPullParser.ENTITY_REF && parser.name != null)) {
                        addChild(ENTITY_REF, parser.name)
                    }
                    parser.nextToken()
                }
            }
        } while (!leave)
    }

    /** Removes the child object at the given index  */

    fun removeChild(idx: Int) {
        children!!.removeElementAt(idx)

        /***  Modification by HHS - start  */
//      types.deleteCharAt (index);
        /** */
        val n = types.length - 1

        for (i in idx until n)
            types.setCharAt(i, types.get(i + 1))

        types.setLength(n)

        /***  Modification by HHS - end    */
    }

/* returns a valid XML representation of this Element including
attributes and children.
public String toString() {
try {
ByteArrayOutputStream bos =
new ByteArrayOutputStream();
XmlWriter xw =
new XmlWriter(new OutputStreamWriter(bos));
write(xw);
xw.close();
return new String(bos.toByteArray());
}
catch (IOException e) {
throw new RuntimeException(e.toString());
}
}
*/

    /** Writes this node to the given XmlWriter. For node and document,
     * this method is identical to writeChildren, except that the
     * stream is flushed automatically.  */

    @Throws(IOException::class)
    fun write(writer: XmlSerializer) {
        writeChildren(writer)
        writer.flush()
    }

    /** Writes the children of this node to the given XmlWriter.  */

    @Throws(IOException::class)
    fun writeChildren(writer: XmlSerializer) {
        if (children == null)
            return

        val len = children!!.size

        for (i in 0 until len) {
            val type = getType(i)
            val child = children!!.elementAt(i)
            when (type) {
                ELEMENT -> (child as Element).write(writer)

                TEXT -> writer.text(child as String)

                IGNORABLE_WHITESPACE -> writer.ignorableWhitespace(child as String)

                CDSECT -> writer.cdsect(child as String)

                COMMENT -> writer.comment(child as String)

                ENTITY_REF -> writer.entityRef(child as String)

                PROCESSING_INSTRUCTION -> writer.processingInstruction(child as String)

                DOCDECL -> writer.docdecl(child as String)

                else -> throw RuntimeException("Illegal type: " + type)
            }
        }
    }

    companion object {

        val DOCUMENT = 0
        val ELEMENT = 2
        val TEXT = 4
        val CDSECT = 5
        val ENTITY_REF = 6
        val IGNORABLE_WHITESPACE = 7
        val PROCESSING_INSTRUCTION = 8
        val COMMENT = 9
        val DOCDECL = 10
    }
}