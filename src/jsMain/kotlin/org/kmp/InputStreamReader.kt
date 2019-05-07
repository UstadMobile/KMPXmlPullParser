package org.kmp

import kotlinx.io.ByteArrayOutputStream
import kotlinx.io.InputStream
import kotlinx.io.Reader
import kotlinx.io.StringReader

actual class InputStreamReader actual constructor(var input: InputStream, _enc: String) : Reader() {

    var reader: StringReader

    init {
        var bout = ByteArrayOutputStream()
        var bytesRead: Int = -1

        while ({ bytesRead = input.read(bout.toByteArray()); bytesRead }() != -1) {
            bout.write(bytesRead)
        }
        var string: String = String(bout.toByteArray())


        reader = StringReader()
    }

    override fun read(dst: CharArray, off: Int, len: Int): Int {

        return 0
    }

    override fun close() {
        input.close()
    }

}