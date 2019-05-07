/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// see LICENSE.txt in distribution for copyright and license information

package org.kmp.io

import kotlinx.io.Reader
import org.kmp.io.XmlPullParserException

/**
 * XML Pull Parser is an interface that defines parsing functionlity provided
 * in [XMLPULL V1 API](http://www.xmlpull.org/) (visit this website to
 * learn more about API and its implementations).
 *
 *
 * There are following different
 * kinds of parser depending on which features are set:
 *  * behaves like XML 1.0 comliant non-validating parser
 * *if no DOCDECL is present* in XML documents when
 * FEATURE_PROCESS_DOCDECL is false (this is **default parser**
 * and internal enetites can still be defiend with defineEntityReplacementText())
 *  * non-validating parser as defined in XML 1.0 spec when
 * FEATURE_PROCESS_DOCDECL is true
 *  * validating parser as defined in XML 1.0 spec when
 * FEATURE_VALIDATION is true (and that implies that FEATURE_PROCESS_DOCDECL is true)
 *
 *
 *
 *
 * There are only two key methods: next() and nextToken() that provides
 * access to high level parsing events and to lower level tokens.
 *
 *
 * The parser is always in some event state and type of the current event
 * can be determined by calling
 * [getEventType()](#next()) mehod.
 * Initially parser is in [START_DOCUMENT](#START_DOCUMENT) state.
 *
 *
 * Method [next()](#next()) return int that contains identifier of parsing event.
 * This method can return following events (and will change parser state to the returned event):<dl>
 * <dt>[START_TAG](#START_TAG)</dt><dd> XML start tag was read
</dd> * <dt>[TEXT](#TEXT)</dt><dd> element contents was read and is available via getText()
</dd> * <dt>[END_TAG](#END_TAG)</dt><dd> XML end tag was read
</dd> * <dt>[END_DOCUMENT](#END_DOCUMENT)</dt><dd> no more events is available
</dd></dl> *
 *
 * The minimal working example of use of API would be looking like this:
 * <pre>
 * import java.io.IOException;
 * import java.io.StringReader;
 *
 * import org.kmp.io.XmlPullParser;
 * import org.xmlpull.v1.XmlPullParserException;
 * import org.xmlpull.v1.XmlPullParserFactory;
 *
 * public class SimpleXmlPullApp
 * {
 *
 * public static void main (String args[])
 * throws XmlPullParserException, IOException
 * {
 * XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
 * factory.setNamespaceAware(true);
 * XmlPullParser xpp = factory.newPullParser();
 *
 * xpp.setInput ( new StringReader ( "&lt;foo>Hello World!&lt;/foo>" ) );
 * int eventType = xpp.getEventType();
 * while (eventType != xpp.END_DOCUMENT) {
 * if(eventType == xpp.START_DOCUMENT) {
 * System.out.println("Start document");
 * } else if(eventType == xpp.END_DOCUMENT) {
 * System.out.println("End document");
 * } else if(eventType == xpp.START_TAG) {
 * System.out.println("Start tag "+xpp.getName());
 * } else if(eventType == xpp.END_TAG) {
 * System.out.println("End tag "+xpp.getName());
 * } else if(eventType == xpp.TEXT) {
 * System.out.println("Text "+xpp.getText());
 * }
 * eventType = xpp.next();
 * }
 * }
 * }
</pre> *
 *
 *
 * When run it will produce following output:
 * <pre>
 * Start document
 * Start tag foo
 * Text Hello World!
 * End tag foo
</pre> *
 *
 *
 * For more details on use of API please read
 * Quick Introduction available at [http://www.xmlpull.org](http://www.xmlpull.org)
 *
 * @see XmlPullParserFactory
 *
 * @see .defineEntityReplacementText
 *
 * @see .next
 *
 * @see .nextToken
 *
 * @see .FEATURE_PROCESS_DOCDECL
 *
 * @see .FEATURE_VALIDATION
 *
 * @see .START_DOCUMENT
 *
 * @see .START_TAG
 *
 * @see .TEXT
 *
 * @see .END_TAG
 *
 * @see .END_DOCUMENT
 *
 *
 * @author Stefan Haustein
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */

