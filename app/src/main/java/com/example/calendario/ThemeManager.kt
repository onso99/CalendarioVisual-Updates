package com.example.calendario

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeSetting(@field:StringRes val displayNameRes: Int) {
    SYSTEM(R.string.system_default),
    LIGHT(R.string.light_theme),
    DARK(R.string.dark_theme)
}

class ThemeManager(private val context: Context) {
    private val _themeSetting = MutableStateFlow(getSavedThemeSetting())
    val themeSetting = _themeSetting.asStateFlow()

    private fun getSavedThemeSetting(): ThemeSetting {
        return SettingsManager.getThemeSetting(context)
    }

    fun setTheme(theme: ThemeSetting) {
        _themeSetting.value = theme
        SettingsManager.saveThemeSetting(context, theme)
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager(context) }
}
