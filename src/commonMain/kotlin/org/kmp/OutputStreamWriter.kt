package org.kmp

import kotlinx.io.OutputStream
import kotlinx.io.Writer

expect class OutputStreamWriter: Writer {
    constructor(os: OutputStream)
    constructor(os: OutputStream, enc_ : String)
}