interface XmlPullParser {


    // --------------------------------------------------------------------------
    // miscellaneous reporting methods

    /**
     * Returns the current depth of the element.
     * Outside the root element, the depth is 0. The
     * depth is incremented by 1 when a start tag is reached.
     * The depth is decremented AFTER the end tag
     * event was observed.
     *
     * <pre>
     * &lt;!-- outside --&gt;     0
     * &lt;root>               1
     * sometext           1
     * &lt;foobar&gt;         2
     * &lt;/foobar&gt;        2
     * &lt;/root&gt;              1
     * &lt;!-- outside --&gt;     0
     * &lt;/pre&gt;
    </pre> *
     */
    fun getDepth(): Int

    /**
     * Short text describing parser position, including a
     * description of the current event and data source if known
     * and if possible what parser was seeing lastly in input.
     * This method is especially useful to give more meaningful error messages.
     */

    fun getPositionDescription(): String


    /**
     * Current line number: numebering starts from 1.
     */
    fun getLineNumber(): Int

    /**
     * Current column: numbering starts from 0 (returned when parser is in START_DOCUMENT state!)
     */
    fun getColumnNumber(): Int


    // --------------------------------------------------------------------------
    // TEXT related methods

    /**
     * Check if current TEXT event contains only whitespace characters.
     * For IGNORABLE_WHITESPACE, this is always true.
     * For TEXT and CDSECT if the current event text contains at lease one non white space
     * character then false is returned. For any other event type exception is thrown.
     *
     *
     * **NOTE:**  non-validating parsers are not
     * able to distinguish whitespace and ignorable whitespace
     * except from whitespace outside the root element. ignorable
     * whitespace is reported as separate event which is exposed
     * via nextToken only.
     *
     *
     * **NOTE:** this function can be only called for element content related events
     * such as TEXT, CDSECT or IGNORABLE_WHITESPACE otherwise
     * exception will be thrown!
     */

    fun isWhitespace(): Boolean

    /**
     * Read text content of the current event as String.
     */

    fun getText(): String

    // --------------------------------------------------------------------------
    // START_TAG / END_TAG shared methods

    /**
     * Returns the namespace URI of the current element.
     * If namespaces are NOT enabled, an empty String ("") always is returned.
     * The current event must be START_TAG or END_TAG, otherwise, null is returned.
     */
    fun getNamespace(): String


    /**
     * Returns the (local) name of the current element
     * when namespaces are enabled
     * or raw name when namespaces are disabled.
     * The current event must be START_TAG or END_TAG, otherwise null is returned.
     *
     * **NOTE:** to reconstruct raw element name
     * when namespaces are enabled you will need to
     * add prefix and colon to localName if prefix is not null.
     *
     */
    fun getName(): String

    /**
     * Returns the prefix of the current element
     * or null if elemet has no prefix (is in defualt namespace).
     * If namespaces are not enabled it always returns null.
     * If the current event is not  START_TAG or END_TAG the null value is returned.
     */
    fun getPrefix(): String


    /**
     * Returns true if the current event is START_TAG and the tag is degenerated
     * (e.g. &lt;foobar/&gt;).
     *
     * **NOTE:** if parser is not on START_TAG then the exception will be thrown.
     */
    fun isEmptyElementTag(): Boolean

    // --------------------------------------------------------------------------
    // START_TAG Attributes retrieval methods

    /**
     * Returns the number of attributes on the current element;
     * -1 if the current event is not START_TAG
     *
     * @see .getAttributeNamespace
     *
     * @see .getAttributeName
     *
     * @see .getAttributePrefix
     *
     * @see .getAttributeValue
     */
    fun getAttributeCount(): Int


