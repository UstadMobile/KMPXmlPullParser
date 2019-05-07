/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// see LICENSE.txt in distribution for copyright and license information

package org.kmp.io

import kotlinx.io.InputStream
import kotlin.jvm.JvmOverloads

/**
 * This class is used to create implementations of XML Pull Parser defined in XMPULL V1 API.
 * The name of actual facotry class will be determied based on several parameters.
 * It works similar to JAXP but tailored to work in J2ME environments
 * (no access to system properties or file system) so name of parser class factory to use
 * and its class used for loading (no classloader - on J2ME no access to context class loaders)
 * must be passed explicitly. If no name of parser factory was passed (or is null)
 * it will try to find name by searching in CLASSPATH for
 * META-INF/services/org.xmlpull.v1.XmlPullParserFactory resource that should contain
 * the name of parser facotry class. If none found it will try to create a default parser
 * factory (if available) or throw exception.
 *
 *
 * **NOTE:**In J2SE or J2EE environments to get best results use
 * `newInstance(property, classLoaderCtx)`
 * where first argument is
 * `System.getProperty(XmlPullParserFactory.DEFAULT_PROPERTY_NAME)`
 * and second is `Thread.getContextClassLoader().getClas()` .
 *
 * @see XmlPullParser
 *
 *
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom/]
 */

abstract class KMPPullParserFactory
//protected boolean namespaceAware;

/**
 * Protected constructor to be called by factory implementations.
 */
