package org.kmp

import kotlinx.io.InputStream
import kotlinx.io.Reader

expect class InputStreamReader(input: InputStream, _enc: String) : Reader