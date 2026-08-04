---
name: Prodigiosos App
description: Sistema Nocturne diseñado por el propietario en Claude Design («Prodigiosos App v2») — grafitos neutros, barra lateral, acento elegible por el usuario, asistentes paso a paso.
colors:
  bg: "#1B1D21"
  sidebar: "#17181C"
  card: "#212429"
  field: "#16181B"
  hover: "#2C3038"
  border-7: "rgba(255,255,255,.07)"
  border-12: "rgba(255,255,255,.12)"
  border-14: "rgba(255,255,255,.14)"
  text: "#E8E9EC"
  soft: "#B9BEC6"
  muted: "#8A9099"
  dim: "#6B7178"
  success: "#8FBF8F"
  danger-soft: "#C98A86"
  danger: "#C4544D"
  warn: "#D6A45E"
  on-accent: "#16181B"
accents:
  morado: { base: "#968AE0", hi: "#A89DEA", deep: "#6A5FC0" }   # por defecto
  azul: { base: "#6FA8DC", hi: "#8ABBE6", deep: "#4A7FB0" }
  verde: { base: "#7FBF8F", hi: "#95CDA3", deep: "#5A9A6C" }
  amarillo: { base: "#D6B45E", hi: "#E2C67E", deep: "#B08F3C" }
  rojo: { base: "#D1706A", hi: "#DD8A85", deep: "#AB4F4A" }
typography:
  family: "Sans del sistema (Segoe UI en Windows); FontFamily.Monospace para direcciones, rutas e INI"
  page-title: "17sp SemiBold"
  card-title: "15sp SemiBold (+ contador '· N' en 13sp muted)"
  row-title: "13.5sp"
  body: "13sp"
  help: "12.5sp muted, lineHeight 18–20sp"
  meta: "11.5sp dim"
rounded:
  button: "6.dp"
  card: "8.dp"
  modal: "10.dp"
  nav-item: "5.dp"
spacing:
  page: "26.dp horizontal, 22.dp vertical; gap 14.dp"
  card-header: "18.dp horizontal, 13.dp vertical"
  row: "18.dp horizontal, 8–11.dp vertical"
  modal: "20.dp"
---

# Design System: Prodigiosos App (Nocturne)

## Overview

**Autoridad: el mock del propietario** («Prodigiosos App v2.dc.html», Claude Design, 2026-08-04). Este documento registra su traducción a Compose Desktop tal como quedó construida. El acento NO es fijo: el usuario lo elige en Ajustes entre cinco (`AppSettings.accentColor`, morado por defecto) y viaja por `LocalAccent`; ningún componente debe usar un color de acento cableado.

**Estructura global:** barra lateral de 200dp (#17181C) con título de la app, tres ítems de navegación y el selector de cuenta de Steam abajo; área de contenido (#1B1D21) con padding 26/22 y gap 14. Sin barra de pestañas superior. Banner de actualización y guía de puesta a punto como tarjetas encima del contenido de la pestaña.

## Componentes (Components.kt)

- **`AppCard`**: panel #212429, borde 1px blanco 7%, radio 8. Cabecera con `CardTitle` (15sp SemiBold + "· N"). Filas separadas por `HairLine` (1px blanco 5–7%).
- **Botones** (radio 6, 13sp SemiBold, padding ~16×8): `PrimaryButton` (relleno acento, texto #16181B), `GhostButton` (borde blanco 14%, texto soft), `AccentGhostButton` (borde acento 50%, texto accent.hi), `SuccessGhostButton` (verde, para Conectar), `DangerGhostButton` (borde #C98A86 45%), `DangerButton` (relleno #C4544D, en confirmaciones), `GhostIconButton` (cuadrado 28–29dp con icono 14dp).
- **`NavItem`** (App.kt): radio 5, seleccionado = fondo acento 14% + barra izquierda de 2dp en acento + texto SemiBold.
- **`AppTextField`**: fondo #16181B, borde blanco 12% (foco: acento 60%), radio 6; `mono=true` para INI/direcciones.
- **`StatusMessage`** (toast): tinta acento 10% + borde 35% (error: rojo), descartable; los éxitos se autodescartan a los 5 s.
- **`WarnBanner`**: ámbar 10% + borde 32%, texto ámbar 12sp.
- **Modales**: `AppModal` (ancho fijo 420–620dp, radio 10, borde blanco 12%, scrim), `ModalHeader` (título + X), `ConfirmModal` (título/cuerpo/aviso opcional + Cancelar y commit), `WizardSteps` (puntos numerados 22dp unidos por líneas: activo = relleno acento; hecho = acento 18% + borde 50%; pendiente = #2C3038), `DoneCircle` (44dp verde 14% con check).
- **`ModeCard`** (ProfilesTab): opción de asistente con emoji, título y descripción; seleccionada = borde acento.

## Patrones de pantalla

- **Cabecera de pestaña**: título 17sp SemiBold a la izquierda; acción primaria "＋ …" a la derecha.
- **Listas**: dentro de una única `AppCard` a pantalla completa; filas con checkbox/tick + texto de dos líneas (nombre 13.5sp, meta mono 11.5sp) + acciones a la derecha; pie fijo con la acción en masa. Estados vacíos centrados: título 15sp + cuerpo muted + CTA primario.
- **Asistentes de 3 pasos** (añadir servidores: Pegar→Revisar→Listo; añadir perfil: Origen→Detalles→Listo): siempre en `AppModal` con `WizardSteps`; el paso final muestra `DoneCircle` + resumen + nota.
- **Confirmaciones**: `ConfirmModal`; lo destructivo usa `DangerButton`; aplicar perfil con ARK abierto muestra `WarnBanner` y deshabilita el commit.
- **Diálogo de Steam**: cerrar Steam para escribir favoritos y **relanzarlo al terminar** (`SteamProcess.launchSteam`).

## Do's and Don'ts

### Do:
- **Do** leer el acento de `LocalAccent` y los neutros de `Palette`; nada de hex sueltos en pantallas.
- **Do** usar los componentes compartidos para todo botón, tarjeta, campo y modal.
- **Do** mono para direcciones `host:puerto`, rutas e INI.
- **Do** pasar todo texto por `i18n/Strings.kt` (ES + EN).
- **Do** flujos nuevos como asistente de 3 pasos si tienen más de un paso.

### Don't:
- **Don't** cablear un color de acento ni añadir acentos fuera de los cinco de `Accents`.
- **Don't** usar sombras/elevación: la profundidad es tonal + bordes finos.
- **Don't** usar píldoras Material ni radios fuera de 5/6/8/10dp.
- **Don't** ejecutar destructivos sin `ConfirmModal`.
- **Don't** bloquear la UI con avisos permanentes: toasts autodescartables y banners solo cuando aplican.
