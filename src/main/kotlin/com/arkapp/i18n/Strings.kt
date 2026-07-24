package com.arkapp.i18n

import androidx.compose.runtime.compositionLocalOf

interface Strings {
    val appTitle: String get() = "Prodigiosos App"
    val tabFavorites: String
    val tabProfiles: String
    val tabSettings: String

    // Favorites
    val favPasteTitle: String
    val favPasteHint: String
    val favQueryPortHint: String
    val favAnalyze: String
    fun favDetected(n: Int): String
    fun favIgnoredLines(n: Int): String
    val favNoServersFound: String
    val favMissingPort: String
    val favDefaultPort: String
    fun favAddSelected(n: Int): String
    val favCurrentTitle: String
    val favRefresh: String
    val favRemove: String
    val favEmpty: String
    val favNoAccount: String
    val favSelectAll: String
    fun favRemoveSelected(n: Int): String
    val favRemoveConfirmTitle: String
    fun favRemoveConfirmBody(n: Int): String
    fun favAddedResult(added: Int, skipped: Int): String
    fun favRemovedResult(n: Int): String
    val steamRunningTitle: String
    val steamRunningBody: String
    val steamCloseAndContinue: String
    val steamClosingWait: String
    val steamCloseTimeout: String
    val steamStartReminder: String

    // Profiles
    val profNewTitle: String
    val profNewHelp: String
    val profSaveCurrent: String
    val profImport: String
    val profCreateNew: String
    val profEdit: String
    val profEditorTitle: String
    val profEditorContentPlaceholder: String
    val profNamePlaceholder: String
    val profSave: String
    val profListTitle: String
    val profEmpty: String
    val profApply: String
    val profReapply: String
    val profDelete: String
    val profActive: String
    val profActiveModified: String
    val profApplyConfirmTitle: String
    fun profApplyConfirmBody(name: String): String
    val profDeleteConfirmTitle: String
    fun profDeleteConfirmBody(name: String): String
    val profArkRunning: String
    val profIntegrityWarning: String
    fun profAppliedOk(name: String): String
    fun profSavedOk(name: String): String
    fun profDeletedOk(name: String): String
    val profRestoreBackup: String
    val profRestoredOk: String
    val profNoBackup: String
    val profArkNotFound: String
    val profFilesMissing: String

    // Settings
    val setLanguage: String
    val setSteamPath: String
    val setArkPath: String
    val setAccount: String
    val setAccountHelp: String
    val setBrowse: String
    val setAutoDetect: String
    val setDetectedOk: String
    val setNotFound: String
    val setInvalidFolder: String
    val setSteamFolderHelp: String
    val setArkFolderHelp: String
    val firstRunTitle: String
    val firstRunBody: String

    // Common
    val cancel: String
    val ok: String
    val errorAccessDenied: String
    fun errorGeneric(message: String): String
}

object EsStrings : Strings {
    override val tabFavorites = "Favoritos de Steam"
    override val tabProfiles = "Perfiles INI"
    override val tabSettings = "Ajustes"

    override val favPasteTitle = "Añadir servidores"
    override val favPasteHint = "Pega aquí tu lista de servidores (vale texto desordenado: nombres, comas, líneas de Discord…)"
    override val favQueryPortHint = "Usa el puerto de consulta (query port), normalmente 27015 o superior — no el puerto de juego 7777."
    override val favAnalyze = "Detectar servidores"
    override fun favDetected(n: Int) = if (n == 1) "1 servidor detectado" else "$n servidores detectados"
    override fun favIgnoredLines(n: Int) = if (n == 1) "1 línea sin servidor reconocible" else "$n líneas sin servidor reconocible"
    override val favNoServersFound = "No se ha detectado ningún servidor en el texto."
    override val favMissingPort = "sin puerto"
    override val favDefaultPort = "Puerto por defecto"
    override fun favAddSelected(n: Int) = "Añadir $n a favoritos"
    override val favCurrentTitle = "Favoritos actuales"
    override val favRefresh = "Actualizar"
    override val favRemove = "Eliminar"
    override val favEmpty = "No hay favoritos guardados."
    override val favNoAccount = "No se ha podido determinar la cuenta de Steam. Revísala en Ajustes."
    override val favSelectAll = "Seleccionar todo"
    override fun favRemoveSelected(n: Int) = "Eliminar seleccionados ($n)"
    override val favRemoveConfirmTitle = "Eliminar favoritos"
    override fun favRemoveConfirmBody(n: Int) =
        if (n == 1) "¿Eliminar 1 servidor de favoritos?" else "¿Eliminar $n servidores de favoritos?"
    override fun favAddedResult(added: Int, skipped: Int) =
        "Añadidos $added servidores" + (if (skipped > 0) " ($skipped duplicados omitidos)." else ".")
    override fun favRemovedResult(n: Int) = if (n == 1) "1 favorito eliminado." else "$n favoritos eliminados."
    override val steamRunningTitle = "Steam está abierto"
    override val steamRunningBody =
        "Steam sobrescribe la lista de favoritos al cerrarse, así que hay que cerrarlo antes de guardar los cambios."
    override val steamCloseAndContinue = "Cerrar Steam y continuar"
    override val steamClosingWait = "Cerrando Steam… puedes cancelar si tarda demasiado."
    override val steamCloseTimeout = "Steam no se ha cerrado a tiempo. Ciérralo manualmente e inténtalo de nuevo."
    override val steamStartReminder = "Hecho. Inicia Steam y comprueba Ver → Servidores → Favoritos."

