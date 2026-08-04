package com.arkapp.i18n

import androidx.compose.runtime.compositionLocalOf

interface Strings {
    val appTitle: String get() = "Prodigiosos App"
    val tabFavorites: String
    val tabProfiles: String
    val tabGameConfig: String
    val tabSettings: String

    // Common
    val cancel: String
    val ok: String
    val back: String
    val next: String
    val close: String
    val save: String
    val errorAccessDenied: String
    fun errorGeneric(message: String): String

    // Updates
    fun updateAvailable(version: String): String
    val updateInstall: String
    val updateDownloading: String

    // First-run setup checklist
    val setupTitle: String
    val setupStepFavorites: String
    val setupStepProfile: String
    val setupGoFavorites: String
    val setupGoProfiles: String
    val setupAllDone: String

    // Tray
    val trayOpen: String
    val trayExit: String

    // Favorites
    val favAddServers: String
    val favCurrentTitle: String
    val favRefresh: String
    val favSelectAll: String
    val favConnect: String
    val favConnectLaunched: String
    val favConnectFailed: String
    fun favSaveNames(n: Int): String
    fun favNamesSaved(n: Int): String
    val favNoNameYet: String
    val favPin: String
    val favUnpin: String
    val favRemove: String
    fun favRemoveSelected(n: Int): String
    fun favRemoveConfirmTitle(n: Int): String
    val favRemoveConfirmBody: String
    fun favRemovedResult(n: Int): String
    val favEmptyTitle: String
    val favEmptyBody: String
    val favNoAccount: String

    // Favorites wizard
    val favWizTitle: String
    val wizPaste: String
    val wizReview: String
    val wizDone: String
    val favWizIntro: String
    val favPasteHint: String
    val favAnalyze: String
    fun favDetected(n: Int): String
    fun favIgnoredLines(n: Int): String
    val favNoServersFound: String
    val favDefaultPort: String
    val favDefaultPortNote: String
    fun favAddSelected(n: Int): String
    fun favAddedTitle(added: Int, skipped: Int): String
    val favDoneNote: String

    // Steam dialog
    val steamRunningTitle: String
    val steamRunningBody: String
    val steamCloseAndContinue: String
    val steamClosing: String
    val steamCloseTimeout: String

    // Profiles
    val profListTitle: String
    val profAddProfile: String
    val profWizTitle: String
    val profWizOrigin: String
    val profWizDetails: String
    val profWizIntro: String
    val profModeSnapshot: String
    val profModeSnapshotDesc: String
    val profModeImport: String
    val profModeImportDesc: String
    val profModeNew: String
    val profModeNewDesc: String
    val profFileLabel: String
    val profNameLabel: String
    val profNamePlaceholder: String
    val profHintSnapshot: String
    val profHintImport: String
    val profHintNew: String
    val profFinishImport: String
    val profFinishEditor: String
    fun profDoneImported(name: String): String
    fun profDoneSaved(name: String): String
    val profDoneNote: String
    val profEmptyTitle: String
    val profEmptyBody: String
    val profEdit: String
    val profApply: String
    val profReapply: String
    val profDelete: String
    val profActive: String
    val profActiveModified: String
    fun profCreatedOn(date: String): String
    fun profApplyConfirmTitle(name: String): String
    val profApplyBody: String
    val profArkRunningWarn: String
    fun profDeleteConfirmTitle(name: String): String
    val profDeleteBody: String
    val profDeleteConfirm: String
    fun profAppliedOk(name: String): String
    fun profSavedOk(name: String): String
    fun profDeletedOk(name: String): String
    val profRestoreBackup: String
    val profRestoredOk: String
    val profNoBackup: String
    val profArkNotFound: String
    val profFilesMissing: String
    val profEditorTitleNew: String
    val profEditorTitleEdit: String
    val profEditorContentPlaceholder: String

    // Game config (GameUserSettings.ini + Input.ini)
    val gcVisualTitle: String
    val gcKeysTitle: String
    fun gcWizIntro(file: String): String
    fun gcApplyBody(file: String): String
    fun gcEmptyBody(file: String): String
    fun gcFileMissing(file: String): String
    val gcModeSnapshot: String
    val gcModeSnapshotDesc: String

