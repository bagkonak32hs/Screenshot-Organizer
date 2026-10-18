package com.example

import com.example.ui.theme.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeModeTest {

    @Test
    fun testAppThemeModeValues() {
        val modes = AppThemeMode.values()
        assertEquals(3, modes.size)

        val system = AppThemeMode.valueOf("SYSTEM")
        assertEquals(AppThemeMode.SYSTEM, system)
        assertNotNull(system.title)
        assertNotNull(system.subtitle)

        val light = AppThemeMode.valueOf("LIGHT")
        assertEquals(AppThemeMode.LIGHT, light)

        val dark = AppThemeMode.valueOf("DARK")
        assertEquals(AppThemeMode.DARK, dark)
    }

    @Test
    fun testDefaultFallback() {
        val parsed = try {
            AppThemeMode.valueOf("INVALID_THEME")
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
        assertEquals(AppThemeMode.SYSTEM, parsed)
    }
}
