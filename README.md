# Prodigiosos App

App de escritorio para **ARK: Survival Evolved** (Windows). / Desktop app for **ARK: Survival Evolved** (Windows).

## Descarga / Download

**[Descargar la última versión (MSI) / Download the latest release](https://github.com/VhostLab/ARK-APP/releases/latest)**

Desde la v1.0.7 la app comprueba actualizaciones al arrancar y ofrece instalarlas con un clic.
*Since v1.0.7 the app checks for updates on startup and offers to install them in one click.*

## Funciones / Features

- **Favoritos de Steam**: pega una lista de servidores (aunque esté desordenada: nombres, comas, listas de Discord…), revisa la vista previa y añádelos a los favoritos del navegador de servidores de Steam para que aparezcan en el filtro "Favoritos" de ARK.
  *Steam Favorites: paste a messy server list, review the preview and add them to Steam's server browser favorites so they show up in ARK's "Favorites" filter.*
- **Perfiles INI**: guarda varias versiones de `BaseDeviceProfiles.ini` y alterna entre ellas con copia de seguridad automática. Puedes guardar el archivo actual del juego, importar un archivo descargado (con cualquier nombre) o crear/editar perfiles en un editor de texto integrado — al aplicar, siempre se copia al juego como `BaseDeviceProfiles.ini`.
  *INI Profiles: store multiple versions of `BaseDeviceProfiles.ini` and switch between them with automatic backups. Snapshot the game's current file, import a downloaded file (any name) or create/edit profiles in a built-in text editor — on apply it is always copied to the game as `BaseDeviceProfiles.ini`.*
- UI en español e inglés. / *Spanish and English UI.*

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
