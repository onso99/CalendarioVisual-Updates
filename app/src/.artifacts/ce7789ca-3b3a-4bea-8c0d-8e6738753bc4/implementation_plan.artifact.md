# Plan de Implementación: Respeto a isForcedWorkingDay en isSunday (MonthlyCalendar y YearlyCalendar)

Este plan actualiza [MonthlyCalendar.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/MonthlyCalendar.kt) y [YearlyCalendar.kt](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/java/com/example/calendario/YearlyCalendar.kt) para que `isSunday` compruebe `!isForcedWorkingDay`, permitiendo que los festivos de Google Calendar marcados como laborables se pinten con el color de día normal.

---

## Cambios Propuestos

### 1. Actualizar `isSunday` en `MonthlyCalendar.kt`
- Definir `val isSunday = !isForcedWorkingDay && (date.dayOfWeek == DayOfWeek.SUNDAY)`.

### 2. Actualizar `isSunday` en `YearlyCalendar.kt`
- Definir `val isSunday = !isForcedWorkingDay && (date.dayOfWeek == DayOfWeek.SUNDAY)`.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla que los festivos de Google Calendar marcados como laborables se muestran con el color de día normal en las vistas mensual y anual.