protected constructor() {

    // features are kept there
    protected var features = Hashtable()

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which are namespace aware.
     *
     * @return  true if the factory is configured to produce parsers
     * which are namespace aware; false otherwise.
     */
    /**
     * Specifies that the parser produced by this factory will provide
     * support for XML namespaces.
     * By default the value of this is set to false.
     *
     * @param awareness true if the parser produced by this code
     * will provide support for XML namespaces;  false otherwise.
     */
    var isNamespaceAware: Boolean
        get() {
            val value = features.get(XmlPullParser.FEATURE_PROCESS_NAMESPACES) as Boolean
            return if (value != null) value!!.booleanValue() else false
        }

        set(awareness) {
            features.put(XmlPullParser.FEATURE_PROCESS_NAMESPACES, awareness)
        }

    /**
     * Indicates whether or not the factory is configured to produce parsers
     * which validate the XML content during parse.
     *
     * @return   true if the factory is configured to produce parsers
     * which validate the XML content during parse; false otherwise.
     */
    /**
     * Specifies that the parser produced by this factory will be validating
     *
     * By default the value of this is set to false.
     *
     * @param validating - if true the parsers created by this factory  must be validating.
     */
    var isValidating: Boolean
        get() {
            val value = features.get(XmlPullParser.FEATURE_VALIDATION) as Boolean
            return if (value != null) value!!.booleanValue() else false
        }
        set(validating) {
            features.put(XmlPullParser.FEATURE_VALIDATION, validating)
        }

    /**
     * Set the features to be set when XML Pull Parser is created by this factory.
     *
     * @param name string with URI identifying feature
     * @param state if true feature will be set; if false will be ignored
     */
    fun setFeature(
        name: String,
        state: Boolean
    ) {
        features.put(name, state)
    }

    /**
     * Return the current value of the feature with given name.
     *
     * @param name The name of feature to be retrieved.
     * @return The value of named feature.
     * Unknown features are <string>always returned as false
    </string> */
    fun getFeature(name: String): Boolean {
        val value = features.get(name) as Boolean
        return if (value != null) value!!.booleanValue() else false
    }

    /**
     *
     * Creates a new instance of a XML Pull Parser
     * using the currently configured factory parameters.
     *
     * @return A new instance of a XML Pull Parser.
     * @throws XmlPullParserException if a parser cannot be created which satisfies the
     * requested configuration.
     */
    abstract fun newPullParser(): XmlPullParser

    companion object {
        private val DEBUG = false

        /** name of parser factory property that should be used for system property
         * or in general to retrieve parser factory clas sname from configuration
         * (currently name of peroperty is org.xmlpull.v1.XmlPullParserFactory)
         */
        val DEFAULT_PROPERTY_NAME = "org.xmlpull.v1.XmlPullParserFactory"

        //private static Class MY_CLASS;
        private val MY_REF = KMPPullParserException()//new XmlPullParserFactory();
        private val DEFAULT_KXML_IMPL_FACTORY_CLASS_NAME = "org.kxml2.io.XmlReader"
        private val DEFAULT_XPP_IMPL_FACTORY_CLASS_NAME = "org.xmlpull.xpp3.factory.Xpp3Factory"
        private val DEFAULT_RESOURCE_NAME = "/META-INF/services/$DEFAULT_PROPERTY_NAME"
        private var foundFactoryClassName: String? = null

        /**
         * Get a new instance of a PullParserFactory from given class name.
         *
         * @param factoryClassName use specified factory class if not null
         * @return result of call to newInstance(null, factoryClassName)
         */
        fun newInstance(factoryClassName: String): KMPPullParserFactory {
            return newInstance(null, factoryClassName)
        }

        private val PREFIX = "DEBUG XMLPULL factory: "

        /** Private method for debugging  */
        private fun debug(msg: String) {
            if (!DEBUG)
                throw RuntimeException(
                    "only when DEBUG enabled can print messages"
                )
            System.err.println(PREFIX + msg)
        }

        /**
         * Get instance of XML pull parser factiry.
         *
         *
         * **NOTE:**  this allows to use -D system properties indirectly and still
         * to support flexible configuration in J2ME environments..
         *
         * @param classLoaderCtx if null Class.forName will be used instead
         * - simple way to use class loaders and still have ME compatibility!
         * @param hint with name of parser factory to use -
         * it is a hint and is ignored if factory is not available.
         */
        @JvmOverloads
        fun newInstance(classLoaderCtx: Class<*>? = null, factoryClassName: String? = null): KMPPullParserException {
            var factoryClassName = factoryClassName

            // if user hinted factory then try to use it ...


            var factoryImpl: KMPPullParserFactory? = null
            if (factoryClassName != null) {
                try {
                    //*
                    var clazz: Class<*>? = null
                    if (classLoaderCtx != null) {
                        clazz = classLoaderCtx!!.forName(factoryClassName)
                    } else {
                        clazz = Class.forName(factoryClassName)
                    }
                    factoryImpl = clazz!!.newInstance() as KMPPullParserFactory
                    //foundFactoryClassName = factoryClassName;
                    //*/
                    if (DEBUG) debug("loaded factoryClassName " + clazz!!)
                } catch (ex: Exception) {
                    if (DEBUG) debug("failed to load factoryClassName " + factoryClassName!!)
                    if (DEBUG) ex.printStackTrace()
                    throw KMPPullParserException(
                        "could not create instance of XMLPULL factory for class " + factoryClassName
                                + " (root exception:" + ex + ")", ex)
                }

            }

            // if could not load then proceed with pre-configured
            if (factoryImpl == null) {

                // default factory is unknown - try to find it!
                if (foundFactoryClassName == null) {
                    findFactoryClassName(classLoaderCtx)
                }

                if (foundFactoryClassName != null) {
                    try {

                        var clazz: Class<*>? = null
                        if (classLoaderCtx != null) {
                            clazz = classLoaderCtx!!.forName(foundFactoryClassName)
                        } else {
                            clazz = Class.forName(foundFactoryClassName)
                        }
                        factoryImpl = clazz!!.newInstance() as XmlPullParserFactory
                        if (DEBUG) debug("loaded pre-configured " + clazz!!)
                    } catch (ex: Exception) {
                        if (DEBUG)
                            debug(("failed to use pre-configured " + foundFactoryClassName!!))
                        if (DEBUG) ex.printStackTrace()
                    }

                }
            }

            // still could not load then proceed with default
            if (factoryImpl == null) {
                try {
                    var clazz: Class<*>? = null
                    factoryClassName = DEFAULT_XPP_IMPL_FACTORY_CLASS_NAME
                    // give one more chance for small implementation
                    if (classLoaderCtx != null) {
                        clazz = classLoaderCtx!!.forName(factoryClassName)
                    } else {
                        clazz = Class.forName(factoryClassName)
                    }
                    factoryImpl = clazz!!.newInstance() as KMPPullParserFactory

                    if (DEBUG)
                        debug(("using default full implementation " + factoryImpl!!))

                    // make it as pre-configured default
                    foundFactoryClassName = factoryClassName

                } catch (ex2: Exception) {
                    try {
                        var clazz: Class<*>? = null
                        factoryClassName = DEFAULT_KXML_IMPL_FACTORY_CLASS_NAME
                        // give one more chance for small implementation
                        if (classLoaderCtx != null) {
                            clazz = classLoaderCtx!!.forName(factoryClassName)
                        } else {
                            clazz = Class.forName(factoryClassName)
                        }
                        factoryImpl = clazz!!.newInstance() as KMPPullParserFactory

                        if (DEBUG)
                            debug(
                                ("no factory was found instead " +
                                        "using small default impl " + factoryImpl)
                            )

                        // now it is pre-configured default
                        foundFactoryClassName = factoryClassName

                    } catch (ex3: Exception) {
                        throw KMPPullParserException(
                            ("could not load any factory class " + "(even small or full default implementation)"), ex3)
                    }

                }

            }

            // return what was found..
            if (factoryImpl == null)
                throw RuntimeException(
                    "XMLPULL: internal parser factory error"
                )
            return factoryImpl
        }

        // --- private utility methods

        /**
         * Finds factory class name from CLASSPATH based on META-INF/service/... resource.
         */
        private fun findFactoryClassName(classLoaderCtx: Class<*>?) {
            if (foundFactoryClassName != null)
            //return; // foundFactoryClassName;
                throw RuntimeException("internal XMLPULL initialization error")

            var `is`: InputStream? = null
            try {

                if (classLoaderCtx != null) {
                    if (DEBUG)
                        debug(
                            ("trying to load " + DEFAULT_RESOURCE_NAME +
                                    " from " + classLoaderCtx)
                        )
                    `is` = classLoaderCtx!!.getResourceAsStream(DEFAULT_RESOURCE_NAME)
                }

                if (`is` == null) {
                    val klass = MY_REF.javaClass //XmlPullParserFactory.getClass();
                    if (DEBUG)
                        debug(
                            ("opening " + DEFAULT_RESOURCE_NAME +
                                    " (class context " + klass + ")")
                        )
                    `is` = klass.getResourceAsStream(DEFAULT_RESOURCE_NAME)
                }

                if (`is` != null) {

                    foundFactoryClassName = readLine(`is`!!)

                    if (DEBUG)
                        debug(
                            "foundFactoryClassName=" + foundFactoryClassName!!
                        )

                    //if( foundFactoryClassName != null
                    //   && !  "".equals( foundFactoryClassName ) ) {
                    //  return foundFactoryClassName;
                    //}
                }
            } catch (ex: Exception) {
                if (DEBUG) ex.printStackTrace()
            } finally {
                if (`is` != null) {
                    try {
                        `is`!!.close()
                    } catch (ex: Exception) {
                    }

                }
            }
            //return foundFactoryClassName;
        }

        /**
         * Utility to read just one line of input without any charset encoding.
         *
         * @param    input               an InputStream
         *
         * @return   a String
         *
         * @throws   IOException
         */
        private fun readLine(input: InputStream): String {
            val sb = StringBuffer()

            while (true) {
                val ch = input.read()
                if (ch < 0) {
                    break
                } else if (ch == '\n'.toInt()) {
                    break
                }
                sb.append(ch.toChar())
            }

            // strip end-of-line \r\n if necessary
            val n = sb.length
            if ((n > 0) && (sb.get(n - 1) == '\r')) {
                sb.setLength(n - 1)
            }

            return (sb.toString())
        }
    }

    //    public XmlPullParser newPullParser() throws XmlPullParserException {
    ////        XmlPullParser pp = new org.xmlpull.v1.kxml.KXmlPullParserImpl();
    ////        pp.setNamespaceAware(namespaceAware);
    ////        return pp;
    //        //throw new XmlPullParserException("not implemented");
    //        throw new XmlPullParserException(
    //            "newPullParser() must be implemented by other class");
    //
    //    }

    //    /**
    //     * Create new XML pull parser with passed propertiesin binary flasgs
    //     * @see XmlPullParser for list of supported flags
    //     */
    //    public XmlPullParser newPullParser(int properties) throws XmlPullParserException {
    //        //throw new XmlPullParserException(
    //        //    "newPullParser() must be implemented by other class");
    //        XmlPullParser xpp = newPullParser();
    //        xpp.setProperty(properties, true);
    //        return xpp;
    //    }

}
/**
 * Create a new instance of a PullParserFactory used to create XML pull parser
 * (see description of class for more details).
 *
 * @return result of call to newInstance(null, null)
 */
/**
 * Get a new instance of a PullParserFactory used to create XML Pull Parser.
 *
 * **NOTE:** passing classLoaderCtx is not very useful in ME
 * but can be useful in J2SE, J2EE or in container environments (such as servlets)
 * where multiple class loaders are used
 * (it is using Class as ClassLoader is not in ME profile).
 *
 * @param classLoaderCtx if not null it is used to find
 * default factory and to create instance
 * @return result of call to newInstance(classLoaderCtx, null)
 */
