package com.arkapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

/*
DIRECTION CONTRACT (user-authored design, 2026-08-04)
THESIS: The user designed the visual world themselves in Claude Design
("Prodigiosos App v2", Nocturne design system) and handed it off as HTML.
This file translates its tokens verbatim; the mock is the authority.
OWN-WORLD: Nocturne graphite darks (#1b1d21 window / #17181c sidebar /
#212429 cards / #16181b fields), hairline white-alpha borders, 6dp buttons,
8dp cards, sidebar navigation with inset accent bar, and a USER-SELECTABLE
accent (morado default, azul, verde, amarillo, rojo). Success #8fbf8f,
danger #c98a86/#c4544d, warn #d6a45e.
FINISH: DESIGN.md records this system; PRODUCT.md pins it as brand.
*/

data class Accent(val key: String, val base: Color, val hi: Color, val deep: Color)

object Accents {
    val all = listOf(
        Accent("morado", Color(0xFF968AE0), Color(0xFFA89DEA), Color(0xFF6A5FC0)),
        Accent("azul", Color(0xFF6FA8DC), Color(0xFF8ABBE6), Color(0xFF4A7FB0)),
        Accent("verde", Color(0xFF7FBF8F), Color(0xFF95CDA3), Color(0xFF5A9A6C)),
        Accent("amarillo", Color(0xFFD6B45E), Color(0xFFE2C67E), Color(0xFFB08F3C)),
        Accent("rojo", Color(0xFFD1706A), Color(0xFFDD8A85), Color(0xFFAB4F4A)),
    )
    fun byKey(key: String?): Accent = all.firstOrNull { it.key == key } ?: all[0]
}

val LocalAccent = compositionLocalOf { Accents.all[0] }

object Palette {
    val Bg = Color(0xFF1B1D21)
    val Sidebar = Color(0xFF17181C)
    val Card = Color(0xFF212429)
    val Field = Color(0xFF16181B)
    val Hover = Color(0xFF2C3038)
    val Border7 = Color.White.copy(alpha = 0.07f)
    val Border12 = Color.White.copy(alpha = 0.12f)
    val Border14 = Color.White.copy(alpha = 0.14f)
    val Text = Color(0xFFE8E9EC)
    val Soft = Color(0xFFB9BEC6)
    val Muted = Color(0xFF8A9099)
    val Dim = Color(0xFF6B7178)
    val Success = Color(0xFF8FBF8F)
    val DangerSoft = Color(0xFFC98A86)
    val Danger = Color(0xFFC4544D)
    val Warn = Color(0xFFD6A45E)
    val OnAccent = Color(0xFF16181B)
}

val ButtonShape = RoundedCornerShape(6.dp)
val CardShape = RoundedCornerShape(8.dp)
val ModalShape = RoundedCornerShape(10.dp)

/**
 * Digit visually centered inside a small circle: trims the font's ascent/descent
 * padding so the glyph box, not the line box, is what gets centered.
 */
@Composable
fun CircleDigit(text: String, fontSize: TextUnit, color: Color) {
    Text(
        text,
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}

// ---------- Buttons ----------

@Composable
private fun ButtonBase(
    text: String,
    enabled: Boolean,
    bg: Color,
    border: Color?,
    textColor: Color,
    onClick: () -> Unit,
    horizontalPadding: Dp = 16.dp,
    width: Dp = Dp.Unspecified,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        color = bg,
        border = border?.let { BorderStroke(1.dp, it) },
    ) {
        Box(
            if (width != Dp.Unspecified) Modifier.width(width).padding(vertical = 8.dp)
            else Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
        }
    }
}

/** Accent-filled primary action. */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val accent = LocalAccent.current
    ButtonBase(
        text, enabled,
        bg = if (enabled) accent.base else Palette.Hover,
        border = null,
        textColor = if (enabled) Palette.OnAccent else Palette.Dim,
        onClick = onClick,
        horizontalPadding = 18.dp,
    )
}

/** Neutral outline. */
@Composable
fun GhostButton(text: String, enabled: Boolean = true, width: Dp = Dp.Unspecified, onClick: () -> Unit) {
    ButtonBase(
        text, enabled,
        bg = Color.Transparent,
        border = Palette.Border14,
        textColor = if (enabled) Palette.Soft else Palette.Dim,
        onClick = onClick,
        width = width,
    )
}

/** Accent outline (secondary emphasis). */
@Composable
fun AccentGhostButton(text: String, enabled: Boolean = true, width: Dp = Dp.Unspecified, onClick: () -> Unit) {
    val accent = LocalAccent.current
    ButtonBase(
        text, enabled,
        bg = Color.Transparent,
        border = accent.base.copy(alpha = 0.5f),
        textColor = if (enabled) accent.hi else Palette.Dim,
        onClick = onClick,
        width = width,
    )
}

/** Success-green outline (connect). */
@Composable
fun SuccessGhostButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    ButtonBase(
        text, enabled,
        bg = Color.Transparent,
        border = Palette.Success.copy(alpha = 0.4f),
        textColor = if (enabled) Palette.Success else Palette.Dim,
        onClick = onClick,
        horizontalPadding = 13.dp,
    )
}

