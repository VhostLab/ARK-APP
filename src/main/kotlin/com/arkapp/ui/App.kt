package com.arkapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkapp.AppState
import com.arkapp.i18n.EnStrings
import com.arkapp.i18n.EsStrings
import com.arkapp.i18n.LocalStrings
import com.arkapp.i18n.Strings
import com.arkapp.update.UpdateChecker
import com.arkapp.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.system.exitProcess

fun systemLanguage(): String = if (Locale.getDefault().language == "es") "es" else "en"

@Composable
fun App(state: AppState) {
    val settings by state.settings.state.collectAsState()
    val strings: Strings = if ((settings.language ?: systemLanguage()) == "es") EsStrings else EnStrings
    val accent = Accents.byKey(settings.accentColor)

    val colorScheme = darkColorScheme(
        primary = accent.base,
        onPrimary = Palette.OnAccent,
        secondary = Palette.Warn,
        onSecondary = Palette.OnAccent,
        background = Palette.Bg,
        onBackground = Palette.Text,
        surface = Palette.Card,
        onSurface = Palette.Text,
        surfaceVariant = Palette.Field,
        onSurfaceVariant = Palette.Muted,
        outline = Palette.Border14,
        error = Palette.Danger,
        onError = Color.White,
    )

    val arkDetectedAtStart = remember { state.arkLocator.arkRoot() != null }
    var selectedTab by remember {
        // ARKAPP_TAB overrides the initial tab (dev/testing aid)
        val initial = System.getenv("ARKAPP_TAB")?.toIntOrNull()?.coerceIn(0, 3)
        mutableStateOf(initial ?: if (arkDetectedAtStart) 0 else 3)
    }

    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var updating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        update = withContext(Dispatchers.IO) { UpdateChecker.check() }
    }

    // Render-loop probe (dev only): ARKAPP_FPS=1 forces FULL-SURFACE redraws and logs the real Hz.
    val probeOn = remember { System.getenv("ARKAPP_FPS") != null }
    var probeTick by remember { mutableStateOf(0) }
    if (probeOn) {
        LaunchedEffect(Unit) {
            var last = 0L
            var frames = 0
            while (true) {
                withFrameNanos { now ->
                    probeTick++
                    frames++
                    if (last == 0L) last = now
                    if (now - last >= 1_000_000_000L) {
                        println("ARKAPP_FPS: $frames")
                        frames = 0
                        last = now
                    }
                }
            }
        }
    }

    // First-run checklist: shown ONCE per install (the first launch with something
    // missing), then marked as seen. It only returns when relaunched from Settings.
    val setupFavDone by state.setupFavoritesDone.collectAsState()
    val setupProfDone by state.setupProfileDone.collectAsState()
    var setupShown by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            state.seedDefaultProfiles()
            state.refreshSetupState()
        }
    }
    LaunchedEffect(settings.setupDismissed, setupFavDone, setupProfDone) {
        val fav = setupFavDone ?: return@LaunchedEffect
        val prof = setupProfDone ?: return@LaunchedEffect
        if (!settings.setupDismissed) {
            if (setupShown == null && fav && prof) {
                state.settings.update { it.copy(setupDismissed = true) }
            } else {
                setupShown = true
                state.settings.update { it.copy(setupDismissed = true) }
            }
        } else if (setupShown == null) {
            setupShown = false
        }
    }

    CompositionLocalProvider(LocalStrings provides strings, LocalAccent provides accent) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(Modifier.fillMaxSize(), color = Palette.Bg) {
                if (probeOn) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Color(red = (probeTick % 256) / 255f, green = 0f, blue = 0f, alpha = 0.02f)
                        )
                    )
                }
                val probeInput = if (probeOn) {
                    Modifier.pointerInput(Unit) {
                        var count = 0
                        var last = 0L
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                count++
                                val now = System.nanoTime()
                                if (last == 0L) last = now
                                if (now - last >= 1_000_000_000L) {
                                    println("ARKAPP_EVT: $count")
                                    count = 0
                                    last = now
                                }
                            }
                        }
                    }
                } else Modifier
                Row(Modifier.fillMaxSize().then(probeInput)) {
                    Sidebar(state, selectedTab, onSelectTab = { selectedTab = it })
                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(horizontal = 26.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        update?.let { info ->
                            UpdateBanner(
                                info = info,
                                updating = updating,
                                onInstall = {
                                    updating = true
                                    scope.launch {
                                        val launched = withContext(Dispatchers.IO) {
                                            UpdateChecker.downloadAndLaunchInstaller(info)
                                        }
                                        if (launched) {
                                            exitProcess(0)
                                        } else {
                                            UpdateChecker.openReleasesPage()
                                            updating = false
                                        }
                                    }
                                },
                                onDismiss = { update = null },
                            )
                        }
                        if (setupShown == true) {
                            SetupStrip(
                                favoritesDone = setupFavDone == true,
                                profileDone = setupProfDone == true,
                                selectedTab = selectedTab,
                                onSelectTab = { selectedTab = it },
                                onDismiss = { setupShown = false },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> FavoritesTab(state)
                                1 -> ProfilesTab(state)
                                2 -> GameConfigTab(state)
                                else -> SettingsTab(state)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(state: AppState, selectedTab: Int, onSelectTab: (Int) -> Unit) {
    val strings = LocalStrings.current
    val settings by state.settings.state.collectAsState()
    val accountId = remember(settings) { state.steamLocator.accountId() }
    val accounts = remember(settings) { state.steamLocator.availableAccountIds() }
    val accountNames = remember(settings) { state.steamLocator.accountNames() }
    var accountMenuOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Palette.Sidebar)
            .padding(horizontal = 12.dp)
            .padding(top = 22.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 18.dp)) {
            Text(strings.appTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, lineHeight = 19.sp)
            Text("ARK: Survival Evolved", fontSize = 11.sp, color = Palette.Muted)
        }
        NavItem(strings.tabFavorites, selectedTab == 0) { onSelectTab(0) }
        NavItem(strings.tabProfiles, selectedTab == 1) { onSelectTab(1) }
        NavItem(strings.tabGameConfig, selectedTab == 2) { onSelectTab(2) }
        NavItem(strings.tabSettings, selectedTab == 3) { onSelectTab(3) }
        Spacer(Modifier.weight(1f))

        val pinned = settings.steamAccountId != null
        Box {
            Surface(
                onClick = { accountMenuOpen = true },
                shape = ButtonShape,
                color = Color(0xFF1F2126),
                border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Border7),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        accountId?.let { accountNames[it] ?: it } ?: strings.setNotFound,
                        fontSize = 12.sp,
                        color = Palette.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (pinned) strings.setAccountPinnedShort else strings.setAccountAutoShort,
                        fontSize = 9.5.sp,
                        color = Palette.Dim,
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = strings.setAccount,
                        tint = Palette.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            DropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }) {
                accounts.forEach { id ->
                    val name = accountNames[id]
                    DropdownMenuItem(
                        text = { Text(if (name != null) "$name · $id" else id, fontSize = 13.sp) },
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
                    text = { Text(strings.setAutoDetect, fontSize = 13.sp) },
                    onClick = {
                        state.settings.update { it.copy(steamAccountId = null) }
                        accountMenuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        color = if (selected) accent.base.copy(alpha = 0.14f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(5.dp))) {
            Box(
                Modifier.width(2.dp).height(34.dp)
                    .background(if (selected) accent.base else Color.Transparent)
            )
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Palette.Text else Palette.Muted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun UpdateBanner(info: UpdateInfo, updating: Boolean, onInstall: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val accent = LocalAccent.current
    Surface(
        shape = CardShape,
        color = accent.base.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.base.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.updateAvailable(info.version), fontSize = 13.sp, color = Palette.Text, modifier = Modifier.weight(1f))
            PrimaryButton(
                if (updating) strings.updateDownloading else strings.updateInstall,
                enabled = !updating,
                onClick = onInstall,
            )
            IconButton(enabled = !updating, onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** First-run checklist driven by real state: favorites present and INI profile applied. */
@Composable
private fun SetupStrip(
    favoritesDone: Boolean,
    profileDone: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val accent = LocalAccent.current
    Surface(
        shape = CardShape,
        color = Palette.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Border7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.setupTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent.hi,
            )
            Spacer(Modifier.width(20.dp))
            if (favoritesDone && profileDone) {
                Text(
                    strings.setupAllDone,
                    fontSize = 13.sp,
                    color = Palette.Success,
                    modifier = Modifier.weight(1f),
                )
            } else {
                SetupStep(
                    number = 1,
                    label = strings.setupStepFavorites,
                    done = favoritesDone,
                    actionLabel = strings.setupGoFavorites.takeIf { !favoritesDone && selectedTab != 0 },
                    onAction = { onSelectTab(0) },
                )
                Spacer(Modifier.width(24.dp))
                SetupStep(
                    number = 2,
                    label = strings.setupStepProfile,
                    done = profileDone,
                    actionLabel = strings.setupGoProfiles.takeIf { !profileDone && selectedTab != 1 },
                    onAction = { onSelectTab(1) },
                )
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    label: String,
    done: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    val accent = LocalAccent.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Palette.Success,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Surface(
                shape = CircleShape,
                color = accent.base.copy(alpha = 0.22f),
                modifier = Modifier.size(18.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircleDigit("$number", fontSize = 10.sp, color = accent.hi)
                }
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = if (done) Palette.Muted else Palette.Text,
        )
        if (actionLabel != null) {
            Spacer(Modifier.width(2.dp))
            TextButton(
                onClick = onAction,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) { Text(actionLabel, fontSize = 12.sp, color = accent.hi) }
        }
    }
}
