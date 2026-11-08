# Plan de Implementación: Opción "Borde de Hoy" en Celdas

Este plan añade la personalización del color del borde del día actual ("Borde de Hoy") en la sección **Celdas** de la pantalla *Personalizar colores*, permitiendo configurarlo de forma independiente para modo claro y oscuro, y propagando la nueva cadena a todos los idiomas de la app.

---

## Cambios Propuestos

### 1. Recurso de Cadena (`strings.xml` y 11 idiomas)
- Añadir `<string name="today_border_color">Borde de Hoy</string>` al archivo principal y su correspondiente traducción a los 11 archivos de localización.

### 2. Constantes y Tema (`AppConstants.kt` y `Theme.kt`)
- Añadir claves `LIGHT_TODAY_BORDER_COLOR` y `DARK_TODAY_BORDER_COLOR`.
- Añadir `todayBorderColor` al modelo `CalendarioColors` leyendo desde SharedPreferences.

### 3. Configuración de Color (`ColorThemeConfig.kt`)
- Añadir `ColorThemeItem` para `today_border_color` bajo la categoría `"Celdas"`, justo debajo de `current_month_cell`.

### 4. Uso en Vistas Mensual y Anual (`MonthlyCalendar.kt` y `YearlyCalendar.kt`)
- Reemplazar el borde negro/blanco duro de la celda de "Hoy" por `themeColors.todayBorderColor`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que la opción aparece en Ajustes -> Personalizar colores -> Celdas y que afecta al borde del día actual en el calendario mensual y anual.