/** Danger outline. */
@Composable
fun DangerGhostButton(text: String, enabled: Boolean = true, width: Dp = Dp.Unspecified, onClick: () -> Unit) {
    ButtonBase(
        text, enabled,
        bg = Color.Transparent,
        border = Palette.DangerSoft.copy(alpha = 0.45f),
        textColor = if (enabled) Palette.DangerSoft else Palette.Dim,
        onClick = onClick,
        width = width,
    )
}

/** Solid destructive commit (inside confirm dialogs). */
@Composable
fun DangerButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    ButtonBase(
        text, enabled,
        bg = if (enabled) Palette.Danger else Palette.Hover,
        border = null,
        textColor = if (enabled) Color.White else Palette.Dim,
        onClick = onClick,
    )
}

/** Small bordered square icon action (refresh, per-row delete…). */
@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = Palette.Soft,
    size: Dp = 29.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Palette.Border14),
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = if (enabled) tint else Palette.Dim, modifier = Modifier.size(14.dp))
        }
    }
}

// ---------- Containers ----------

/** Bordered graphite panel (#212429). */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = Palette.Card,
        border = BorderStroke(1.dp, Palette.Border7),
    ) {
        Column(content = content)
    }
}

/** Section title, 15sp semibold (inside card headers). */
@Composable
fun CardTitle(text: String, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
        if (count != null) {
            Text(" · $count", fontSize = 13.sp, color = Palette.Muted)
        }
    }
}

/** Thin 1px separator line. */
@Composable
fun HairLine(alpha: Float = 0.07f) {
    Box(Modifier.fillMaxWidth().height(1.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth().height(1.dp), color = Color.White.copy(alpha = alpha)) {}
    }
}

/** Amber warning banner. */
@Composable
fun WarnBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = ButtonShape,
        color = Palette.Warn.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Palette.Warn.copy(alpha = 0.32f)),
        modifier = modifier,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = Palette.Warn,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
        )
    }
}

/** Dismissible toast banner at the top of the content area. */
@Composable
fun StatusMessage(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val accent = LocalAccent.current
    val tint = if (isError) Palette.Danger else accent.base
    Surface(
        shape = CardShape,
        color = tint.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 13.sp, color = Palette.Text, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ---------- Inputs ----------

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    mono: Boolean = false,
    readOnly: Boolean = false,
) {
    val accent = LocalAccent.current
    // Hide the placeholder as soon as the field gains focus so it never sits under the caret.
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        placeholder = placeholder?.takeIf { !focused }?.let { { Text(it, fontSize = 13.sp, color = Palette.Dim) } },
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly,
        textStyle = TextStyle(
            fontSize = if (mono) 12.sp else 13.sp,
            lineHeight = 19.sp,
            color = Palette.Text,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        ),
        shape = ButtonShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Palette.Field,
            unfocusedContainerColor = Palette.Field,
            focusedBorderColor = accent.base.copy(alpha = 0.6f),
            unfocusedBorderColor = Palette.Border12,
            cursorColor = accent.base,
            focusedTextColor = Palette.Text,
            unfocusedTextColor = Palette.Text,
        ),
    )
}

// ---------- Modals ----------

/** Scrimmed centered modal container in the app window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModal(width: Dp, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(width),
            shape = ModalShape,
            color = Palette.Card,
            border = BorderStroke(1.dp, Palette.Border12),
        ) {
            Column(content = content)
        }
    }
}

/** Modal header row: title + close X. */
@Composable
fun ModalHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(14.dp))
        }
    }
}

/** Wizard progress: numbered dots joined by lines. */
@Composable
fun WizardSteps(current: Int, labels: List<String>) {
    val accent = LocalAccent.current
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val n = index + 1
            val on = current == n
            val done = current > n
            Surface(
                shape = CircleShape,
                color = when {
                    on -> accent.base
                    done -> accent.base.copy(alpha = 0.18f)
                    else -> Palette.Hover
                },
                border = when {
                    on -> null
                    done -> BorderStroke(1.dp, accent.base.copy(alpha = 0.5f))
                    else -> BorderStroke(1.dp, Palette.Border14)
                },
                modifier = Modifier.size(22.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircleDigit(
                        "$n",
                        fontSize = 11.sp,
                        color = when {
                            on -> Palette.OnAccent
                            done -> accent.hi
                            else -> Palette.Muted
                        },
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                color = if (on) Palette.Text else Palette.Muted,
            )
            if (index < labels.lastIndex) {
                Box(Modifier.width(8.dp))
                Surface(Modifier.width(56.dp).height(1.dp), color = Palette.Border12) {}
                Box(Modifier.width(8.dp))
            }
        }
    }
}

/** Confirm dialog in the design language. */
@Composable
fun ConfirmModal(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    warning: String? = null,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModal(width = 440.dp, onDismiss = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
            Spacer(Modifier.height(10.dp))
            Text(body, fontSize = 13.sp, color = Palette.Muted, lineHeight = 20.sp)
            if (warning != null) {
                Spacer(Modifier.height(10.dp))
                WarnBanner(warning, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                GhostButton(dismissLabel, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                if (danger) DangerButton(confirmLabel, enabled = confirmEnabled, onClick = onConfirm)
                else PrimaryButton(confirmLabel, enabled = confirmEnabled, onClick = onConfirm)
            }
        }
    }
}

/** Big green check circle for wizard done steps. */
@Composable
fun DoneCircle() {
    Surface(
        shape = CircleShape,
        color = Palette.Success.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Palette.Success.copy(alpha = 0.4f)),
        modifier = Modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Palette.Success,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