    // Settings
    val setLanguage: String
    val setAccent: String
    val setAccentHelp: String
    fun accentName(key: String): String
    val setSteamPath: String
    val setArkPath: String
    val setValid: String
    val setNotFound: String
    val setBrowse: String
    val setAutoDetect: String
    val setInvalidFolder: String
    val setWindowTitle: String
    val setWindowHelp: String
    val setWindowReset: String
    fun windowSizeName(key: String?): String
    val setShowSetupAgain: String
    val setShowSetupAgainHelp: String
    val setAccount: String
    val setAccountPinnedShort: String
    val setAccountAutoShort: String
    val setAccountPinned: String
    val setAccountAuto: String
    val firstRunTitle: String
    val firstRunBody: String
}

object EsStrings : Strings {
    override val tabFavorites = "Favoritos de Steam"
    override val tabProfiles = "Perfiles INI"
    override val tabGameConfig = "Config del juego"
    override val tabSettings = "Ajustes"

    override val cancel = "Cancelar"
    override val ok = "Aceptar"
    override val back = "← Atrás"
    override val next = "Siguiente →"
    override val close = "Cerrar"
    override val save = "Guardar"
    override val errorAccessDenied =
        "Acceso denegado al escribir. Cierra el juego y Steam, o ejecuta la app como administrador."
    override fun errorGeneric(message: String) = "Error: $message"

    override fun updateAvailable(version: String) = "Nueva versión $version disponible."
    override val updateInstall = "Descargar e instalar"
    override val updateDownloading = "Descargando…"

    override val setupTitle = "Puesta a punto"
    override val setupStepFavorites = "Servidores en favoritos"
    override val setupStepProfile = "Perfil INI aplicado"
    override val setupGoFavorites = "Ir a Favoritos"
    override val setupGoProfiles = "Ir a Perfiles INI"
    override val setupAllDone = "¡Todo listo! Inicia Steam y entra desde Ver → Servidores → Favoritos."

    override val trayOpen = "Abrir"
    override val trayExit = "Salir"

    override val favAddServers = "Añadir servidores"
    override val favCurrentTitle = "Favoritos actuales"
    override val favRefresh = "Refrescar"
    override val favSelectAll = "Seleccionar todo"
    override val favConnect = "Conectar"
    override val favConnectLaunched = "Abriendo Steam para conectar al servidor…"
    override val favConnectFailed = "No se ha podido lanzar Steam. Revisa la carpeta de Steam en Ajustes."
    override fun favSaveNames(n: Int) =
        if (n == 1) "Guardar 1 nombre en Steam" else "Guardar $n nombres en Steam"
    override fun favNamesSaved(n: Int) =
        if (n == 1) "1 nombre guardado en favoritos." else "$n nombres guardados en favoritos."
    override val favNoNameYet = "Sin nombre todavía — se consultará al servidor"
    override val favPin = "Fijar arriba"
    override val favUnpin = "Quitar de fijados"
    override val favRemove = "Eliminar"
    override fun favRemoveSelected(n: Int) = "Eliminar seleccionados ($n)"
    override fun favRemoveConfirmTitle(n: Int) =
        if (n == 1) "¿Eliminar 1 servidor de favoritos?" else "¿Eliminar $n servidores de favoritos?"
    override val favRemoveConfirmBody =
        "Se quitarán de tus favoritos de Steam. Podrás volver a añadirlos cuando quieras."
    override fun favRemovedResult(n: Int) = if (n == 1) "1 favorito eliminado." else "$n favoritos eliminados."
    override val favEmptyTitle = "Aún no hay favoritos"
    override val favEmptyBody =
        "Aquí verás los servidores de la comunidad guardados en tus favoritos de Steam. Pulsa «Añadir servidores» y pega la lista para empezar."
    override val favNoAccount = "No se ha podido determinar la cuenta de Steam. Revísala en la barra lateral."