    override val profNewTitle = "Nuevo perfil"
    override val profNewHelp =
        "Guarda el archivo actual del juego con un nombre, crea uno desde cero o importa un archivo descargado. Se guardará siempre como BaseDeviceProfiles.ini."
    override val profSaveCurrent = "Guardar el actual"
    override val profImport = "Importar archivo…"
    override val profCreateNew = "Crear nuevo"
    override val profEdit = "Editar"
    override val profEditorTitle = "Editor de BaseDeviceProfiles.ini"
    override val profEditorContentPlaceholder = "Contenido del archivo…"
    override val profNamePlaceholder = "Nombre del perfil"
    override val profSave = "Guardar"
    override val profListTitle = "Perfiles guardados"
    override val profEmpty = "No hay perfiles todavía. Guarda el actual, crea uno nuevo o importa un archivo."
    override val profApply = "Aplicar"
    override val profReapply = "Reaplicar"
    override val profDelete = "Eliminar"
    override val profActive = "Activo"
    override val profActiveModified = "Activo (modificado)"
    override val profApplyConfirmTitle = "Aplicar perfil"
    override fun profApplyConfirmBody(name: String) =
        "Se sobrescribirá el BaseDeviceProfiles.ini del juego con el del perfil \"$name\". Antes se hará una copia de seguridad del actual."
    override val profDeleteConfirmTitle = "Eliminar perfil"
    override fun profDeleteConfirmBody(name: String) = "¿Eliminar el perfil \"$name\"? Esta acción no se puede deshacer."
    override val profArkRunning = "ARK está en ejecución. Ciérralo antes de aplicar un perfil (el juego sobrescribe sus ini al salir)."
    override val profIntegrityWarning =
        "Steam restaura BaseDeviceProfiles.ini al verificar archivos o al actualizar el juego. Después de eso, pulsa \"Reaplicar\" en tu perfil activo."
    override fun profAppliedOk(name: String) = "Perfil \"$name\" aplicado. Copia de seguridad creada."
    override fun profSavedOk(name: String) = "Perfil \"$name\" guardado."
    override fun profDeletedOk(name: String) = "Perfil \"$name\" eliminado."
    override val profRestoreBackup = "Restaurar última copia de seguridad"
    override val profRestoredOk = "Copia de seguridad restaurada."
    override val profNoBackup = "No hay copias de seguridad."
    override val profArkNotFound = "No se ha encontrado la instalación de ARK. Configúrala en Ajustes."
    override val profFilesMissing = "No se ha encontrado el BaseDeviceProfiles.ini del juego."

    override val setLanguage = "Idioma"
    override val setSteamPath = "Carpeta de Steam"
    override val setArkPath = "Carpeta de ARK"
    override val setAccount = "Cuenta de Steam (accountid)"
    override val setAccountHelp = "Se detecta automáticamente desde la última sesión de Steam. Cámbiala solo si usas varias cuentas."
    override val setBrowse = "Examinar…"
    override val setAutoDetect = "Detectar automáticamente"
    override val setDetectedOk = "Detectado"
    override val setNotFound = "No encontrado"
    override val setInvalidFolder = "La carpeta seleccionada no parece válida."
    override val setSteamFolderHelp = "La carpeta que contiene steam.exe."
    override val setArkFolderHelp = "La carpeta de instalación de ARK (la que contiene \"ShooterGame\")."
    override val firstRunTitle = "Configuración inicial"
    override val firstRunBody = "No se ha podido localizar la instalación de ARK automáticamente. Selecciona la carpeta manualmente."

    override val cancel = "Cancelar"
    override val ok = "Aceptar"
    override val errorAccessDenied =
        "Acceso denegado al escribir. Cierra el juego y Steam, o ejecuta ARK-APP como administrador."
    override fun errorGeneric(message: String) = "Error: $message"
}

object EnStrings : Strings {
    override val tabFavorites = "Steam Favorites"
    override val tabProfiles = "INI Profiles"
    override val tabSettings = "Settings"

