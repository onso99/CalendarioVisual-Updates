package com.example.calendario

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAppConstants() {
        // Al referenciar una clase del proyecto principal, eliminamos el warning de "Unnecessary module dependency"
        assertNotNull(AppConstants.APP_SETTINGS_PREFS_NAME)
    }
}