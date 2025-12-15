package com.example.calendario.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.calendario.AppConstants

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        cabecera = AppConstants.LightColors.cabecera,
        fondoSecciones = AppConstants.LightColors.fondoSecciones,
        fondoDialogos = AppConstants.LightColors.fondoDialogos,
        settingsBackground = AppConstants.LightColors.settingsBackground,
        background = AppConstants.LightColors.background,
        onBackground = AppConstants.LightColors.onBackground,
        error = AppConstants.LightColors.error,
        onError = AppConstants.LightColors.onError,
        textSystem = AppConstants.LightColors.textSystem,
        textSundayHoliday = AppConstants.LightColors.textSundayHoliday,
        textBirthday = AppConstants.LightColors.textBirthday,
        textEventDefault = AppConstants.LightColors.textEventDefault,
        textEvent1 = AppConstants.LightColors.textEvent1,
        textEvent2 = AppConstants.LightColors.textEvent2,
        dropdownMenuBackground = AppConstants.LightColors.dropdownMenuBackground,
        todayHighlightColor = AppConstants.LightColors.todayHighlightColor,
        eventListTitleColor = AppConstants.LightColors.eventListTitleColor,
        toggleButtonSelectedBackground = AppConstants.LightColors.toggleButtonselectedBackground,
        monthlyCalendarGridBackground = AppConstants.LightColors.monthlyCalendarGridBackground,
        monthlyCalendarDayCellBackground = AppConstants.LightColors.monthlyCalendarDayCellBackground,
        monthlyCalendarEmptyCellBackground = AppConstants.LightColors.monthlyCalendarEmptyCellBackground,
        monthlyCalendarTodayCellBorder = AppConstants.LightColors.monthlyCalendarTodayCellBorder,
        monthlyCalendarHeaderBackground = AppConstants.LightColors.monthlyCalendarHeaderBackground,
        monthlyCalendarDayNumberNormal = AppConstants.LightColors.monthlyCalendarDayNumberNormal,
        miniMonthTodayHighlightBackground = AppConstants.LightColors.miniMonthTodayHighlightBackground,
        miniMonthDayNumberNormal = AppConstants.LightColors.miniMonthDayNumberNormal
    )
}

@Composable
fun CalendarioTheme(
    darkTheme: Boolean,
    themeUpdateTrigger: Int, // Agrega este parámetro
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val customColors = remember(darkTheme, themeUpdateTrigger) {
        getThemeColors(context, darkTheme)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.cabecera,
            onPrimary = customColors.onBackground,
            background = customColors.background,
            onBackground = customColors.onBackground,
            surface = customColors.fondoSecciones,
            onSurface = customColors.onBackground,
            error = customColors.error,
            onError = customColors.onError
        )
    } else {
        lightColorScheme(
            primary = customColors.cabecera,
            onPrimary = customColors.onBackground,
            background = customColors.background,
            onBackground = customColors.onBackground,
            surface = customColors.fondoSecciones,
            onSurface = customColors.onBackground,
            error = customColors.error,
            onError = customColors.onError
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object CalendarioTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}