/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// see LICENSE.txt in distribution for copyright and license information

package org.kmp.io

/**
 * This exception is thrown to signal XML Pull Parser related faults.
 *
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom/]
 */
class XmlPullParserException : Exception {
    var detail: Throwable? = null
    var lineNumber = -1
        protected set
    var columnNumber = -1
        protected set

    constructor() {}

    constructor(s: String) : super(s) {}

    constructor(s: String, thrwble: Throwable) : super(s) {
        this.detail = thrwble
    }

    constructor(s: String, row: Int, column: Int) : super(s) {
        this.lineNumber = row
        this.columnNumber = column
    }


    constructor(msg: String, parser: XmlPullParser) : super(msg + parser.getPositionDescription()) {
        this.lineNumber = parser.getLineNumber()
        this.columnNumber = parser.getColumnNumber()
    }

    override fun getMessage(): String {
        return if (detail == null)
            super.message
        else
            super.message + "; nested exception is: \n\t"
                    + detail!!.message
    }

    //NOTE: code that prints this and detail is difficult in J2ME
    override fun printStackTrace() {
        if (detail == null) {
            super.printStackTrace()
        } else {
            synchronized(System.err) {
                System.err.println(super.message + "; nested exception is:")
                detail!!.printStackTrace()
            }
        }
    }

}
