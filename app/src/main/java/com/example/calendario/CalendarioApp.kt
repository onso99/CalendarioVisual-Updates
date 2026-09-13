package com.example.calendario

import androidx.compose.runtime.Composable

@Composable
fun CalendarioApp(
    themeManager: ThemeManager,
    onThemeUpdated: () -> Unit,
    viewModel: CalendarioViewModel,
    darkTheme: Boolean
) {
    CalendarioScreen(
        themeManager = themeManager,
        onThemeUpdated = onThemeUpdated,
        viewModel = viewModel,
        darkTheme = darkTheme
    )
}
