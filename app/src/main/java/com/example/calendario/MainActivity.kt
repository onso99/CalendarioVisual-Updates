package com.example.calendario

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.calendario.ui.theme.CalendarioTheme

class MainActivity : ComponentActivity() {

    private val calendarioViewModel: CalendarioViewModel by viewModels()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContent {
            val themeManager = rememberThemeManager()
            val themeSetting by themeManager.themeSetting.collectAsState()
            var themeUpdateTrigger by remember { mutableIntStateOf(0) }
            val onThemeUpdated = { themeUpdateTrigger += 1 }

            val useDarkTheme = when (themeSetting) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            CalendarioTheme(darkTheme = useDarkTheme, themeUpdateTrigger = themeUpdateTrigger) {
                CalendarioApp(
                    themeManager = themeManager,
                    onThemeUpdated = onThemeUpdated,
                    viewModel = calendarioViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        calendarioViewModel.refreshData()
    }
}
