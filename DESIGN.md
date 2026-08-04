---
name: Prodigiosos App
description: Utilidad de escritorio oscura, sobria y premium (listón Discord/Steam nuevo) — negro neutro, un único acento cian, bandas rectangulares y títulos con tracking.
colors:
  accent-cyan: "#4DD0E1"
  on-accent: "#00363D"
  hint-amber: "#FFB74D"
  ground: "#0E1013"
  panel: "#15181D"
  inset-panel: "#1D2127"
  text: "#E2E5E9"
  muted-text: "#A6ADB8"
  outline: "#2A2F36"
  success: "#66BB6A"
  error: "#FF6E6E"
  on-error: "#3D0000"
  disabled-text: "#6A7178"
typography:
  header:
    fontFamily: "Material3 por defecto (sans del sistema)"
    fontWeight: 700
    letterSpacing: "2.4sp"
  title:
    fontFamily: "Material3 por defecto (sans del sistema)"
    fontWeight: 600
    letterSpacing: "1.2sp–1.6sp"
  label:
    fontFamily: "Material3 por defecto (sans del sistema)"
    letterSpacing: "1.0sp–1.2sp"
  body:
    fontFamily: "Material3 por defecto (sans del sistema)"
  mono:
    fontFamily: "FontFamily.Monospace"
    fontSize: "13.sp (editor INI); bodySmall en direcciones y rutas"
rounded:
  band: "6.dp"
  panel: "8.dp"
spacing:
  xs: "8.dp"
  sm: "12.dp"
  md: "16.dp"
  lg: "20.dp"
components:
  button-commit:
    backgroundColor: "{colors.accent-cyan}"
    textColor: "{colors.on-accent}"
    rounded: "{rounded.band}"
    padding: "20.dp horizontal, 10.dp vertical"
    note: "HorizonButton — texto en MAYÚSCULAS; una sola por página"
  button-destructive:
    backgroundColor: "{colors.error}"
    textColor: "{colors.on-error}"
    rounded: "{rounded.band}"
  section-card:
    backgroundColor: "{colors.panel}"
    rounded: "{rounded.panel}"
    padding: "16.dp"
  info-banner:
    backgroundColor: "{colors.hint-amber} al 12% de alfa"
    textColor: "{colors.text}"
    rounded: "{rounded.band}"
---

# Design System: Prodigiosos App

## Overview

**Norte creativo: el estándar de la categoría, ejecutado impecable.** Compromiso de marca registrado en PRODUCT.md (2026-07-30): oscuro neutro premium con **un único acento**, el cian de marca `#4DD0E1`, al nivel de acabado de Discord / Steam nuevo. Se construyó y descartó un mundo expresivo (ciclorama cobalto/rosa, seed dc4bfb92 — ver `.impeccable/approved.json`); el usuario fijó la sobriedad. Futuro trabajo visual: refinar dentro de esta paleta, nunca reemplazarla ni añadir segundos acentos.