    override val favPasteTitle = "Add servers"
    override val favPasteHint = "Paste your server list here (messy text is fine: names, commas, Discord lines…)"
    override val favQueryPortHint = "Use the query port (usually 27015 or higher) — not the game port 7777."
    override val favAnalyze = "Detect servers"
    override fun favDetected(n: Int) = if (n == 1) "1 server detected" else "$n servers detected"
    override fun favIgnoredLines(n: Int) = if (n == 1) "1 line with no recognizable server" else "$n lines with no recognizable server"
    override val favNoServersFound = "No servers were detected in the text."
    override val favMissingPort = "no port"
    override val favDefaultPort = "Default port"
    override fun favAddSelected(n: Int) = "Add $n to favorites"
    override val favCurrentTitle = "Current favorites"
    override val favRefresh = "Refresh"
    override val favRemove = "Remove"
    override val favEmpty = "No favorites saved."
    override val favNoAccount = "Could not determine the Steam account. Check it in Settings."
    override val favSelectAll = "Select all"
    override fun favRemoveSelected(n: Int) = "Remove selected ($n)"
    override val favRemoveConfirmTitle = "Remove favorites"
    override fun favRemoveConfirmBody(n: Int) =
        if (n == 1) "Remove 1 server from favorites?" else "Remove $n servers from favorites?"
    override fun favAddedResult(added: Int, skipped: Int) =
        "Added $added servers" + (if (skipped > 0) " ($skipped duplicates skipped)." else ".")
    override fun favRemovedResult(n: Int) = if (n == 1) "1 favorite removed." else "$n favorites removed."
    override val steamRunningTitle = "Steam is running"
    override val steamRunningBody =
        "Steam overwrites the favorites list when it exits, so it must be closed before saving changes."
    override val steamCloseAndContinue = "Close Steam and continue"
    override val steamClosingWait = "Closing Steam… you can cancel if it takes too long."
    override val steamCloseTimeout = "Steam did not close in time. Close it manually and try again."
    override val steamStartReminder = "Done. Start Steam and check View → Game Servers → Favorites."

    override val profNewTitle = "New profile"
    override val profNewHelp =
        "Save the game's current file under a name, create one from scratch or import a downloaded file. It is always stored as BaseDeviceProfiles.ini."
    override val profSaveCurrent = "Save current"
    override val profImport = "Import file…"
    override val profCreateNew = "Create new"
    override val profEdit = "Edit"
    override val profEditorTitle = "BaseDeviceProfiles.ini editor"
    override val profEditorContentPlaceholder = "File content…"
    override val profNamePlaceholder = "Profile name"
    override val profSave = "Save"
    override val profListTitle = "Saved profiles"
    override val profEmpty = "No profiles yet. Save the current one, create a new one or import a file."
    override val profApply = "Apply"
    override val profReapply = "Reapply"
    override val profDelete = "Delete"
    override val profActive = "Active"
    override val profActiveModified = "Active (modified)"
    override val profApplyConfirmTitle = "Apply profile"
    override fun profApplyConfirmBody(name: String) =
        "The game's BaseDeviceProfiles.ini will be overwritten with the one from profile \"$name\". A backup of the current file will be made first."
    override val profDeleteConfirmTitle = "Delete profile"
    override fun profDeleteConfirmBody(name: String) = "Delete profile \"$name\"? This cannot be undone."
    override val profArkRunning = "ARK is running. Close it before applying a profile (the game rewrites its ini files on exit)."
    override val profIntegrityWarning =
        "Steam restores BaseDeviceProfiles.ini when verifying files or updating the game. After that, press \"Reapply\" on your active profile."
    override fun profAppliedOk(name: String) = "Profile \"$name\" applied. Backup created."
    override fun profSavedOk(name: String) = "Profile \"$name\" saved."
    override fun profDeletedOk(name: String) = "Profile \"$name\" deleted."
    override val profRestoreBackup = "Restore last backup"
    override val profRestoredOk = "Backup restored."
    override val profNoBackup = "No backups available."
    override val profArkNotFound = "ARK installation not found. Configure it in Settings."
    override val profFilesMissing = "The game's BaseDeviceProfiles.ini was not found."

    override val setLanguage = "Language"
    override val setSteamPath = "Steam folder"
    override val setArkPath = "ARK folder"
    override val setAccount = "Steam account (accountid)"
    override val setAccountHelp = "Auto-detected from the last Steam session. Change it only if you use multiple accounts."
    override val setBrowse = "Browse…"
    override val setAutoDetect = "Auto-detect"
    override val setDetectedOk = "Detected"
    override val setNotFound = "Not found"
    override val setInvalidFolder = "The selected folder does not look valid."
    override val setSteamFolderHelp = "The folder that contains steam.exe."
    override val setArkFolderHelp = "The ARK install folder (the one containing \"ShooterGame\")."
    override val firstRunTitle = "Initial setup"
    override val firstRunBody = "The ARK installation could not be located automatically. Select the folder manually."

    override val cancel = "Cancel"
    override val ok = "OK"
    override val errorAccessDenied =
        "Access denied while writing. Close the game and Steam, or run ARK-APP as administrator."
    override fun errorGeneric(message: String) = "Error: $message"
}

val LocalStrings = compositionLocalOf<Strings> { EsStrings }