    override val favWizTitle = "Añadir servidores a favoritos"
    override val wizPaste = "Pegar"
    override val wizReview = "Revisar"
    override val wizDone = "Listo"
    override val favWizIntro =
        "Pega la lista de servidores tal cual la tengas — mensajes de Discord, tablas de webs o texto suelto. La app se encarga del resto."
    override val favPasteHint = "Pega aquí tu lista de servidores…"
    override val favAnalyze = "Detectar servidores"
    override fun favDetected(n: Int) = if (n == 1) "1 servidor detectado" else "$n servidores detectados"
    override fun favIgnoredLines(n: Int) =
        if (n == 1) "1 línea no se ha reconocido y se ignorará." else "$n líneas no se han reconocido y se ignorarán."
    override val favNoServersFound = "No se ha reconocido ningún servidor en el texto pegado."
    override val favDefaultPort = "Puerto por defecto"
    override val favDefaultPortNote = "Se aplicará a los que no lo indiquen."
    override fun favAddSelected(n: Int) = "Añadir $n a favoritos"
    override fun favAddedTitle(added: Int, skipped: Int) =
        (if (added == 1) "1 servidor añadido a favoritos" else "$added servidores añadidos a favoritos") +
            (if (skipped > 0) " ($skipped duplicados omitidos)" else "")
    override val favDoneNote =
        "Inicia Steam y entra desde Ver → Servidores → Favoritos. Los nombres que falten se rellenarán solos al consultar cada servidor."

    override val steamRunningTitle = "Steam está abierto"
    override val steamRunningBody =
        "Para guardar los favoritos hay que cerrar Steam un momento. Se volverá a abrir al terminar."
    override val steamCloseAndContinue = "Cerrar Steam y continuar"
    override val steamClosing = "Cerrando Steam…"
    override val steamCloseTimeout = "Steam no se ha cerrado a tiempo. Ciérralo manualmente e inténtalo de nuevo."

    override val profListTitle = "Perfiles guardados"
    override val profAddProfile = "Añadir perfil"
    override val profWizTitle = "Añadir perfil INI"
    override val profWizOrigin = "Origen"
    override val profWizDetails = "Detalles"
    override val profWizIntro =
        "Un perfil es una copia con nombre del archivo BaseDeviceProfiles.ini del juego. ¿De dónde sale este perfil?"
    override val profModeSnapshot = "Del juego actual"
    override val profModeSnapshotDesc = "Guarda el archivo que ARK usa ahora mismo."
    override val profModeImport = "Importar archivo"
    override val profModeImportDesc = "Trae un .ini que te hayan pasado."
    override val profModeNew = "Crear desde cero"
    override val profModeNewDesc = "Empieza con un archivo vacío y edítalo."
    override val profFileLabel = "Archivo"
    override val profNameLabel = "Nombre del perfil"
    override val profNamePlaceholder = "p. ej. «Gráficos altos»"
    override val profHintSnapshot = "Se guardará una copia del archivo actual del juego; el original no se toca."
    override val profHintImport = "El archivo se copiará; el original no se toca."
    override val profHintNew = "Al terminar se abrirá el editor para escribir el contenido."
    override val profFinishImport = "Importar"
    override val profFinishEditor = "Abrir el editor"
    override fun profDoneImported(name: String) = "Perfil «$name» importado"
    override fun profDoneSaved(name: String) = "Perfil «$name» guardado"
    override val profDoneNote =
        "Ya aparece en «Perfiles guardados». Aplícalo cuando quieras — antes de sobrescribir se guarda una copia de seguridad automática."
    override val profEmptyTitle = "Aún no hay perfiles"
    override val profEmptyBody =
        "Guarda el archivo actual del juego con un nombre, importa uno de un archivo o crea uno desde cero. Después podrás aplicarlos con un clic."
    override val profEdit = "Editar"
    override val profApply = "Aplicar"
    override val profReapply = "Reaplicar"
    override val profDelete = "Borrar"
    override val profActive = "Activo"
    override val profActiveModified = "Activo (modificado)"
    override fun profCreatedOn(date: String) = "Creado el $date"
    override fun profApplyConfirmTitle(name: String) = "¿Aplicar «$name»?"
    override val profApplyBody =
        "Se sobrescribirá el BaseDeviceProfiles.ini actual del juego. Antes se guardará una copia de seguridad automática."
    override val profArkRunningWarn =
        "ARK está en ejecución. Cierra el juego antes de aplicar — al salir, ARK sobrescribe el archivo y se perderían los cambios."
    override fun profDeleteConfirmTitle(name: String) = "¿Eliminar el perfil «$name»?"
    override val profDeleteBody = "Esta acción no se puede deshacer. El archivo del juego no se toca."
    override val profDeleteConfirm = "Eliminar"
    override fun profAppliedOk(name: String) = "Perfil «$name» aplicado. Copia de seguridad guardada."
    override fun profSavedOk(name: String) = "Perfil «$name» guardado."
    override fun profDeletedOk(name: String) = "Perfil «$name» eliminado."
    override val profRestoreBackup = "Restaurar última copia de seguridad"
    override val profRestoredOk = "Copia de seguridad restaurada en el archivo del juego."
    override val profNoBackup = "No hay copias de seguridad."
    override val profArkNotFound = "No se ha encontrado la instalación de ARK. Configúrala en Ajustes."
    override val profFilesMissing = "No se ha encontrado el BaseDeviceProfiles.ini del juego."
    override val profEditorTitleNew = "Nuevo perfil"
    override val profEditorTitleEdit = "Editar perfil"
    override val profEditorContentPlaceholder = "Contenido del archivo…"

