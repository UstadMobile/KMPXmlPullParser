package org.kmp

import kotlinx.io.ByteArrayOutputStream
import kotlinx.io.InputStream
import kotlinx.io.Reader
import kotlinx.io.StringReader
import kotlinx.serialization.stringFromUtf8Bytes

actual class InputStreamReader actual constructor(input: InputStream, _enc: String) : Reader() {

    private var reader: StringReader

    init {
        var bout = ByteArrayOutputStream()
        var bytesRead: Int
        try {
            while (input.read(bout.toByteArray()).also { bytesRead = it } != -1) {
                bout.write(bytesRead)
            }
        } finally {
            input.close()
        }
        reader = StringReader(stringFromUtf8Bytes(bout.toByteArray()))
    }

    override fun read(dst: CharArray, off: Int, len: Int): Int {
        return reader.read(dst, off, len)
    }

    override fun close() {
        reader.close()
    }

}