    // --------------------------------------------------------------------------
    // actual parsing methods

    /**
     * Returns the type of the current event (START_TAG, END_TAG, TEXT, etc.)
     *
     * @see .next
     * @see .nextToken
     */
    fun getEventType(): Int

    /**
     * Use this call to change the general behaviour of the parser,
     * such as namespace processing or doctype declaration handling.
     * This method must be called before the first call to next or
     * nextToken. Otherwise, an exception is trown.
     *
     * Example: call setFeature(FEATURE_PROCESS_NAMESPACES, true) in order
     * to switch on namespace processing. Default settings correspond
     * to properties requested from the XML Pull Parser factory
     * (if none were requested then all feautures are by default false).
     *
     * @exception XmlPullParserException if feature is not supported or can not be set
     * @exception IllegalArgumentException if feature string is null
     */
    fun setFeature(name: String, state: Boolean)

    /**
     * Return the current value of the feature with given name.
     *
     * **NOTE:** unknown features are <string>always returned as false
     *
     * @param name The name of feature to be retrieved.
     * @return The value of named feature.
     * @exception IllegalArgumentException if feature string is null
    </string> */

    fun getFeature(name: String): Boolean

    /**
     * Set the value of a property.
     *
     * The property name is any fully-qualified URI.
     */
    fun setProperty(name: String, value: Any)

    /**
     * Look up the value of a property.
     *
     * The property name is any fully-qualified URI. I
     *
     * **NOTE:** unknown features are <string>always returned as null
     *
     * @param name The name of property to be retrieved.
     * @return The value of named property.
    </string> */
    fun getProperty(name: String): Any


    /**
     * Set the input for parser. Parser event state is set to START_DOCUMENT.
     * Using null parameter will stop parsing and reset parser state
     * allowing parser to free internal resources (such as parsing buffers).
     */
    fun setInput(`in`: Reader)

    /**
     * Set new value for entity replacement text as defined in
     * [XML 1.0 Section 4.5
 * Construction of Internal Entity Replacement Text](http://www.w3.org/TR/REC-xml#intern-replacement).
     * If FEATURE_PROCESS_DOCDECL or FEATURE_VALIDATION are set then calling this
     * function will reulst in exception because when processing of DOCDECL is enabled
     * there is no need to set manually entity replacement text.
     *
     *
     * The motivation for this function is to allow very small implementations of XMLPULL
     * that will work in J2ME environments and though may not be able to process DOCDECL
     * but still can be made to work with predefined DTDs by using this function to
     * define well known in advance entities.
     * Additionally as XML Schemas are replacing DTDs by allowing parsers not to process DTDs
     * it is possible to create more efficient parser implementations
     * that can be used as underlying layer to do XML schemas validation.
     *
     *
     *
     * **NOTE:** this is replacement text and it is not allowed
     * to contain any other entity reference
     *
     * **NOTE:** list of pre-defined entites will always contain standard XML
     * entities (such as &amp;amp; &amp;lt; &amp;gt; &amp;quot; &amp;apos;)
     * and they cannot be replaced!
     *
     * @see .setInput
     *
     * @see .FEATURE_PROCESS_DOCDECL
     *
     * @see .FEATURE_VALIDATION
     */
    fun defineEntityReplacementText(entityName: String, replacementText: String)

    /**
     * Return position in stack of first namespace slot for element at passed depth.
     * If namespaces are not enabled it returns always 0.
     *
     * **NOTE:** default namespace is not included in namespace table but
     * available by getNamespace() and not available from getNamespace(String)
     *
     * @see .getNamespacePrefix
     *
     * @see .getNamespaceUri
     *
     * @see .getNamespace
     * @see .getNamespace
     */
    fun getNamespaceCount(depth: Int): Int

    /**
     * Return namespace prefixes for position pos in namespace stack
     */
    fun getNamespacePrefix(pos: Int): String

