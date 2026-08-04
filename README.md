# Prodigiosos App

App de escritorio para **ARK: Survival Evolved** (Windows). / Desktop app for **ARK: Survival Evolved** (Windows).

## Descarga / Download

**[Descargar la última versión (MSI) / Download the latest release](https://github.com/VhostLab/ARK-APP/releases/latest)**

Desde la v1.0.7 la app comprueba actualizaciones al arrancar y ofrece instalarlas con un clic.
*Since v1.0.7 the app checks for updates on startup and offers to install them in one click.*

## Funciones / Features

- **Favoritos de Steam**: pega una lista de servidores en cualquier formato (Discord, tablas web con el nombre encima de la IP…), revisa la vista previa y añádelos a los favoritos de Steam. Nombres reales consultados a cada servidor (A2S), botón ▶ Conectar que lanza ARK contra el servidor, y estrella para fijar tus mapas arriba.
  *Steam Favorites: paste a server list in any format, review, and add to Steam favorites. Real names queried per server (A2S), a ▶ Connect button that launches ARK, and a star to pin your maps to the top.*
- **Perfiles INI**: versiones con nombre de `BaseDeviceProfiles.ini` con copia de seguridad automática al aplicar; captura el del juego, importa o edita en el editor integrado. Incluye 3 perfiles de la comunidad de serie.
  *INI Profiles: named versions of `BaseDeviceProfiles.ini` with automatic backups on apply; snapshot, import or edit in the built-in editor. Ships with 3 community profiles.*
- **Config del juego**: perfiles de `GameUserSettings.ini` (visual) e `Input.ini` (teclas) para guardar y compartir tu configuración completa.
  *Game config: profiles of `GameUserSettings.ini` (visuals) and `Input.ini` (keybindings).*
- **Actualizaciones automáticas** desde GitHub Releases, bandeja del sistema, acento de color elegible y UI en español e inglés.
  *Auto-updates from GitHub Releases, system tray, selectable accent color, Spanish and English UI.*

## Notas importantes / Important notes

- Steam debe estar **cerrado** al guardar favoritos (Steam sobrescribe la lista al salir). La app se ofrece a cerrarlo.
- La dirección de un servidor usa el **puerto de consulta** (normalmente 27015+), no el puerto de juego 7777.
- Steam restaura `BaseDeviceProfiles.ini` al verificar la integridad o actualizar el juego → botón **Reaplicar**.
- Datos de la app en `%APPDATA%\ARK-APP` (perfiles, copias de seguridad y ajustes).
- El `BaseDeviceProfiles.ini` del juego vive en `<ARK>\Engine\Config\`.

## Desarrollo / Development

- Kotlin + Compose Multiplatform for Desktop. JDK 21 (Temurin), Gradle wrapper.
- Ejecutar en desarrollo: `.\gradlew.bat run`
- Tests: `.\gradlew.bat test`
- Instalador MSI: `.\gradlew.bat packageMsi` (el plugin de Compose descarga WiX automáticamente)
  - Salida: `build\compose\binaries\main\msi\Prodigiosos App-<versión>.msi`
  - El MSI es per-user (sin UAC), con acceso directo en escritorio y menú Inicio, y JRE incluido.

## Ideas v2

- Ping A2S_INFO para comprobar que los servidores pegados responden.
- Exportar/importar perfiles como `.zip`.