    override val gcVisualTitle = "Ajustes visuales"
    override val gcKeysTitle = "Teclas y controles"
    override fun gcWizIntro(file: String) =
        "Un perfil es una copia con nombre del archivo $file del juego. ¿De dónde sale este perfil?"
    override fun gcApplyBody(file: String) =
        "Se sobrescribirá el $file actual del juego. Antes se guardará una copia de seguridad automática."
    override fun gcEmptyBody(file: String) =
        "Guarda la configuración actual del juego con un nombre o importa un $file que te hayan pasado. Después podrás aplicarlos con un clic."
    override fun gcFileMissing(file: String) =
        "No se ha encontrado el $file del juego. Arranca ARK al menos una vez para que se cree."
    override val gcModeSnapshot = "Del juego actual"
    override val gcModeSnapshotDesc = "Guarda la configuración que ARK usa ahora mismo."

    override val setLanguage = "Idioma"
    override val setAccent = "Color de la app"
    override val setAccentHelp = "Se aplica a botones, pestañas y acentos."
    override fun accentName(key: String) = when (key) {
        "morado" -> "Morado"
        "azul" -> "Azul"
        "verde" -> "Verde"
        "amarillo" -> "Amarillo"
        else -> "Rojo"
    }
    override val setSteamPath = "Carpeta de Steam"
    override val setArkPath = "Carpeta de ARK"
    override val setValid = "Válida"
    override val setNotFound = "No encontrada"
    override val setBrowse = "Examinar…"
    override val setAutoDetect = "Detectar automáticamente"
    override val setInvalidFolder = "La carpeta seleccionada no parece válida."
    override val setWindowTitle = "Ventana"
    override val setWindowHelp = "Elige un tamaño fijo, o deja que la app recuerde el tamaño tal y como la dejes."
    override val setWindowReset = "Restablecer tamaño por defecto"
    override fun windowSizeName(key: String?) = when (key) {
        "s" -> "Pequeña (1280×800)"
        "m" -> "Mediana (1600×950)"
        "l" -> "Grande (1920×1080)"
        "full" -> "Casi completa"
        else -> "Recordar la última"
    }
    override val setShowSetupAgain = "Volver a mostrar la guía"
    override val setShowSetupAgainHelp = "La guía de los primeros pasos solo aparece la primera vez que abres la app."
    override val setAccount = "Cuenta de Steam"
    override val setAccountPinnedShort = "fijada"
    override val setAccountAutoShort = "auto"
    override val setAccountPinned = "Cuenta fijada — se usará siempre esta."
    override val setAccountAuto = "Detección automática (última sesión de Steam)."
    override val firstRunTitle = "Configuración inicial"
    override val firstRunBody = "No se ha podido localizar la instalación de ARK automáticamente. Selecciona la carpeta manualmente."
}

object EnStrings : Strings {
    override val tabFavorites = "Steam Favorites"
    override val tabProfiles = "INI Profiles"
    override val tabGameConfig = "Game config"
    override val tabSettings = "Settings"

    override val cancel = "Cancel"
    override val ok = "OK"
    override val back = "← Back"
    override val next = "Next →"
    override val close = "Close"
    override val save = "Save"
    override val errorAccessDenied =
        "Access denied while writing. Close the game and Steam, or run the app as administrator."
    override fun errorGeneric(message: String) = "Error: $message"

    override fun updateAvailable(version: String) = "New version $version available."
    override val updateInstall = "Download and install"
    override val updateDownloading = "Downloading…"

    override val setupTitle = "Getting set up"
    override val setupStepFavorites = "Servers in favorites"
    override val setupStepProfile = "INI profile applied"
    override val setupGoFavorites = "Go to Favorites"
    override val setupGoProfiles = "Go to INI Profiles"
    override val setupAllDone = "All set! Start Steam and join from View → Game Servers → Favorites."

