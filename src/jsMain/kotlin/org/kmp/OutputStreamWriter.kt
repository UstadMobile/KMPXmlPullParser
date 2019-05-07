package org.kmp

import kotlinx.io.OutputStream
import kotlinx.io.Writer
import kotlinx.serialization.toUtf8Bytes

actual class OutputStreamWriter : Writer {

    private var os: OutputStream

    actual constructor(os: OutputStream) {
        this.os = os
    }

    actual constructor(os: OutputStream, enc_: String) {
        this.os = os
    }

    override fun close() {
        os.close()
    }

    override fun flush() {
        os.flush()
    }

    override fun write(src: CharArray, off: Int, len: Int) {
        os.write(String(src, off, len).toUtf8Bytes())
    }
}