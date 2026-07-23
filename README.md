# ARK-APP

App de escritorio para **ARK: Survival Evolved** (Windows). / Desktop app for **ARK: Survival Evolved** (Windows).

## Funciones / Features

- **Favoritos de Steam**: pega una lista de servidores (aunque esté desordenada: nombres, comas, listas de Discord…), revisa la vista previa y añádelos a los favoritos del navegador de servidores de Steam para que aparezcan en el filtro "Favoritos" de ARK.
  *Steam Favorites: paste a messy server list, review the preview and add them to Steam's server browser favorites so they show up in ARK's "Favorites" filter.*
- **Perfiles INI**: guarda varias versiones de `GameUserSettings.ini` + `BaseDeviceProfiles.ini` y alterna entre ellas con copia de seguridad automática.
  *INI Profiles: store multiple versions of both files and switch between them with automatic backups.*
- UI en español e inglés. / *Spanish and English UI.*

## Notas importantes / Important notes

- Steam debe estar **cerrado** al guardar favoritos (Steam sobrescribe la lista al salir). La app se ofrece a cerrarlo.
- La dirección de un servidor usa el **puerto de consulta** (normalmente 27015+), no el puerto de juego 7777.
- Steam restaura `BaseDeviceProfiles.ini` al verificar la integridad o actualizar el juego → botón **Reaplicar**.
- Datos de la app en `%APPDATA%\ARK-APP` (perfiles, copias de seguridad y ajustes).

## Desarrollo / Development

- Kotlin + Compose Multiplatform for Desktop. JDK 21 (Temurin), Gradle wrapper.
- Ejecutar en desarrollo: `.\gradlew.bat run`
- Tests: `.\gradlew.bat test`
- Instalador MSI: `.\gradlew.bat packageMsi` (requiere los binarios de WiX 3.14 en `tools\wix314`, ver abajo)
  - Salida: `build\compose\binaries\main\msi\ARK-APP-<versión>.msi`

### WiX (solo para empaquetar)

jpackage necesita `candle.exe`/`light.exe` de WiX 3.14 en el `PATH`. Descarga
[wix314-binaries.zip](https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm) y extráelo en `tools\wix314`, o instala WiX Toolset 3.14 en el sistema.

## Ideas v2

- Ping A2S_INFO para comprobar que los servidores pegados responden.
- Exportar/importar perfiles como `.zip`.