**Key Characteristics:**
- Fondo plano neutro (#0E1013) sin degradados de página; paneles mate sin sombra.
- Títulos en MAYÚSCULAS con tracking ("registro de cue") sobre una regla de acento de 2dp.
- Botones como bandas rectangulares de 6dp, nunca píldoras Material.
- Direcciones, puertos, rutas e IDs siempre en monoespaciada.
- Público no técnico hispanohablante: todo texto vía `i18n/Strings.kt` (ES+EN); el color nunca es la única señal.

## Colors

### Accent (único)
- **Cian de marca** (#4DD0E1): estructura e interacción. Botón de commit (`HorizonButton`), botones primarios, indicador de pestaña (`HorizonRule`: degradado cian→cian 15% como subrayado), número de paso pendiente, banner de actualización (12% alfa). Texto sobre cian: #00363D.

### Semánticos
- **Éxito** (#66BB6A, `SuccessGreen`): completado/válido — checks del cue strip, mensajes de éxito, ruta detectada, cuenta fijada, perfil activo (chip al 18% de alfa).
- **Aviso** (#FFB74D, secondary): relleno de `InfoBanner` al 12% con icono a tinta plena.
- **Error** (#FF6E6E): errores y acciones destructivas; texto #3D0000. Siempre con `ConfirmDialog` en lo destructivo.

### Neutrales
- Fondo #0E1013 → panel #15181D → panel embutido #1D2127 (filas, deshabilitados) → tintes de alfa 12–22%.
- Texto #E2E5E9; atenuado #A6ADB8; contorno #2A2F36; texto deshabilitado #6A7178.

### Named Rules
**La regla del acento único.** Un solo color de acento en toda la app. Verde, ámbar y rojo son semánticos, no decorativos. El color nunca es la única señal: siempre acompaña icono o etiqueta.

## Typography

Material3 por defecto (sans del sistema), modificada en `CueTypography`; mono para todo lo que "lee la máquina".

- **Header de app** (titleLarge, Bold, 2.4sp): solo el título en cabecera.
- **Título de sección** (`CueTitle`: titleSmall SemiBold 1.2sp, MAYÚSCULAS) sobre la regla de acento de 2dp.
- **Label** (labelLarge 1.2sp / labelMedium 1.0sp): botones (MAYÚSCULAS en `HorizonButton`), pestañas (MAYÚSCULAS + icono 16dp).
- **Body** (bodyMedium/bodySmall sin modificar): prosa y ayudas.
- **Mono**: toda dirección `host:puerto`, ruta, ID o el editor INI (13.sp). Si hay nombre amistoso, nombre en sans y dirección debajo en mono atenuado.

## Layout

Ventana única en columna: cabecera (20dp h / 14dp v), banner de actualización opcional, pestañas transparentes, cue strip de primer arranque, contenido con padding 20dp h / 12–20dp v. Favoritos: dos columnas iguales separadas 20dp, sin scroll externo — las listas scrollean dentro de su panel con el botón de acción fijo. Ritmo `spacedBy` 8/12/16dp; interior de panel 16dp.

## Elevation & Depth

Sin sombras. Profundidad tonal: fondo → panel → panel embutido → tintes de alfa. `TabRow` transparente.

## Shapes

Bandas de 6dp (`ButtonShape`) para botones, banners, mensajes y filas; 8dp para paneles (`SectionCard`). Única forma circular: el círculo de 18dp del paso pendiente. Firma geométrica: la regla de acento de 2dp (28dp bajo títulos; ancho de pestaña como indicador).

## Components

- **`HorizonButton` (commit):** banda cian sólida, texto MAYÚSCULAS en #00363D; exactamente una por página (añadir seleccionados, aplicar perfil). Deshabilitado: #1D2127 / #6A7178.
- **`Button` primario:** cian de esquema + `shape = ButtonShape`. **Destructivo:** containerColor error + onError, tras `ConfirmDialog`. **Secundario:** `OutlinedButton` con `ButtonShape`; terciario `TextButton`.
- **`SectionCard`:** 8dp, #15181D, sin borde ni sombra, padding 16dp, título vía `CueTitle`.
- **`OutlinedTextField`:** contorno #2A2F36; mono donde aplica; errores vía `StatusMessage`.
- **`TabRow`:** transparente; indicador 2dp `HorizonRule`.
- **`InfoBanner` / `StatusMessage`:** 6dp, tinta al 12–14% + icono 18dp; éxito verde, error rojo.
- **Cue Strip (`SetupStrip`):** checklist de primer arranque sobre #1D2127 al 55%; pendiente = círculo cian 22% con dígito; completo = check verde 16dp; descartable y persistente.

## Do's and Don'ts

### Do:
- **Do** mantener un solo acento (cian) y los semánticos solo para su significado.
- **Do** usar `CueTitle` para todo título de sección y `ButtonShape` para todo botón.
- **Do** usar mono para toda dirección/puerto/ruta/ID.
- **Do** pasar todo texto por `i18n/Strings.kt` (ES+EN).

### Don't:
- **Don't** introducir paletas expresivas, segundos acentos ni degradados de página (compromiso de marca).
- **Don't** añadir sombras/elevación/bordes a paneles; profundidad tonal.
- **Don't** usar píldoras Material ni radios fuera de 6/8dp.
- **Don't** animar fondos (rendimiento fijado: estático).
- **Don't** ejecutar destructivos sin `ConfirmDialog`.
