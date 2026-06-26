package com.example.calendario

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada, que se ejecutará en un dispositivo Android.
 * Ver [documentación de pruebas](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la aplicación bajo prueba.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.calendario", appContext.packageName)
    }
}
