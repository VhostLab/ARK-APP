package com.arkapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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

/*
DIRECTION CONTRACT (impeccable, revised: user took the standing exit)
THESIS: The category standard executed impeccably — a sober premium dark
utility at Discord/new-Steam craft level. The cyclorama-dawn world (seed
dc4bfb92) was built, reviewed (ship) and rejected by the user on color.
OWN-WORLD: Neutral near-black ladder #0E1013 / #15181D / #1D2127; ONE accent,
the brand cyan #4DD0E1; success green #66BB6A; error #FF6E6E. Flat ground,
matte 8dp panels, 6dp band buttons in tracked caps, cue titles over a cyan
fading rule, mono for every address.
STORY / FIRST VIEWPORT: unchanged from the layout + onboarding work.
FORM: canon (standing exit); recorded as a brand commitment in PRODUCT.md.
FINISH: DESIGN.md records this palette, not the rejected world.
*/
private val AppColors = darkColorScheme(
    primary = Color(0xFF4DD0E1),        // brand cyan, the single accent
    onPrimary = Color(0xFF00363D),
    secondary = Color(0xFFFFB74D),      // hint banners
    onSecondary = Color(0xFF3E2E00),
    tertiary = Color(0xFF9AE3EE),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE2E5E9),
    surface = Color(0xFF15181D),
    onSurface = Color(0xFFE2E5E9),
    surfaceVariant = Color(0xFF1D2127),
    onSurfaceVariant = Color(0xFFA6ADB8),
    outline = Color(0xFF2A2F36),
    error = Color(0xFFFF6E6E),
    onError = Color(0xFF3D0000),
)

private val CueTypography = Typography().let { t ->
    t.copy(
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
        labelLarge = t.labelLarge.copy(letterSpacing = 1.2.sp),
        labelMedium = t.labelMedium.copy(letterSpacing = 1.0.sp),
    )
}

fun systemLanguage(): String = if (Locale.getDefault().language == "es") "es" else "en"

@Composable
fun App(state: AppState) {
    val settings by state.settings.state.collectAsState()
    val strings: Strings = if ((settings.language ?: systemLanguage()) == "es") EsStrings else EnStrings

    val arkDetectedAtStart = remember { state.arkLocator.arkRoot() != null }
    var selectedTab by remember {
        // ARKAPP_TAB overrides the initial tab (dev/testing aid)
        val initial = System.getenv("ARKAPP_TAB")?.toIntOrNull()?.coerceIn(0, 2)
        mutableStateOf(initial ?: if (arkDetectedAtStart) 0 else 2)
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
        withContext(Dispatchers.IO) { state.refreshSetupState() }
    }
    LaunchedEffect(settings.setupDismissed, setupFavDone, setupProfDone) {
        val fav = setupFavDone ?: return@LaunchedEffect
        val prof = setupProfDone ?: return@LaunchedEffect
        if (!settings.setupDismissed) {
            if (setupShown == null && fav && prof) {
                // Everything was already set up before ever seeing the guide: mark seen silently.
                state.settings.update { it.copy(setupDismissed = true) }
            } else {
                setupShown = true
                state.settings.update { it.copy(setupDismissed = true) }
            }
        } else if (setupShown == null) {
            setupShown = false
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        MaterialTheme(colorScheme = AppColors, typography = CueTypography) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val probeInput = if (System.getenv("ARKAPP_FPS") != null) {
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
                if (probeOn) {
                    // Full-surface invalidation: a near-invisible wash whose color changes every frame.
                    Box(
                        Modifier.fillMaxSize().background(
                            Color(red = (probeTick % 256) / 255f, green = 0f, blue = 0f, alpha = 0.02f)
                        )
                    )
                }
                Column(Modifier.fillMaxSize().then(probeInput)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.appTitle,
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.4.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "ARK: Survival Evolved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    update?.let { info ->
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    strings.updateAvailable(info.version),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    enabled = !updating,
                                    shape = ButtonShape,
                                    onClick = {
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
                                ) {
                                    Text(if (updating) strings.updateDownloading else strings.updateInstall)
                                }
                                IconButton(enabled = !updating, onClick = { update = null }) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(2.dp)
                                    .background(HorizonRule)
                            )
                        },
                    ) {
                        AppTab(strings.tabFavorites, Icons.Default.Star, selectedTab == 0) { selectedTab = 0 }
                        AppTab(strings.tabProfiles, Icons.AutoMirrored.Filled.List, selectedTab == 1) { selectedTab = 1 }
                        AppTab(strings.tabSettings, Icons.Default.Settings, selectedTab == 2) { selectedTab = 2 }
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
                    when (selectedTab) {
                        0 -> FavoritesTab(state)
                        1 -> ProfilesTab(state)
                        else -> SettingsTab(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium)
            }
        },
    )
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
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                strings.setupTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(20.dp))
            if (favoritesDone && profileDone) {
                Text(
                    strings.setupAllDone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen,
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
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                modifier = Modifier.size(18.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null) {
            Spacer(Modifier.width(2.dp))
            TextButton(
                onClick = onAction,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) { Text(actionLabel, style = MaterialTheme.typography.labelMedium) }
        }
    }
}