    override val trayOpen = "Open"
    override val trayExit = "Quit"

    override val favAddServers = "Add servers"
    override val favCurrentTitle = "Current favorites"
    override val favRefresh = "Refresh"
    override val favSelectAll = "Select all"
    override val favConnect = "Connect"
    override val favConnectLaunched = "Opening Steam to connect to the server…"
    override val favConnectFailed = "Could not launch Steam. Check the Steam folder in Settings."
    override fun favSaveNames(n: Int) =
        if (n == 1) "Save 1 name to Steam" else "Save $n names to Steam"
    override fun favNamesSaved(n: Int) =
        if (n == 1) "1 name saved to favorites." else "$n names saved to favorites."
    override val favNoNameYet = "No name yet — the server will be queried"
    override val favPin = "Pin to top"
    override val favUnpin = "Unpin"
    override val favRemove = "Remove"
    override fun favRemoveSelected(n: Int) = "Remove selected ($n)"
    override fun favRemoveConfirmTitle(n: Int) =
        if (n == 1) "Remove 1 server from favorites?" else "Remove $n servers from favorites?"
    override val favRemoveConfirmBody =
        "They will be removed from your Steam favorites. You can add them again anytime."
    override fun favRemovedResult(n: Int) = if (n == 1) "1 favorite removed." else "$n favorites removed."
    override val favEmptyTitle = "No favorites yet"
    override val favEmptyBody =
        "The community servers saved to your Steam favorites will show up here. Press \"Add servers\" and paste the list to get started."
    override val favNoAccount = "Could not determine the Steam account. Check it in the sidebar."

    override val favWizTitle = "Add servers to favorites"
    override val wizPaste = "Paste"
    override val wizReview = "Review"
    override val wizDone = "Done"
    override val favWizIntro =
        "Paste the server list exactly as you have it — Discord messages, web tables or loose text. The app takes care of the rest."
    override val favPasteHint = "Paste your server list here…"
    override val favAnalyze = "Detect servers"
    override fun favDetected(n: Int) = if (n == 1) "1 server detected" else "$n servers detected"
    override fun favIgnoredLines(n: Int) =
        if (n == 1) "1 line was not recognized and will be ignored." else "$n lines were not recognized and will be ignored."
    override val favNoServersFound = "No servers were recognized in the pasted text."
    override val favDefaultPort = "Default port"
    override val favDefaultPortNote = "Applied to the ones that don't specify it."
    override fun favAddSelected(n: Int) = "Add $n to favorites"
    override fun favAddedTitle(added: Int, skipped: Int) =
        (if (added == 1) "1 server added to favorites" else "$added servers added to favorites") +
            (if (skipped > 0) " ($skipped duplicates skipped)" else "")
    override val favDoneNote =
        "Start Steam and join from View → Game Servers → Favorites. Missing names will fill in on their own by querying each server."

    override val steamRunningTitle = "Steam is running"
    override val steamRunningBody =
        "Steam needs to close for a moment to save the favorites. It will reopen when done."
    override val steamCloseAndContinue = "Close Steam and continue"
    override val steamClosing = "Closing Steam…"
    override val steamCloseTimeout = "Steam did not close in time. Close it manually and try again."

