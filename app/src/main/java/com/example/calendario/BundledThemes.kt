package com.example.calendario

object BundledThemes {
    val themes = listOf(
        // Océano Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Océano",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "gradient"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF0077C2",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFF0F9FE",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#30D6CF15",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FF6700FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF0077C2",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF38A391",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#4081D4FA"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF01579B",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF001F29",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#804FC3F7",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF004D40",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF01579B",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF003341",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#4001579B"
            )
        ),
        // Bosque Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Bosque",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "sweep"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF388E3C",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFF1F8E9",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80C8E6C9",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFA5D6A7",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF388E3C",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40A5D6A7"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF1B5E20",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF122112",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#8081C784",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF2E7D32",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF1B5E20",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF1A3A1A",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#401B5E20"
            )
        ),
        // Volcán Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Volcán",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "radial"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FFE64A19",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFFFF3E0",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80FFCCBC",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFFFB74D",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FFE64A19",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40FFB74D"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FFBF360C",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF211A13",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#80FF8A65",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFD84315",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FFBF360C",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF4E342E",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40BF360C"
            )
        ),
        // Amanecer Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Amanecer",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "gradient"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FFFF8F00",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFFFF8E1",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80FFECB3",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFFFD54F",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FFFF8F00",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40FFD54F"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FFC56000",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF261C00",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#80FFB74D",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFFFB300",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FFC56000",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF403000",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40C56000"
            )
        ),
        // Verde Oliva Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Verde Oliva",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "gradient"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF827717",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFF9FBE7",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80F0F4C3",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFDCE775",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF827717",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40DCE775"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF558B2F",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF1A1C1A",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#80AED581",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF33691E",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF558B2F",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF2E352E",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40558B2F"
            )
        ),
        // Grafito Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Grafito",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "radial"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF607D8B",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFECEFF1",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80CFD8DC",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFB0BEC5",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF607D8B",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40B0BEC5"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF455A64",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF263238",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#8090A4AE",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF546E7A",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF455A64",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF37474F",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40455A64"
            )
        ),
        // Tierra Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Tierra",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "sweep"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF795548",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFFBE9E7",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80FFCCBC",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFFFCCBC",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF795548",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40FFCCBC"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF5D4037",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF261F1C",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#80A1887F",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF6D4C41",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF5D4037",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF4E342E",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#405D4037"
            )
        ),
        // Lavanda Theme
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Lavanda",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "gradient"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF7E57C2",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFF3E5F5",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80E1BEE7",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FFCE93D8",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF7E57C2",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40CE93D8"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF673AB7",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF1E122A",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#80BA68C8",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF8E24AA",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF673AB7",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF311B92",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#40673AB7"
            )
        )
    )
}
