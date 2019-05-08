package org.kmp

import kotlin.test.Test
import kotlin.test.assertEquals

class JavaAsserts {

    companion object {
        fun <T> javaAssertEquals(message: String, expected: T, actual: T) {
            assertEquals(expected, actual, message)
        }

        fun <T> javaAssertEquals(expected: T, actual: T) {
            assertEquals(expected, actual)
        }

    }

}