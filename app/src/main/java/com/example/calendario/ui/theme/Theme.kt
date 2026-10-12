package com.example.calendario.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.calendario.AppConstants
import com.example.calendario.SettingsManager
import com.example.calendario.getContrastColor

data class CustomColors(
    val cabecera: Color,
    val settingsBackground: Color,
    val textSundayHoliday: Color,
    val textBirthday: Color,
    val textEvent1: Color,
    val textEvent2: Color,
    val textEvent3: Color,
    val textEvent4: Color,
    val textEvent5: Color,
    val todayHighlightColor: Color,
    val noteIconColor: Color,
    val monthlyCalendarGridBackground: Color,
    val monthlyCalendarGridEffect: Color,
    val monthlyCalendarDayCellBackground: Color,
) {
    // Cálculo automático del borde del día actual en el calendario anual
    val miniMonthTodayCellBorder: Color
        get() {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(cabecera.toArgb(), hsl)
            val isBgDark = ColorUtils.calculateLuminance(settingsBackground.toArgb()) < 0.5
            hsl[2] = if (isBgDark) 0.80f else 0.25f
            return Color(ColorUtils.HSLToColor(hsl))
        }

    // Cálculo automático del texto del sistema (NEUTRALIZADO: Gris 80% o 25% para datos base)
    val textSystem: Color
        get() {
            val isBgDark = ColorUtils.calculateLuminance(settingsBackground.toArgb()) < 0.5
            return if (isBgDark) Color(0xFFCCCCCC) else Color(0xFF404040)
        }

    // Cálculo automático del fondo de secciones (10% de variación respecto al fondo)
    val fondoSecciones: Color
        get() {
            val isBackgroundDark = ColorUtils.calculateLuminance(settingsBackground.toArgb()) < 0.5
            val factor = if (isBackgroundDark) 0.1f else -0.1f // 10% de variación

            return Color(
                red = (settingsBackground.red + factor).coerceIn(0f, 1f),
                green = (settingsBackground.green + factor).coerceIn(0f, 1f),
                blue = (settingsBackground.blue + factor).coerceIn(0f, 1f),
                alpha = settingsBackground.alpha
            )
        }

    // Cálculo automático del fondo de diálogos
    val fondoDialogos: Color
        get() {
            val isBackgroundDark = ColorUtils.calculateLuminance(settingsBackground.toArgb()) < 0.5
            return if (isBackgroundDark) {
                fondoSecciones
            } else {
                settingsBackground
            }
        }
}

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        cabecera = AppConstants.LightColors.cabecera,
        settingsBackground = AppConstants.LightColors.settingsBackground,
        textSundayHoliday = AppConstants.LightColors.textSundayHoliday,
        textBirthday = AppConstants.LightColors.textBirthday,
        textEvent1 = AppConstants.LightColors.textEvent1,
        textEvent2 = AppConstants.LightColors.textEvent2,
        textEvent3 = AppConstants.LightColors.textEvent3,
        textEvent4 = AppConstants.LightColors.textEvent4,
        textEvent5 = AppConstants.LightColors.textEvent5,
        todayHighlightColor = AppConstants.LightColors.todayHighlightColor,
        noteIconColor = AppConstants.LightColors.noteIconColor,
        monthlyCalendarGridBackground = AppConstants.LightColors.monthlyCalendarGridBackground,
        monthlyCalendarGridEffect = AppConstants.LightColors.monthlyCalendarGridEffect,
        monthlyCalendarDayCellBackground = AppConstants.LightColors.monthlyCalendarDayCellBackground
    )
}

@Composable
fun CalendarioTheme(
    darkTheme: Boolean,
    themeUpdateTrigger: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val customColors = remember(darkTheme, themeUpdateTrigger) {
        getThemeColors(context, darkTheme)
    }

    val onPrimaryColor = customColors.cabecera.getContrastColor(customColors.settingsBackground)
    val surfaceVariantColor = customColors.fondoSecciones
    val outlineColor = customColors.textSystem.copy(alpha = 0.5f)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            primaryContainer = customColors.cabecera.copy(alpha = 0.2f),
            onPrimaryContainer = customColors.textSystem,
            secondary = customColors.cabecera,
            onSecondary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.settingsBackground,
            onSurface = customColors.textSystem,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = customColors.textSystem,
            outline = outlineColor
        )
    } else {
        lightColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            primaryContainer = customColors.cabecera.copy(alpha = 0.1f),
            onPrimaryContainer = customColors.textSystem,
            secondary = customColors.cabecera,
            onSecondary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.settingsBackground,
            onSurface = customColors.textSystem,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = customColors.textSystem,
            outline = outlineColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = colorScheme.primary.getContrastColor(customColors.settingsBackground) != Color.White
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

fun getThemeColors(context: Context, darkTheme: Boolean): CustomColors {
    val prefs = SettingsManager.getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME)

    fun getSafeInt(key: String, default: Int): Int = SettingsManager.getSafeInt(prefs, key, default)

    // Colores Permanentes (Invariantes a modo oscuro y temas)
    val permanentSundayHoliday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday.toArgb()))
    val permanentBirthday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday.toArgb()))
    val permanentEvent1 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1.toArgb()))
    val permanentEvent2 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2.toArgb()))
    val permanentEvent3 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_3, AppConstants.LightColors.textEvent3.toArgb()))
    val permanentEvent4 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_4, AppConstants.LightColors.textEvent4.toArgb()))
    val permanentEvent5 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_5, AppConstants.LightColors.textEvent5.toArgb()))
    val permanentNoteIcon = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.LightColors.noteIconColor.toArgb()))

    return if (darkTheme) {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.DARK_CABECERA, AppConstants.DarkColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.DarkColors.settingsBackground.toArgb())),
            textSundayHoliday = permanentSundayHoliday,
            textBirthday = permanentBirthday,
            textEvent1 = permanentEvent1,
            textEvent2 = permanentEvent2,
            textEvent3 = permanentEvent3,
            textEvent4 = permanentEvent4,
            textEvent5 = permanentEvent5,
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.DarkColors.todayHighlightColor.toArgb())),
            noteIconColor = permanentNoteIcon,
            monthlyCalendarGridBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.DarkColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.DarkColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.DarkColors.monthlyCalendarDayCellBackground.toArgb()))
        )
    } else {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.LightColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground.toArgb())),
            textSundayHoliday = permanentSundayHoliday,
            textBirthday = permanentBirthday,
            textEvent1 = permanentEvent1,
            textEvent2 = permanentEvent2,
            textEvent3 = permanentEvent3,
            textEvent4 = permanentEvent4,
            textEvent5 = permanentEvent5,
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor.toArgb())),
            noteIconColor = permanentNoteIcon,
            monthlyCalendarGridBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.LightColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground.toArgb()))
        )
    }
}

fun isColorDark(color: Color, background: Color): Boolean {
    val contrastingColor = Color(ColorUtils.compositeColors(color.toArgb(), background.toArgb()))
    return ColorUtils.calculateLuminance(contrastingColor.toArgb()) < 0.5
}
