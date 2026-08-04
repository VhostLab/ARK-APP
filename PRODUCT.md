# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop

(App nativa de escritorio para Windows — Kotlin + Compose Multiplatform for Desktop. No es web ni móvil; ninguno de los cuatro valores del esquema aplica.)

## Users

Miembros de la comunidad **Prodigiosos** (cluster de servidores de ARK: Survival Evolved, organizada en Discord, hispanohablante). El usuario típico **no es nada técnico**: no sabe qué es un puerto de consulta ni dónde está la carpeta del juego. Sigue instrucciones de Discord y copia/pega, pero todo lo demás debe ser a prueba de errores y en su idioma. La UI existe en español (primario) e inglés.

## Product Purpose

El trabajo nº1: que un **miembro nuevo de la comunidad deje ARK listo para jugar en minutos y sin pedir ayuda** — los servidores del cluster añadidos a los favoritos de Steam y el `BaseDeviceProfiles.ini` optimizado aplicado. Trabajo secundario recurrente: reaplicar el INI cuando Steam lo restaura y mantener la lista de favoritos cuando cambian los servidores. Éxito = cero preguntas de soporte en Discord para la puesta en marcha.

## Positioning

En lugar de tutoriales de Discord con pasos manuales (editar VDF/INI a mano, puertos, rutas), la app escribe directamente en los favoritos reales de Steam del usuario aceptando la lista de servidores tal y como circula por Discord (texto desordenado, con nombres y comas), siempre con vista previa antes de tocar nada, y gestiona perfiles de INI con copia de seguridad automática.

## Operating Context

- Distribución: MSI por Discord y GitHub Releases (repo público `VhostLab/ARK-APP`); auto-actualización integrada desde la v1.0.7.
- El usuario tiene Windows con Steam y ARK instalados; puede haber **varias cuentas de Steam en el mismo PC** (elegir la cuenta correcta importa).
- Steam debe estar **cerrado** al escribir favoritos (Steam sobrescribe la lista al salir); la app se ofrece a cerrarlo.
- Steam restaura `BaseDeviceProfiles.ini` al verificar integridad o actualizar el juego → existe el gesto "Reaplicar".
- Las listas de servidores llegan como texto pegado desde Discord; usan el **puerto de consulta** (27015+), no el de juego (7777) — confusión real y recurrente.

## Capabilities and Constraints

- Favoritos: parseo tolerante de listas pegadas → vista previa con selección → añadir/eliminar en el archivo de favoritos de Steam de la cuenta elegida (dedupe automático).
- Perfiles INI: guardar versiones, importar, editor integrado, aplicar con backup automático; datos de la app en `%APPDATA%\ARK-APP`.
- Instalación MSI per-user (sin UAC), con `upgradeUuid` fijo — actualiza en sitio.
- La app debe funcionar **íntegramente sin red**; el comprobador de actualizaciones falla en silencio.
- i18n obligatoria: toda cadena visible pasa por `Strings.kt` (ES + EN); nada hardcodeado en un solo idioma.
- Terminología asentada: "favoritos", "puerto de consulta", "perfiles", "reaplicar", "detección automática" (de cuenta/rutas).

## Brand Commitments

- Nombre: **Prodigiosos App** (renombrada desde "ARK-APP" en v1.0.5; el repo conserva el nombre antiguo).
- Icono existente: `packaging/icon.ico` / `src/main/resources/icon.png`.
- Vendor: Aimar.
- **Paleta (compromiso, 2026-07-30):** oscuro neutro premium con un único acento, el cian de marca `#4DD0E1`. El usuario rechazó explícitamente mundos visuales expresivos de color (se construyó y descartó un tema cobalto/rosa); el listón es la sobriedad y acabado de Discord / Steam nuevo. Futuro trabajo visual: refinar dentro de esta paleta, no reemplazarla.

## Evidence on Hand

- Formatos reales de listas de servidores capturados en los tests (`src/test/kotlin/com/arkapp/ServerListParserTest.kt`).
- README bilingüe como descripción canónica de funciones.
- No hay assets de marketing, testimonios ni métricas; el trabajo futuro no debe inventarlos.

## Product Principles

1. **A prueba de novatos antes que potente.** Un miembro nuevo sin conocimientos debe lograrlo a la primera y sin ayuda; cada paso arriesgado va protegido (vista previa, backups, aviso de Steam abierto).
2. **Entrada tolerante, resultado explícito.** Se acepta el texto desordenado del mundo real, pero siempre se muestra qué va a pasar antes (vista previa con selección) y qué pasó después (contadores de añadidos/omitidos).
3. **Español primero, inglés siempre.** Cada texto existe en ambos idiomas; el español es la lengua de la comunidad.
4. **Nunca perder datos del jugador.** Backup automático antes de sobrescribir; las operaciones son repetibles sin daño (dedupe, reaplicar).
5. **Silenciosa salvo cuando importa.** Funciona offline; la red y las actualizaciones fallan en silencio y los avisos son descartables.

## Accessibility & Inclusion

Sin estándar formal exigido. Requisito de idioma: español como lengua primaria de la comunidad, inglés como segunda; el nivel de lectura debe ser llano (usuarios no técnicos).
