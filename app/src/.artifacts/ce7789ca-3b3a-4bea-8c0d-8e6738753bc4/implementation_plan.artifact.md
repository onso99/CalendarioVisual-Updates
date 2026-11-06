# Plan de Implementación: Localización Completa de `event_duration_days`

Este plan añade la cadena `event_duration_days` traducida a sus respectivos idiomas en los 11 archivos de localización y remueve el ignore en el archivo principal en [strings.xml](file:///C:/Users/onso9/Documents/AndroidStudio/Calendario/app/src/main/res/values/strings.xml).

---

## Cambios Propuestos

### 1. Archivo Principal (`res/values/strings.xml`)
- Actualizar `<string name="event_duration_days">%1$d días</string>` (sin `tools:ignore`).

### 2. Archivos de Localización (11 idiomas)
- Añadir la traducción nativa correspondiente de `event_duration_days` en:
  - `values-en`: `%1$d days`
  - `values-ca`: `%1$d dies`
  - `values-de`: `%1$d Tage`
  - `values-eu`: `%1$d egun`
  - `values-fr`: `%1$d jours`
  - `values-gl`: `%1$d días`
  - `values-it`: `%1$d giorni`
  - `values-ja`: `%1$d日`
  - `values-pt`: `%1$d dias`
  - `values-ru`: `%1$d дней`
  - `values-zh`: `%1$d天`

---

## Plan de Verificación

- Inspeccionar con `analyze_file`.
- Compilar con `gradle_build("app:assembleDebug")`.
- Verificar compilación exitosa sin warnings de traducción.