    override val profListTitle = "Saved profiles"
    override val profAddProfile = "Add profile"
    override val profWizTitle = "Add INI profile"
    override val profWizOrigin = "Source"
    override val profWizDetails = "Details"
    override val profWizIntro =
        "A profile is a named copy of the game's BaseDeviceProfiles.ini file. Where does this profile come from?"
    override val profModeSnapshot = "From the current game"
    override val profModeSnapshotDesc = "Saves the file ARK is using right now."
    override val profModeImport = "Import a file"
    override val profModeImportDesc = "Bring in a .ini someone sent you."
    override val profModeNew = "Create from scratch"
    override val profModeNewDesc = "Start with an empty file and edit it."
    override val profFileLabel = "File"
    override val profNameLabel = "Profile name"
    override val profNamePlaceholder = "e.g. \"High graphics\""
    override val profHintSnapshot = "A copy of the game's current file will be saved; the original is untouched."
    override val profHintImport = "The file will be copied; the original is untouched."
    override val profHintNew = "The editor will open so you can write the content."
    override val profFinishImport = "Import"
    override val profFinishEditor = "Open the editor"
    override fun profDoneImported(name: String) = "Profile \"$name\" imported"
    override fun profDoneSaved(name: String) = "Profile \"$name\" saved"
    override val profDoneNote =
        "It now shows under \"Saved profiles\". Apply it whenever you want — an automatic backup is saved before overwriting."
    override val profEmptyTitle = "No profiles yet"
    override val profEmptyBody =
        "Save the game's current file under a name, import one from a file or create one from scratch. Then apply them with one click."
    override val profEdit = "Edit"
    override val profApply = "Apply"
    override val profReapply = "Reapply"
    override val profDelete = "Delete"
    override val profActive = "Active"
    override val profActiveModified = "Active (modified)"
    override fun profCreatedOn(date: String) = "Created on $date"
    override fun profApplyConfirmTitle(name: String) = "Apply \"$name\"?"
    override val profApplyBody =
        "The game's current BaseDeviceProfiles.ini will be overwritten. An automatic backup is saved first."
    override val profArkRunningWarn =
        "ARK is running. Close the game before applying — on exit, ARK rewrites the file and the changes would be lost."
    override fun profDeleteConfirmTitle(name: String) = "Delete profile \"$name\"?"
    override val profDeleteBody = "This cannot be undone. The game's file is not touched."
    override val profDeleteConfirm = "Delete"
    override fun profAppliedOk(name: String) = "Profile \"$name\" applied. Backup saved."
    override fun profSavedOk(name: String) = "Profile \"$name\" saved."
    override fun profDeletedOk(name: String) = "Profile \"$name\" deleted."
    override val profRestoreBackup = "Restore last backup"
    override val profRestoredOk = "Backup restored to the game's file."
    override val profNoBackup = "No backups available."
    override val profArkNotFound = "ARK installation not found. Configure it in Settings."
    override val profFilesMissing = "The game's BaseDeviceProfiles.ini was not found."
    override val profEditorTitleNew = "New profile"
    override val profEditorTitleEdit = "Edit profile"
    override val profEditorContentPlaceholder = "File content…"

    override val gcVisualTitle = "Visual settings"
    override val gcKeysTitle = "Keys and controls"
    override fun gcWizIntro(file: String) =
        "A profile is a named copy of the game's $file file. Where does this profile come from?"
    override fun gcApplyBody(file: String) =
        "The game's current $file will be overwritten. An automatic backup is saved first."
    override fun gcEmptyBody(file: String) =
        "Save the game's current configuration under a name or import a $file someone sent you. Then apply them with one click."
    override fun gcFileMissing(file: String) =
        "The game's $file was not found. Launch ARK at least once so it gets created."
    override val gcModeSnapshot = "From the current game"
    override val gcModeSnapshotDesc = "Saves the configuration ARK is using right now."

    override val setLanguage = "Language"
    override val setAccent = "App color"
    override val setAccentHelp = "Applied to buttons, tabs and accents."
    override fun accentName(key: String) = when (key) {
        "morado" -> "Purple"
        "azul" -> "Blue"
        "verde" -> "Green"
        "amarillo" -> "Yellow"
        else -> "Red"
    }
    override val setSteamPath = "Steam folder"
    override val setArkPath = "ARK folder"
    override val setValid = "Valid"
    override val setNotFound = "Not found"
    override val setBrowse = "Browse…"
    override val setAutoDetect = "Detect automatically"
    override val setInvalidFolder = "The selected folder does not look valid."
    override val setWindowTitle = "Window"
    override val setWindowHelp = "Pick a fixed size, or let the app remember the size exactly as you leave it."
    override val setWindowReset = "Reset to default size"
    override fun windowSizeName(key: String?) = when (key) {
        "s" -> "Small (1280×800)"
        "m" -> "Medium (1600×950)"
        "l" -> "Large (1920×1080)"
        "full" -> "Near fullscreen"
        else -> "Remember last"
    }
    override val setShowSetupAgain = "Show the guide again"
    override val setShowSetupAgainHelp = "The first-steps guide only shows the first time you open the app."
    override val setAccount = "Steam account"
    override val setAccountPinnedShort = "pinned"
    override val setAccountAutoShort = "auto"
    override val setAccountPinned = "Account pinned — this one will always be used."
    override val setAccountAuto = "Auto-detected (last Steam session)."
    override val firstRunTitle = "Initial setup"
    override val firstRunBody = "The ARK installation could not be located automatically. Select the folder manually."
}

val LocalStrings = compositionLocalOf<Strings> { EsStrings }