    /**
     * Return namespace URIs for position pos in namespace stack
     * If pos is out of range it throw exception.
     */
    fun getNamespaceUri(pos: Int): String

    /**
     * Return uri for the given prefix.
     * It is depending on current state of parser to find
     * what namespace uri is mapped from namespace prefix.
     * For example for 'xsi' if xsi namespace prefix
     * was declared to 'urn:foo' it will return 'urn:foo'.
     *
     *
     * It will return null if namespace could not be found.
     *
     *
     * Convenience method for
     *
     * <pre>
     * for (int i = getNamespaceCount (getDepth ())-1; i >= 0; i--) {
     * if (getNamespacePrefix (i).equals (prefix)) {
     * return getNamespaceUri (i);
     * }
     * }
     * return null;
    </pre> *
     *
     *
     * However parser implementation can be more efficient about.
     *
     * @see .getNamespaceCount
     *
     * @see .getNamespacePrefix
     *
     * @see .getNamespaceUri
     */


    fun getNamespace(prefix: String): String


    /**
     * Get the buffer that contains text of the current event and
     * start offset of text is passed in first slot of input int array
     * and its length is in second slot.
     *
     *
     * **NOTE:** this buffer must not
     * be modified and its content MAY change after call to next() or nextToken().
     *
     *
     * **NOTE:** this methid must return always the same value as getText()
     * and if getText() returns null then this methid returns null as well and
     * values returned in holder MUST be -1 (both start and length).
     *
     * @see .getText
     *
     *
     * @param holderForStartAndLength the 2-element int array into which
     * values of start offset and length will be written into frist and second slot of array.
     * @return char buffer that contains text of current event
     * or null if the current event has no text associated.
     */
    fun getTextCharacters(holderForStartAndLength: IntArray): CharArray

    /**
     * Returns the namespace URI of the specified attribute
     * number index (starts from 0).
     * Returns empty string ("") if namespaces are not enabled or attribute has no namespace.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     *
     * **NOTE:** if FEATURE_REPORT_NAMESPACE_ATTRIBUTES is set
     * then namespace attributes (xmlns:ns='...') amust be reported
     * with namespace
     * [http://www.w3.org/2000/xmlns/](http://www.w3.org/2000/xmlns/)
     * (visit this URL for description!).
     * The default namespace attribute (xmlns="...") will be reported with empty namespace.
     * Then xml prefix is bound as defined in
     * [Namespaces in XML](http://www.w3.org/TR/REC-xml-names/#ns-using)
     * specification to "http://www.w3.org/XML/1998/namespace".
     *
     * @param zero based index of attribute
     * @return attribute namespace or "" if namesapces processing is not enabled.
     */
    fun getAttributeNamespace(index: Int): String

    /**
     * Returns the local name of the specified attribute
     * if namespaces are enabled or just attribute name if namespaces are disabled.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param zero based index of attribute
     * @return attribute names
     */
    fun getAttributeName(index: Int): String

    /**
     * Returns the prefix of the specified attribute
     * Returns null if the element has no prefix.
     * If namespaces are disabled it will always return null.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param zero based index of attribute
     * @return attribute prefix or null if namesapces processing is not enabled.
     */
    fun getAttributePrefix(index: Int): String

    /**
     * Returns the given attributes value
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param zero based index of attribute
     * @return value of attribute
     */
    fun getAttributeValue(index: Int): String

    /**
     * Returns the attributes value identified by namespace URI and namespace localName.
     * If namespaces are disbaled namespace must be null.
     * If current event type is not START_TAG then IndexOutOfBoundsException will be thrown.
     *
     * @param namespace Namespace of the attribute if namespaces are enabled otherwise must be null
     * @param name If namespaces enabled local name of attribute otherwise just attribute name
     * @return value of attribute
     */
    fun getAttributeValue(namespace: String, name: String): String


