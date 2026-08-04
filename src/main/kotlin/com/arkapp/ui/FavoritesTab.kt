package com.arkapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import com.arkapp.steam.FavoritesRepository
import com.arkapp.steam.ServerListParser
import com.arkapp.steam.ServerParseResult
import com.arkapp.steam.ServerQuery
import com.arkapp.steam.SteamProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.AccessDeniedException

@Composable
fun FavoritesTab(state: AppState) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by state.settings.state.collectAsState()

    val accountId = remember(settings) { state.steamLocator.accountId() }
    val accounts = remember(settings) { state.steamLocator.availableAccountIds() }
    val accountNames = remember(settings) { state.steamLocator.accountNames() }

    var pasteText by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<ServerParseResult?>(null) }
    var checked by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var defaultPortText by remember { mutableStateOf("27015") }
    var favoritesList by remember { mutableStateOf<List<FavoritesRepository.Favorite>>(emptyList()) }
    var selectedFavs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var steamDialogAction by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    var steamWaitJob by remember { mutableStateOf<Job?>(null) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    // A2S_INFO-resolved names: by preview index for detected servers, by entryKey for saved favorites
    var previewNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var liveNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun refreshFavorites() {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { state.favorites.list() } }
                .onSuccess { list ->
                    favoritesList = list
                    selectedFavs = selectedFavs intersect list.map { it.entryKey }.toSet()
                    state.setupFavoritesDone.value = list.isNotEmpty()
                    liveNames = emptyMap()
                    scope.launch(Dispatchers.IO) {
                        val unnamed = list.filter { it.name.isBlank() || it.name == it.address }
                        val found = coroutineScope {
                            unnamed.map { fav ->
                                async {
                                    val host = fav.address.substringBeforeLast(':')
                                    val port = fav.address.substringAfterLast(':').toIntOrNull()
                                    if (host.isBlank() || port == null) null
                                    else ServerQuery.queryName(host, port)?.let { fav.entryKey to it }
                                }
                            }.awaitAll().filterNotNull().toMap()
                        }
                        if (found.isNotEmpty()) liveNames = found
                    }
                }
                .onFailure { message = strings.errorGeneric(it.message ?: it.toString()) to true }
        }
    }

    LaunchedEffect(Unit) { refreshFavorites() }

    suspend fun runGuarded(block: suspend () -> Unit) {
        busy = true
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AccessDeniedException) {
            message = strings.errorAccessDenied to true
        } catch (e: Exception) {
            message = strings.errorGeneric(e.message ?: e.toString()) to true
        } finally {
            busy = false
        }
    }

    fun withSteamClosed(action: suspend () -> Unit) {
        scope.launch {
            if (withContext(Dispatchers.IO) { SteamProcess.isSteamRunning() }) {
                steamDialogAction = action
            } else {
                runGuarded(action)
            }
        }
    }

    val defaultPort = defaultPortText.toIntOrNull()?.takeIf { it in 1..65535 } ?: 27015

    val doAdd: suspend () -> Unit = doAdd@{
        val result = parseResult ?: return@doAdd
        val selected = result.servers.withIndex().filter { it.index in checked }
        val (added, skipped) = withContext(Dispatchers.IO) {
            state.favorites.add(
                selected.map { (i, s) ->
                    FavoritesRepository.NewFavorite(
                        s.name ?: previewNames[i] ?: s.address(defaultPort),
                        s.address(defaultPort),
                    )
                }
            )
        }
        message = (strings.favAddedResult(added, skipped) + " " + strings.steamStartReminder) to false
        parseResult = null
        checked = emptySet()
        previewNames = emptyMap()
        refreshFavorites()
    }

    fun removeFavorites(keys: Set<String>) {
        withSteamClosed {
            val removed = withContext(Dispatchers.IO) { state.favorites.remove(keys) }
            message = strings.favRemovedResult(removed) to false
            refreshFavorites()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                strings.setAccount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (accounts.isEmpty()) {
                Text(
                    strings.setNotFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Box {
                    OutlinedButton(onClick = { accountMenuOpen = true }, shape = ButtonShape) {
                        val name = accountId?.let { accountNames[it] }
                        Text(
                            when {
                                accountId == null -> strings.setNotFound
                                name != null -> "$name · $accountId"
                                else -> accountId
                            },
                            fontFamily = if (name == null) FontFamily.Monospace else null,
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }) {
                        accounts.forEach { id ->
                            val name = accountNames[id]
                            DropdownMenuItem(
                                text = {
                                    if (name != null) Text("$name · $id")
                                    else Text(id, fontFamily = FontFamily.Monospace)
                                },
                                leadingIcon = {
                                    if (id == accountId) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    state.settings.update { it.copy(steamAccountId = id) }
                                    accountMenuOpen = false
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(strings.setAutoDetect) },
                            onClick = {
                                state.settings.update { it.copy(steamAccountId = null) }
                                accountMenuOpen = false
                            },
                        )
                    }
                }
                val pinned = settings.steamAccountId != null
                Text(
                    if (pinned) strings.setAccountPinned else strings.setAccountAuto,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pinned) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Left: paste → detect → preview → add
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionCard(strings.favPasteTitle) {
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text(strings.favPasteHint) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.size(12.dp))
                    Button(
                        enabled = pasteText.isNotBlank(),
                        shape = ButtonShape,
                        onClick = {
                            val result = ServerListParser.parse(pasteText)
                            parseResult = result
                            checked = result.servers.indices.toSet()
                            previewNames = emptyMap()
                            val fallbackPort = defaultPort
                            scope.launch(Dispatchers.IO) {
                                val found = coroutineScope {
                                    result.servers.mapIndexed { i, s ->
                                        async {
                                            if (s.name != null) null
                                            else ServerQuery.queryName(s.host, s.port ?: fallbackPort)?.let { i to it }
                                        }
                                    }.awaitAll().filterNotNull().toMap()
                                }
                                if (found.isNotEmpty()) previewNames = found
                            }
                        },
                    ) { Text(strings.favAnalyze) }
                }

                parseResult?.let { result ->
                    SectionCard(
                        strings.favDetected(result.servers.size),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        if (result.ignoredLines.isNotEmpty()) {
                            InfoBanner(
                                strings.favIgnoredLines(result.ignoredLines.size),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        if (result.servers.isEmpty()) {
                            Text(strings.favNoServersFound, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            if (result.servers.any { it.port == null }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.favDefaultPort, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(10.dp))
                                    OutlinedTextField(
                                        value = defaultPortText,
                                        onValueChange = { defaultPortText = it.filter(Char::isDigit).take(5) },
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp),
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                            }
                            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                                itemsIndexed(result.servers) { index, server ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = index in checked,
                                            onCheckedChange = {
                                                checked = if (it) checked + index else checked - index
                                            },
                                        )
                                        val resolvedName = server.name ?: previewNames[index]
                                        Column {
                                            Text(
                                                resolvedName ?: server.host,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (resolvedName != null) FontWeight.Medium else FontWeight.Normal,
                                            )
                                            val portLabel = server.port?.toString()
                                                ?: "$defaultPort (${strings.favMissingPort})"
                                            Text(
                                                "${server.host}:$portLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.size(12.dp))
                            HorizonButton(
                                text = strings.favAddSelected(checked.size),
                                enabled = checked.isNotEmpty() && !busy,
                                onClick = {
                                    if (state.steamLocator.favoritesFile() == null) {
                                        message = strings.favNoAccount to true
                                    } else {
                                        withSteamClosed(doAdd)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Right: current favorites with multi-select
            SectionCard(modifier = Modifier.weight(1f).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CueTitle(strings.favCurrentTitle, modifier = Modifier.weight(1f))
                    if (liveNames.isNotEmpty()) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                val names = liveNames
                                withSteamClosed {
                                    withContext(Dispatchers.IO) {
                                        names.forEach { (key, name) -> state.favorites.rename(key, name) }
                                    }
                                    message = strings.favNamesSaved(names.size) to false
                                    refreshFavorites()
                                }
                            },
                        ) { Text(strings.favSaveNames(liveNames.size)) }
                    }
                    IconButton(onClick = { refreshFavorites() }) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.favRefresh)
                    }
                }
                if (favoritesList.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                strings.favEmpty,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedFavs.size == favoritesList.size,
                            onCheckedChange = { all ->
                                selectedFavs =
                                    if (all) favoritesList.map { it.entryKey }.toSet() else emptySet()
                            },
                        )
                        Text(strings.favSelectAll, style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(
                        Modifier.weight(1f).padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(favoritesList, key = { it.entryKey + it.address }) { fav ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = fav.entryKey in selectedFavs,
                                        onCheckedChange = {
                                            selectedFavs =
                                                if (it) selectedFavs + fav.entryKey
                                                else selectedFavs - fav.entryKey
                                        },
                                    )
                                    val displayName =
                                        if (fav.name.isNotBlank() && fav.name != fav.address) fav.name
                                        else liveNames[fav.entryKey] ?: fav.address
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = if (displayName == fav.address) FontFamily.Monospace else null,
                                        )
                                        if (fav.address != displayName) {
                                            Text(
                                                fav.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(
                                        enabled = !busy,
                                        onClick = {
                                            scope.launch {
                                                val ok = withContext(Dispatchers.IO) {
                                                    SteamProcess.launchConnect(state.steamLocator.steamRoot(), fav.address)
                                                }
                                                message =
                                                    (if (ok) strings.favConnectLaunched else strings.favConnectFailed) to !ok
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = strings.favConnect,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    IconButton(
                                        enabled = !busy,
                                        onClick = { removeFavorites(setOf(fav.entryKey)) },
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = strings.favRemove,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        enabled = selectedFavs.isNotEmpty() && !busy,
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        onClick = { confirmBulkDelete = true },
                    ) { Text(strings.favRemoveSelected(selectedFavs.size)) }
                }
            }
        }
    }

    if (confirmBulkDelete) {
        ConfirmDialog(
            title = strings.favRemoveConfirmTitle,
            body = strings.favRemoveConfirmBody(selectedFavs.size),
            confirmLabel = strings.favRemove,
            dismissLabel = strings.cancel,
            onConfirm = {
                confirmBulkDelete = false
                removeFavorites(selectedFavs)
            },
            onDismiss = { confirmBulkDelete = false },
        )
    }

    steamDialogAction?.let { pendingAction ->
        val waiting = steamWaitJob != null
        AlertDialog(
            onDismissRequest = {
                steamWaitJob?.cancel()
                steamWaitJob = null
                steamDialogAction = null
            },
            title = { Text(strings.steamRunningTitle) },
            text = {
                Column {
                    Text(strings.steamRunningBody)
                    if (waiting) {
                        Spacer(Modifier.size(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.steamClosingWait, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !waiting,
                    onClick = {
                        steamWaitJob = scope.launch {
                            try {
                                val root = state.steamLocator.steamRoot()
                                if (root == null) {
                                    message = strings.favNoAccount to true
                                    steamDialogAction = null
                                    return@launch
                                }
                                withContext(Dispatchers.IO) { SteamProcess.requestSteamShutdown(root) }
                                if (SteamProcess.awaitSteamExit()) {
                                    steamDialogAction = null
                                    runGuarded(pendingAction)
                                } else {
                                    message = strings.steamCloseTimeout to true
                                    steamDialogAction = null
                                }
                            } finally {
                                steamWaitJob = null
                            }
                        }
                    },
                ) { Text(strings.steamCloseAndContinue) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        steamWaitJob?.cancel()
                        steamWaitJob = null
                        steamDialogAction = null
                    },
                ) { Text(strings.cancel) }
            },
        )
    }
}
