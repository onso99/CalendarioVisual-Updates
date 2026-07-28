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

data class CustomColors(
    val cabecera: Color,
    val settingsBackground: Color,
    val textSundayHoliday: Color,
    val textBirthday: Color,
    val textEvent1: Color,
    val textEvent2: Color,
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

    // Color para etiquetas estructurales (CONTRASTE REAL sobre el color del tema)
    val textLabel: Color
        get() {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(cabecera.toArgb(), hsl)
            // Umbral al 75% para forzar texto blanco incluso en colores muy claros
            val isCabeceraDark = hsl[2] < 0.75f 
            
            // Aplicamos contraste: Si el fondo es <75%, texto muy claro; si es >75%, texto muy oscuro
            hsl[2] = if (isCabeceraDark) 0.85f else 0.15f
            return Color(ColorUtils.HSLToColor(hsl))
        }

    // Cálculo automático del borde del día actual (Tintado sutil basado en el color principal)
    val monthlyCalendarTodayCellBorder: Color
        get() {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(cabecera.toArgb(), hsl)
            val isCellDark = ColorUtils.calculateLuminance(monthlyCalendarDayCellBackground.toArgb()) < 0.5
            hsl[2] = if (isCellDark) 0.80f else 0.25f
            return Color(ColorUtils.HSLToColor(hsl))
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

    val onPrimaryColor = if (isColorDark(customColors.cabecera, customColors.settingsBackground)) Color.White else Color.Black

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem
        )
    } else {
        lightColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isColorDark(colorScheme.primary, customColors.settingsBackground)
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
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    fun getSafeInt(key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (_: ClassCastException) {
            (prefs.all[key] as? Number)?.toInt() ?: default
        }
    }

    return if (darkTheme) {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.DARK_CABECERA, AppConstants.DarkColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.DarkColors.settingsBackground.toArgb())),
            textSundayHoliday = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppConstants.DarkColors.textSundayHoliday.toArgb())),
            textBirthday = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY, AppConstants.DarkColors.textBirthday.toArgb())),
            textEvent1 = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_1, AppConstants.DarkColors.textEvent1.toArgb())),
            textEvent2 = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_2, AppConstants.DarkColors.textEvent2.toArgb())),
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.DarkColors.todayHighlightColor.toArgb())),
            noteIconColor = Color(getSafeInt(AppConstants.ColorKeys.DARK_NOTE_ICON_COLOR, AppConstants.DarkColors.noteIconColor.toArgb())),
            monthlyCalendarGridBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.DarkColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.DarkColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.DarkColors.monthlyCalendarDayCellBackground.toArgb()))
        )
    } else {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.LightColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground.toArgb())),
            textSundayHoliday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday.toArgb())),
            textBirthday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday.toArgb())),
            textEvent1 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1.toArgb())),
            textEvent2 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2.toArgb())),
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor.toArgb())),
            noteIconColor = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.LightColors.noteIconColor.toArgb())),
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
