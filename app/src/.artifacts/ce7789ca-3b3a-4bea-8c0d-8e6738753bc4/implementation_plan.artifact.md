# Plan de Implementación: Mejoras en Festivos, Scroll en Gestor y Banner en Búsqueda

Este plan implementa 4 mejoras solicitadas:
1. Excluir festivos de Google marcados como laborables de la lista de eventos y del diálogo de día del mes (`CalendarDataUtils.kt` y `DialogsEvents.kt`).
2. Desplazar la lista en `HolidayManagerScreen.kt` al festivo seleccionado cuando se pasa `initialFestivo`.
3. Desplazar la lista en `HolidayManagerScreen.kt` al festivo más próximo a la fecha actual ("Hoy") en la apertura normal.
4. Añadir el banner temporal de ayuda ("Pulsación larga para multiselección") en `SearchScreen.kt`.

---

## Cambios Propuestos

### 1. Actualizar `CalendarDataUtils.kt` y `DialogsEvents.kt`
- En `CalendarDataUtils.kt`, actualizar el filtro de laborables para incluir todas las fechas marcadas como `WORKING_DAY`.
- En `DialogsEvents.kt` (`DayEventsDialog`), excluir de la lista los festivos cuya fecha esté en `workingDayDates`.

### 2. Actualizar `HolidayManagerScreen.kt`
- Usar `rememberLazyListState()` y un `LaunchedEffect` para desplazar automáticamente la lista (`scrollToItem`) al festivo seleccionado o al más próximo a la fecha actual.

### 3. Actualizar `SearchScreen.kt`
- Añadir el banner de ayuda temporal de 5 segundos con el icono `Info` y color de cabecera.

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar en pantalla cada uno de los 4 comportamientos.
