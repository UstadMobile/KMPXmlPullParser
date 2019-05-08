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

import org.kmp.io.KMPPullParser
import org.kmp.io.KMPSerializerParser
import org.kmp.io.KMPXmlParser

/**
 * In order to create an element, please use the createElement method
 * instead of invoking the constructor directly. The right place to
 * add user defined initialization code is the init method.  */

class KMPElement : KMPNode() {

    internal lateinit var namespace: String
    /**
     * returns the (local) name of the element  */

    /**
     * sets the name of the element  */

    lateinit var name: String
    protected var attributes: MutableList<Any>? = null
    /**
     * Returns the parent node of this element  */

    /**
     * Sets the Parent of this element. Automatically called from the
     * add method.  Please use with care, you can simply
     * create inconsitencies in the document tree structure using
     * this method!   */
    var parent: KMPNode? = null
        internal set
    protected var prefixes: MutableList<Any>? = null

    /**
     * Returns the number of attributes of this element.  */

    val attributeCount: Int
        get() = if (attributes == null) 0 else attributes!!.size

    /**
     * Returns the root node, determined by ascending to the
     * all parents un of the root element.  */

    val root: KMPNode
        get() {

            var current: KMPElement = this

            while (current.parent != null) {
                if (current.parent !is KMPElement) return current.parent!!
                current = current.parent as KMPElement
            }

            return current
        }


    /**
     * returns the number of declared namespaces, NOT including
     * parent elements  */

    val namespaceCount: Int
        get() = if (prefixes == null) 0 else prefixes!!.size

    constructor() {}

    /**
     * called when all properties are set, but before children
     * are parsed. Please do not use setParent for initialization
     * code any longer.  */

    fun init() {}


    /**
     * removes all children and attributes  */

    fun clear() {
        attributes = null
        children = null
    }

    /**
     * Forwards creation request to parent if any, otherwise
     * calls super.createElement.  */

    override fun createElement(namespace: String?, name: String): KMPElement {

        return if (this.parent == null)
            super.createElement(namespace, name)
        else
            this.parent!!.createElement(namespace, name)
    }

    fun getAttributeNamespace(index: Int): String {
        return attributes!!.elementAt(index)[0]
    }

    /*	public String getAttributePrefix (int index) {
		return ((String []) attributes.elementAt (index)) [1];
	}*/

    fun getAttributeName(index: Int): String {
        return attributes!!.elementAt(index)[1]
    }


    fun getAttributeValue(index: Int): String {
        return attributes!!.elementAt(index)[2]
    }


    fun getAttributeValue(namespace: String?, name: String): String? {
        for (i in 0 until attributeCount) {
            if (name == getAttributeName(i) && (namespace == null || namespace == getAttributeNamespace(i))) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    /**
     * returns the namespace of the element  */

    fun getNamespace(): String {
        return namespace
    }


    /**
     * returns the namespace for the given prefix  */

    fun getNamespaceUri(prefix: String?): String? {
        val cnt = namespaceCount
        for (i in 0 until cnt) {
            if (prefix === getNamespacePrefix(i) || prefix != null && prefix == getNamespacePrefix(i))
                return getNamespaceUri(i)
        }
        return if (parent is Element) (parent as Element).getNamespaceUri(prefix) else null
    }


    fun getNamespacePrefix(i: Int): String {
        return prefixes!!.elementAt(i)[0]
    }

    fun getNamespaceUri(i: Int): String {
        return prefixes!!.elementAt(i)[1]
    }

    /*
     * Returns the parent element if available, null otherwise

    public Element getParentElement() {
        return (parent instanceof Element)
            ? ((Element) parent)
            : null;
    }
*/

    /**
     * Builds the child elements from the given Parser. By overwriting
     * parse, an element can take complete control over parsing its
     * subtree.  */

    override fun parse(parser: KMPXmlParser) {

        for (i in parser.getNamespaceCount(parser.depth - 1) until parser.getNamespaceCount(parser.depth)) {
            setPrefix(parser.getNamespacePrefix(i), parser.getNamespaceUri(i))
        }


        for (i in 0 until parser.attributeCount)
            setAttribute(
                parser.getAttributeNamespace(i),
                //	        			  parser.getAttributePrefix (i),
                parser.getAttributeName(i),
                parser.getAttributeValue(i)
            )


        //        if (prefixMap == null) throw new RuntimeException ("!!");

        init()


        if (parser.isEmptyElementTag())
            parser.nextToken()
        else {
            parser.nextToken()
            super.parse(parser)

            if (childCount == 0)
                addChild(IGNORABLE_WHITESPACE, "")
        }

        parser.require(
            KMPPullParser.END_TAG,
            getNamespace(),
            name
        )

        parser.nextToken()
    }


    /**
     * Sets the given attribute; a value of null removes the attribute  */

    fun setAttribute(namespace: String?, name: String, value: String?) {
        var namespace = namespace
        if (attributes == null)
            attributes = Vector()

        if (namespace == null)
            namespace = ""

        for (i in attributes!!.indices.reversed()) {
            val attribut = attributes!!.elementAt(i) as Array<String>
            if (attribut[0] == namespace && attribut[1] == name) {

                if (value == null) {
                    attributes!!.removeElementAt(i)
                } else {
                    attribut[2] = value
                }
                return
            }
        }

        attributes!!.addElement(arrayOf<String>(namespace, name, value))
    }


    /**
     * Sets the given prefix; a namespace value of null removess the
     * prefix  */

    fun setPrefix(prefix: String, namespace: String) {
        if (prefixes == null) prefixes = Vector()
        prefixes!!.addElement(arrayOf(prefix, namespace))
    }

    /**
     * sets the namespace of the element. Please note: For no
     * namespace, please use Xml.NO_NAMESPACE, null is not a legal
     * value. Currently, null is converted to Xml.NO_NAMESPACE, but
     * future versions may throw an exception.  */

    fun setNamespace(namespace: String?) {
        if (namespace == null)
            throw NullPointerException("Use \"\" for empty namespace")
        this.namespace = namespace
    }


    /**
     * Writes this element and all children to the given XmlWriter.  */

    override fun write(writer: KMPSerializerParser) {

        if (prefixes != null) {
            for (i in prefixes!!.indices) {
                writer.setPrefix(getNamespacePrefix(i), getNamespaceUri(i))
            }
        }

        writer.startTag(
            getNamespace(),
            name
        )

        val len = attributeCount

        for (i in 0 until len) {
            writer.attribute(
                getAttributeNamespace(i),
                getAttributeName(i),
                getAttributeValue(i)
            )
        }

        writeChildren(writer)

        writer.endTag(getNamespace(), name)
    }
}