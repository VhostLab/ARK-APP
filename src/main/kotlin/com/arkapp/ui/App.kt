package com.arkapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkapp.AppState
import com.arkapp.i18n.EnStrings
import com.arkapp.i18n.EsStrings
import com.arkapp.i18n.LocalStrings
import com.arkapp.i18n.Strings
import java.util.Locale

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    onPrimary = Color(0xFF00363D),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3E2E00),
    background = Color(0xFF0E1416),
    onBackground = Color(0xFFDEE3E5),
    surface = Color(0xFF161D20),
    onSurface = Color(0xFFDEE3E5),
    surfaceVariant = Color(0xFF1E272B),
    onSurfaceVariant = Color(0xFFB2BEC3),
    error = Color(0xFFFF6E6E),
    onError = Color(0xFF3D0000),
)

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

    CompositionLocalProvider(LocalStrings provides strings) {
        MaterialTheme(colorScheme = DarkColors) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "ARK: Survival Evolved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
                        AppTab(strings.tabFavorites, Icons.Default.Star, selectedTab == 0) { selectedTab = 0 }
                        AppTab(strings.tabProfiles, Icons.AutoMirrored.Filled.List, selectedTab == 1) { selectedTab = 1 }
                        AppTab(strings.tabSettings, Icons.Default.Settings, selectedTab == 2) { selectedTab = 2 }
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
                Text(label)
            }
        },
    )
}
