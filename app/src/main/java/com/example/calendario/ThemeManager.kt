package com.example.calendario

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeSetting(val displayName: String) {
    LIGHT("Claro"),
    DARK("Oscuro"),
    SYSTEM("Del sistema")
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    private val _themeSetting = MutableStateFlow(getSavedThemeSetting())
    val themeSetting = _themeSetting.asStateFlow()

    private fun getSavedThemeSetting(): ThemeSetting {
        val savedTheme = prefs.getString(AppConstants.KEY_THEME_SETTING, ThemeSetting.LIGHT.name)
        return ThemeSetting.valueOf(savedTheme ?: ThemeSetting.LIGHT.name)
    }

    fun setTheme(theme: ThemeSetting) {
        _themeSetting.value = theme
        prefs.edit().putString(AppConstants.KEY_THEME_SETTING, theme.name).apply()
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager(context) }
}
