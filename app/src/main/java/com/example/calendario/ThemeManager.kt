package com.example.calendario

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeSetting(@field:StringRes val displayNameRes: Int) {
    LIGHT(R.string.light_theme),
    DARK(R.string.dark_theme),
    SYSTEM(R.string.system_default)
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
        prefs.edit {
            putString(AppConstants.KEY_THEME_SETTING, theme.name)
        }
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager(context) }
}
