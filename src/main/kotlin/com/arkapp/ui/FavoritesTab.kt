package com.arkapp.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkapp.AppState
import com.arkapp.i18n.LocalStrings
import com.arkapp.steam.FavoritesRepository
import com.arkapp.steam.ServerListParser
import com.arkapp.steam.ServerParseResult
import com.arkapp.steam.SteamProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.AccessDeniedException

@Composable
fun FavoritesTab(state: AppState) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

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

    fun refreshFavorites() {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { state.favorites.list() } }
                .onSuccess { list ->
                    favoritesList = list
                    selectedFavs = selectedFavs intersect list.map { it.entryKey }.toSet()
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
        val selected = result.servers.filterIndexed { i, _ -> i in checked }
        val (added, skipped) = withContext(Dispatchers.IO) {
            state.favorites.add(
                selected.map {
                    FavoritesRepository.NewFavorite(it.name ?: it.address(defaultPort), it.address(defaultPort))
                }
            )
        }
        message = (strings.favAddedResult(added, skipped) + " " + strings.steamStartReminder) to false
        parseResult = null
        checked = emptySet()
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
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Left: paste → detect → preview → add
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(strings.favPasteTitle) {
                    InfoBanner(strings.favQueryPortHint, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.size(12.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text(strings.favPasteHint) },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.size(12.dp))
                    Button(
                        enabled = pasteText.isNotBlank(),
                        onClick = {
                            val result = ServerListParser.parse(pasteText)
                            parseResult = result
                            checked = result.servers.indices.toSet()
                        },
                    ) { Text(strings.favAnalyze) }
                }

                parseResult?.let { result ->
                    SectionCard(strings.favDetected(result.servers.size)) {
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
                            result.servers.forEachIndexed { index, server ->
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
                                    Column {
                                        Text(
                                            server.name ?: server.host,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (server.name != null) FontWeight.Medium else FontWeight.Normal,
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
                            Spacer(Modifier.size(12.dp))
                            Button(
                                enabled = checked.isNotEmpty() && !busy,
                                onClick = {
                                    if (state.steamLocator.favoritesFile() == null) {
                                        message = strings.favNoAccount to true
                                    } else {
                                        withSteamClosed(doAdd)
                                    }
                                },
                            ) { Text(strings.favAddSelected(checked.size)) }
                        }
                    }
                }
            }

            // Right: current favorites with multi-select
            SectionCard(modifier = Modifier.weight(1f).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.favCurrentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { refreshFavorites() }) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.favRefresh)
                    }
                }
                if (favoritesList.isEmpty()) {
                    Text(
                        strings.favEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
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
                                shape = RoundedCornerShape(10.dp),
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
                                    Column(Modifier.weight(1f)) {
                                        Text(fav.name, style = MaterialTheme.typography.bodyMedium)
                                        if (fav.address != fav.name) {
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
