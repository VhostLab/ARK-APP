package com.arkapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.AccessDeniedException

@Composable
fun FavoritesTab(state: AppState) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val settings by state.settings.state.collectAsState()

    // Wizard: 0 closed, 1 paste, 2 review, 3 done
    var wizStep by remember { mutableStateOf(0) }
    var pasteText by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<ServerParseResult?>(null) }
    var checked by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var defaultPortText by remember { mutableStateOf("27015") }
    var lastAdded by remember { mutableStateOf(0 to 0) }

    var favoritesList by remember { mutableStateOf<List<FavoritesRepository.Favorite>>(emptyList()) }
    var selectedFavs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var steamDialogAction by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    var steamWaitJob by remember { mutableStateOf<Job?>(null) }

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

    // Success messages fade out on their own; errors stay until dismissed.
    LaunchedEffect(message) {
        val current = message
        if (current != null && !current.second) {
            delay(5000)
            if (message == current) message = null
        }
    }

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
        lastAdded = added to skipped
        wizStep = 3
        refreshFavorites()
    }

    fun closeWizard() {
        wizStep = 0
        pasteText = ""
        parseResult = null
        checked = emptySet()
        previewNames = emptyMap()
    }

    fun removeFavorites(keys: Set<String>) {
        withSteamClosed {
            withContext(Dispatchers.IO) { state.favorites.remove(keys) }
            refreshFavorites()
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        message?.let { (text, isError) -> StatusMessage(text, isError) { message = null } }

        // Toolbar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.tabFavorites,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.Text,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton("＋ ${strings.favAddServers}") { wizStep = 1 }
        }

        AppCard(Modifier.weight(1f).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) { CardTitle(strings.favCurrentTitle, favoritesList.size) }
                if (liveNames.isNotEmpty()) {
                    AccentGhostButton(strings.favSaveNames(liveNames.size), enabled = !busy) {
                        val names = liveNames
                        withSteamClosed {
                            withContext(Dispatchers.IO) {
                                names.forEach { (key, name) -> state.favorites.rename(key, name) }
                            }
                            message = strings.favNamesSaved(names.size) to false
                            refreshFavorites()
                        }
                    }
                }
                GhostIconButton(Icons.Default.Refresh, strings.favRefresh) { refreshFavorites() }
            }

            if (favoritesList.isEmpty()) {
                HairLine()
                Box(Modifier.weight(1f).fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.favEmptyTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.favEmptyBody,
                            fontSize = 13.sp,
                            color = Palette.Muted,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(380.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("＋ ${strings.favAddServers}") { wizStep = 1 }
                    }
                }
            } else {
                HairLine()
                val pinnedSet = settings.pinnedServers.toSet()
                val displayList = favoritesList.sortedBy { if (it.address.lowercase() in pinnedSet) 0 else 1 }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Checkbox(
                        checked = selectedFavs.size == favoritesList.size,
                        onCheckedChange = { all ->
                            selectedFavs = if (all) favoritesList.map { it.entryKey }.toSet() else emptySet()
                        },
                    )
                    Text(strings.favSelectAll, fontSize = 12.5.sp, color = Palette.Muted)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(displayList, key = { it.entryKey + it.address }) { fav ->
                        HairLine(0.05f)
                        val pinned = fav.address.lowercase() in pinnedSet
                        val displayName =
                            if (fav.name.isNotBlank() && fav.name != fav.address) fav.name
                            else liveNames[fav.entryKey] ?: fav.address
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = fav.entryKey in selectedFavs,
                                onCheckedChange = {
                                    selectedFavs =
                                        if (it) selectedFavs + fav.entryKey
                                        else selectedFavs - fav.entryKey
                                },
                            )
                            IconButton(
                                onClick = {
                                    val addr = fav.address.lowercase()
                                    state.settings.update {
                                        it.copy(
                                            pinnedServers =
                                                if (addr in it.pinnedServers) it.pinnedServers - addr
                                                else listOf(addr) + it.pinnedServers
                                        )
                                    }
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = if (pinned) strings.favUnpin else strings.favPin,
                                    tint = if (pinned) Palette.Warn else Palette.Dim.copy(alpha = 0.55f),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    fontSize = 13.5.sp,
                                    color = if (pinned) Palette.Warn else Palette.Text,
                                    fontFamily = if (displayName == fav.address) FontFamily.Monospace else FontFamily.Default,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (displayName != fav.address) fav.address else strings.favNoNameYet,
                                    fontSize = 11.5.sp,
                                    color = Palette.Muted,
                                    fontFamily = if (displayName != fav.address) FontFamily.Monospace else FontFamily.Default,
                                )
                            }
                            SuccessGhostButton("▶ ${strings.favConnect}", enabled = !busy) {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        SteamProcess.launchConnect(state.steamLocator.steamRoot(), fav.address)
                                    }
                                    message =
                                        (if (ok) strings.favConnectLaunched else strings.favConnectFailed) to !ok
                                }
                            }
                            GhostIconButton(
                                Icons.Default.Close,
                                strings.favRemove,
                                tint = Palette.DangerSoft,
                                size = 28.dp,
                                enabled = !busy,
                            ) { removeFavorites(setOf(fav.entryKey)) }
                        }
                    }
                }
                HairLine()
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp)) {
                    DangerGhostButton(
                        strings.favRemoveSelected(selectedFavs.size),
                        enabled = selectedFavs.isNotEmpty() && !busy,
                    ) { confirmBulkDelete = true }
                }
            }
        }
    }

    // ══ Add-servers wizard ══
    if (wizStep > 0) {
        AppModal(width = 620.dp, onDismiss = { closeWizard() }) {
            ModalHeader(strings.favWizTitle) { closeWizard() }
            WizardSteps(wizStep, listOf(strings.wizPaste, strings.wizReview, strings.wizDone))
            when (wizStep) {
                1 -> Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(strings.favWizIntro, fontSize = 12.5.sp, color = Palette.Muted, lineHeight = 19.sp)
                    AppTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = strings.favPasteHint,
                        minLines = 7,
                        mono = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 220.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        GhostButton(strings.cancel) { closeWizard() }
                        Spacer(Modifier.width(8.dp))
                        PrimaryButton(strings.favAnalyze, enabled = pasteText.isNotBlank()) {
                            val result = ServerListParser.parse(pasteText)
                            if (result.servers.isEmpty()) {
                                message = strings.favNoServersFound to true
                                return@PrimaryButton
                            }
                            parseResult = result
                            checked = result.servers.indices.toSet()
                            previewNames = emptyMap()
                            wizStep = 2
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
                        }
                    }
                }

                2 -> {
                    val result = parseResult
                    Column(
                        Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            strings.favDetected(result?.servers?.size ?: 0),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.Text,
                        )
                        if (result != null && result.ignoredLines.isNotEmpty()) {
                            WarnBanner(strings.favIgnoredLines(result.ignoredLines.size), Modifier.fillMaxWidth())
                        }
                        if (result != null && result.servers.any { it.port == null }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(strings.favDefaultPort, fontSize = 12.5.sp, color = Palette.Muted)
                                AppTextField(
                                    value = defaultPortText,
                                    onValueChange = { defaultPortText = it.filter(Char::isDigit).take(5) },
                                    singleLine = true,
                                    mono = true,
                                    modifier = Modifier.width(90.dp),
                                )
                                Text(strings.favDefaultPortNote, fontSize = 11.5.sp, color = Palette.Dim)
                            }
                        }
                        androidx.compose.material3.Surface(
                            shape = ButtonShape,
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Border7),
                        ) {
                            LazyColumn(Modifier.heightIn(max = 200.dp).padding(6.dp)) {
                                itemsIndexed(result?.servers ?: emptyList()) { index, server ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                                    ) {
                                        Checkbox(
                                            checked = index in checked,
                                            onCheckedChange = {
                                                checked = if (it) checked + index else checked - index
                                            },
                                        )
                                        Text(
                                            server.name ?: previewNames[index] ?: strings.favNoNameYet,
                                            fontSize = 13.sp,
                                            color = Palette.Text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            server.address(defaultPort),
                                            fontSize = 12.sp,
                                            color = Palette.Muted,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            GhostButton(strings.back) { wizStep = 1 }
                            PrimaryButton(strings.favAddSelected(checked.size), enabled = checked.isNotEmpty() && !busy) {
                                if (state.steamLocator.favoritesFile() == null) {
                                    message = strings.favNoAccount to true
                                } else {
                                    withSteamClosed(doAdd)
                                }
                            }
                        }
                    }
                }

                3 -> Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DoneCircle()
                    Text(
                        strings.favAddedTitle(lastAdded.first, lastAdded.second),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.Text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        strings.favDoneNote,
                        fontSize = 12.5.sp,
                        color = Palette.Muted,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(380.dp),
                    )
                    PrimaryButton(strings.close) { closeWizard() }
                }
            }
        }
    }

    if (confirmBulkDelete) {
        ConfirmModal(
            title = strings.favRemoveConfirmTitle(selectedFavs.size),
            body = strings.favRemoveConfirmBody,
            confirmLabel = strings.favRemove,
            danger = true,
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
        AppModal(width = 420.dp, onDismiss = {
            steamWaitJob?.cancel()
            steamWaitJob = null
            steamDialogAction = null
        }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.steamRunningTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                Text(strings.steamRunningBody, fontSize = 13.sp, color = Palette.Muted, lineHeight = 20.sp)
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostButton(strings.cancel, enabled = !waiting) {
                        steamWaitJob?.cancel()
                        steamWaitJob = null
                        steamDialogAction = null
                    }
                    Spacer(Modifier.width(8.dp))
                    if (waiting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    PrimaryButton(
                        if (waiting) strings.steamClosing else strings.steamCloseAndContinue,
                        enabled = !waiting,
                    ) {
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
                                    // The design promises Steam comes back on its own.
                                    withContext(Dispatchers.IO) { SteamProcess.launchSteam(root) }
                                } else {
                                    message = strings.steamCloseTimeout to true
                                    steamDialogAction = null
                                }
                            } finally {
                                steamWaitJob = null
                            }
                        }
                    }
                }
            }
        }
    }
}