    /**
     * Get next parsing event - element content wil be coalesced and only one
     * TEXT event must be returned for whole element content
     * (comments and processing instructions will be ignored and emtity references
     * must be expanded or exception mus be thrown if entity reerence can not be exapnded).
     * If element content is empty (content is "") then no TEXT event will be reported.
     *
     *
     * **NOTE:** empty element (such as &lt;tag/>) will be reported
     * with  two separate events: START_TAG, END_TAG - it must be so to preserve
     * parsing equivalency of empty element to &lt;tag>&lt;/tag>.
     * (see isEmptyElementTag ())
     *
     * @see .isEmptyElementTag
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     */

    operator fun next(): Int


    /**
     * This method works similarly to next() but will expose
     * additional event types (COMMENT, CDSECT, DOCDECL, ENTITY_REF, PROCESSING_INSTRUCTION, or
     * IGNORABLE_WHITESPACE) if they are available in input.
     *
     *
     * If special feature FEATURE_XML_ROUNDTRIP
     * (identified by URI: http://xmlpull.org/v1/doc/features.html#xml-roundtrip)
     * is true then it is possible to do XML document round trip ie. reproduce
     * exectly on output the XML input using getText().
     *
     *
     * Here is the list of tokens that can be  returned from nextToken()
     * and what getText() and getTextCharacters() returns:<dl>
     * <dt>START_DOCUMENT</dt><dd>null
    </dd> * <dt>END_DOCUMENT</dt><dd>null
    </dd> * <dt>START_TAG</dt><dd>null
     * unless FEATURE_XML_ROUNDTRIP enabled and then returns XML tag, ex: &lt;tag attr='val'>
    </dd> * <dt>END_TAG</dt><dd>null
     * unless FEATURE_XML_ROUNDTRIP enabled and then returns XML tag, ex: &lt;/tag>
    </dd> * <dt>TEXT</dt><dd>return unnormalized element content
    </dd> * <dt>IGNORABLE_WHITESPACE</dt><dd>return unnormalized characters
    </dd> * <dt>CDSECT</dt><dd>return unnormalized text *inside* CDATA
     * ex. 'fo&lt;o' from &lt;!CDATA[fo&lt;o]]>
    </dd> * <dt>PROCESSING_INSTRUCTION</dt><dd>return unnormalized PI content ex: 'pi foo' from &lt;?pi foo?>
    </dd> * <dt>COMMENT</dt><dd>return comment content ex. 'foo bar' from &lt;!--foo bar-->
    </dd> * <dt>ENTITY_REF</dt><dd>return unnormalized text of entity_name ()
     * <br></br>**NOTE:** it is user responsibility to resolve entity reference
     * <br></br>**NOTE:** character entities and standard entities such as
     * &amp;amp; &amp;lt; &amp;gt; &amp;quot; &amp;apos; are reported as well
     * and are not resolved and not reported as TEXT tokens!
     * This requirement is added to allow to do roundtrip of XML documents!
    </dd> * <dt>DOCDECL</dt><dd>return inside part of DOCDECL ex. returns:<pre>
     * &quot; titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]&quot;</pre>
    </dd></dl> *
     * for input document that contained:<pre>
     * &lt;!DOCTYPE titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]></pre>
     *
     *
     *
     *
     * **NOTE:** returned text of token is not end-of-line normalized.
     *
     * @see .next
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     *
     * @see .COMMENT
     *
     * @see .DOCDECL
     *
     * @see .PROCESSING_INSTRUCTION
     *
     * @see .ENTITY_REF
     *
     * @see .IGNORABLE_WHITESPACE
     */

    fun nextToken(): Int

    //-----------------------------------------------------------------------------
    // utility methods to mak XML parsing easier ...

    /**
     * test if the current event is of the given type and if the
     * namespace and name do match. null will match any namespace
     * and any name. If the current event is TEXT with isWhitespace()=
     * true, and the required type is not TEXT, next () is called prior
     * to the test. If the test is not passed, an exception is
     * thrown. The exception text indicates the parser position,
     * the expected event and the current event (not meeting the
     * requirement.
     *
     *
     * essentially it does this
     * <pre>
     * if (getEventType() == TEXT && type != TEXT && isWhitespace ())
     * next ();
     *
     * if (type != getEventType()
     * || (namespace != null && !namespace.equals (getNamespace ()))
     * || (name != null && !name.equals (getName ()))
     * throw new XmlPullParserException ( "expected "+ TYPES[ type ]+getPositionDesctiption());
    </pre> *
     */
    fun require(type: Int, namespace: String, name: String)


    /**
     * If the current event is text, the value of getText is
     * returned and next() is called. Otherwise, an empty
     * String ("") is returned. Useful for reading element
     * content without needing to performing an additional
     * check if the element is empty.
     *
     *
     * essentially it does this
     * <pre>
     * if (getEventType != TEXT) return ""
     * String result = getText ();
     * next ();
     * return result;
    </pre> *
     */
    fun readText(): String

    companion object {

        /** This constant represents lack of or default namespace (empty string "")  */
        val NO_NAMESPACE = ""

        // ----------------------------------------------------------------------------
        // EVENT TYPES as reported by next()

        /**
         * EVENT TYPE and TOKEN: signalize that parser is at the very beginning of the document
         * and nothing was read yet - the parser is before first call to next() or nextToken()
         * (available from [next()](#next()) and [nextToken()](#nextToken())).
         *
         * @see .next
         *
         * @see .nextToken
         */
        val START_DOCUMENT = 0

        /**
         * EVENT TYPE and TOKEN: logical end of xml document
         * (available from [next()](#next()) and [nextToken()](#nextToken())).
         *
         *
         * **NOTE:** calling again
         * [next()](#next()) or [nextToken()](#nextToken())
         * will result in exception being thrown.
         *
         * @see .next
         *
         * @see .nextToken
         */
        val END_DOCUMENT = 1

        /**
         * EVENT TYPE and TOKEN: start tag was just read
         * (available from [next()](#next()) and [nextToken()](#nextToken())).
         * The name of start tag is available from getName(), its namespace and prefix are
         * available from getNamespace() and getPrefix()
         * if [namespaces are enabled](#FEATURE_PROCESS_NAMESPACES).
         * See getAttribute* methods to retrieve element attributes.
         * See getNamespace* methods to retrieve newly declared namespaces.
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getName
         *
         * @see .getPrefix
         *
         * @see .getNamespace
         *
         * @see .getAttributeCount
         *
         * @see .getDepth
         *
         * @see .getNamespaceCount
         *
         * @see .getNamespace
         *
         * @see .FEATURE_PROCESS_NAMESPACES
         */
        val START_TAG = 2

        /**
         * EVENT TYPE and TOKEN: end tag was just read
         * (available from [next()](#next()) and [nextToken()](#nextToken())).
         * The name of start tag is available from getName(), its namespace and prefix are
         * available from getNamespace() and getPrefix()
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getName
         *
         * @see .getPrefix
         *
         * @see .getNamespace
         *
         * @see .FEATURE_PROCESS_NAMESPACES
         */
        val END_TAG = 3


        /**
         * EVENT TYPE and TOKEN: character data was read and will be available by call to getText()
         * (available from [next()](#next()) and [nextToken()](#nextToken())).
         *
         * **NOTE:** next() will (in contrast to nextToken ()) accumulate multiple
         * events into one TEXT event, skipping IGNORABLE_WHITESPACE,
         * PROCESSING_INSTRUCTION and COMMENT events.
         *
         * **NOTE:** if state was reached by calling next() the text value will
         * be normalized and if the token was returned by nextToken() then getText() will
         * return unnormalized content (no end-of-line normalization - it is content exactly as in
         * input XML)
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val TEXT = 4

        // ----------------------------------------------------------------------------
        // additional events exposed by lower level nextToken()

        /**
         * TOKEN: CDATA sections was just read
         * (this token is available only from [nextToken()](#nextToken())).
         * The value of text inside CDATA section is available  by callling getText().
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val CDSECT: Byte = 5

        /**
         * TOKEN: Entity reference was just read
         * (this token is available only from [nextToken()](#nextToken())).
         * The entity name is available by calling getText() and it is user responsibility
         * to resolve entity reference.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val ENTITY_REF: Byte = 6

        /**
         * TOKEN: Ignorable whitespace was just read
         * (this token is available only from [nextToken()](#nextToken())).
         * For non-validating
         * parsers, this event is only reported by nextToken() when
         * outside the root elment.
         * Validating parsers may be able to detect ignorable whitespace at
         * other locations.
         * The value of ignorable whitespace is available by calling getText()
         *
         *
         * **NOTE:** this is different than callinf isWhitespace() method
         * as element content may be whitespace but may not be ignorable whitespace.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val IGNORABLE_WHITESPACE: Byte = 7

        /**
         * TOKEN: XML processing instruction declaration was just read
         * and getText() will return text that is inside processing instruction
         * (this token is available only from [nextToken()](#nextToken())).
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val PROCESSING_INSTRUCTION: Byte = 8

        /**
         * TOKEN: XML comment was just read and getText() will return value inside comment
         * (this token is available only from [nextToken()](#nextToken())).
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val COMMENT = 9

        /**
         * TOKEN: XML DOCTYPE declaration was just read
         * and getText() will return text that is inside DOCDECL
         * (this token is available only from [nextToken()](#nextToken())).
         *
         * @see .nextToken
         *
         * @see .getText
         */
        val DOCDECL = 10


        /**
         * Use this array to convert evebt type number (such as START_TAG) to
         * to string giving event name, ex: "START_TAG" == TYPES[START_TAG]
         */
        val TYPES = arrayOf(
            "START_DOCUMENT",
            "END_DOCUMENT",
            "START_TAG",
            "END_TAG",
            "TEXT",
            "CDSECT",
            "ENTITY_REF",
            "IGNORABLE_WHITESPACE",
            "PROCESSING_INSTRUCTION",
            "COMMENT",
            "DOCDECL"
        )


        // ----------------------------------------------------------------------------
        // namespace related features

        /**
         * FEATURE: Processing of namespaces is by default set to false.
         *
         * **NOTE:** can not be changed during parsing!
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        val FEATURE_PROCESS_NAMESPACES = "http://xmlpull.org/v1/doc/features.html#process-namespaces"

        /**
         * FEATURE: Report namespace attributes also - they can be distinguished
         * looking for prefix == "xmlns" or prefix == null and name == "xmlns
         * it is off by default and only meaningful when FEATURE_PROCESS_NAMESPACES feature is on.
         *
         * **NOTE:** can not be changed during parsing!
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        val FEATURE_REPORT_NAMESPACE_ATTRIBUTES = "http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes"

        /**
         * FEATURE: Processing of DOCDECL is by default set to false
         * and if DOCDECL is encountered it is reported by nextToken()
         * and ignored by next().
         *
         * If processing is set to true then DOCDECL must be processed by parser.
         *
         *
         * **NOTE:** if the DOCDECL was ignored
         * further in parsing there may be fatal exception when undeclared
         * entity is encountered!
         *
         * **NOTE:** can not be changed during parsing!
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        val FEATURE_PROCESS_DOCDECL = "http://xmlpull.org/v1/doc/features.html#process-docdecl"

        /**
         * FEATURE: Report all validation errors as defined by XML 1.0 sepcification
         * (implies that FEATURE_PROCESS_DOCDECL is true and both internal and external DOCDECL
         * will be processed).
         *
         * **NOTE:** can not be changed during parsing!
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        val FEATURE_VALIDATION = "http://xmlpull.org/v1/doc/features.html#validation"
    }

}
