package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.presentation.device.LocalDeviceProfile
import com.dynamicframe.ui.theme.FocusHintEffect
import com.dynamicframe.ui.theme.safeClickable

/** Paleta Aurora Glass — cyan eléctrico sobre cristal oscuro (mockup HUD). */
val AuroraCyan = Color(0xFF00E5FF)
val AuroraCyanDim = Color(0x9900E5FF)
val AuroraGlassDark = Color(0xCC081220)
val AuroraGlassDeep = Color(0x99081220)
val AuroraGlassBorder = Color(0x4D00E5FF)
val AuroraText = Color(0xF2FFFFFF)
val AuroraTextMuted = Color(0xB3FFFFFF)

@Composable
fun AuroraClockTop(
    time: String,
    date: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99081220))
            .border(1.dp, AuroraGlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(
            text = time,
            color = AuroraText,
            fontSize = 36.sp,
            fontWeight = FontWeight.Light
        )
        if (date != null) {
            Text(text = date, color = AuroraTextMuted, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
fun AuroraGlassHud(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(AuroraGlassDeep, AuroraGlassDark)
                )
            )
            .border(1.dp, AuroraGlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun AuroraHudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hintDescription: String = contentDescription,
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Box(
        modifier = modifier
            .size(44.dp)
            .scale(if (focused && device.isTv) 1.08f else 1f)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) AuroraCyan else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interaction
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = AuroraText, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun AuroraCenterPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    hintDescription: String = if (isPlaying) "Pausar" else "Reproducir",
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Box(
        modifier = modifier
            .size(120.dp)
            .scale(if (focused && device.isTv) 1.05f else 1f)
            .onFocusChanged { onFocusChanged(it.isFocused) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraCyan.copy(alpha = if (focused) 0.55f else 0.28f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0x4400E5FF))
                .border(
                    width = if (focused) 3.dp else 2.dp,
                    color = if (focused) AuroraCyan else AuroraCyanDim,
                    shape = CircleShape
                )
                .safeClickable(
                    onClick = onClick,
                    showFocusBorder = false,
                    focusShape = CircleShape,
                    interactionSource = interaction
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = hintDescription,
                tint = AuroraText,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun AuroraTransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hintDescription: String = contentDescription,
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(if (focused && device.isTv) 1.1f else 1f)
            .clip(CircleShape)
            .background(if (focused) AuroraCyan.copy(alpha = 0.25f) else Color(0x22FFFFFF))
            .border(
                1.dp,
                if (focused) AuroraCyan else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
            .safeClickable(
                onClick = onClick,
                showFocusBorder = false,
                focusShape = CircleShape,
                interactionSource = interaction
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = AuroraText, modifier = Modifier.size(24.dp))
    }
}

/** Slider fino estilo mockup (no stepper 40%/100%). En TV: ← → ajusta. */
@Composable
fun AuroraGlassVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    step: Float = 0.05f,
    hintDescription: String = "Volumen",
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (focused) AuroraCyan else AuroraTextMuted,
            modifier = Modifier.size(20.dp)
        )
        if (device.isTv) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (focused) 8.dp else 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = if (focused) 1.dp else 0.dp,
                        color = AuroraCyan.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .focusable(interactionSource = interaction)
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft -> {
                                onValueChange((value - step).coerceIn(0f, 1f)); true
                            }
                            Key.DirectionRight -> {
                                onValueChange((value + step).coerceIn(0f, 1f)); true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.ButtonA -> {
                                onValueChange((value + step).coerceIn(0f, 1f)); true
                            }
                            else -> false
                        }
                    }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onValueChange((value + step).coerceIn(0f, 1f)) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value.coerceIn(0.02f, 1f))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AuroraCyan.copy(alpha = 0.5f), AuroraCyan)
                            )
                        )
                )
            }
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AuroraCyan,
                    activeTrackColor = AuroraCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
fun AuroraMusicChip(
    title: String,
    artist: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x44081220))
            .border(1.dp, AuroraGlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = null,
            tint = AuroraCyan,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(title, color = AuroraText, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
            if (artist != null) {
                Text(artist, color = AuroraTextMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun AuroraAlbumPillRow(
    pills: List<Pair<String?, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .then(if (device.isTv) Modifier.focusGroup() else Modifier),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(pills, key = { (id, label) -> "${id ?: "all"}::$label" }) { (id, label) ->
            val selected = selectedId == id || (id == null && selectedId == null)
            AuroraAlbumPill(
                label = label,
                selected = selected,
                onClick = { onSelect(id) }
            )
        }
    }
}

@Composable
private fun AuroraAlbumPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        focused -> AuroraCyan
        selected -> AuroraCyan.copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.2f)
    }
    val bg = when {
        selected -> AuroraCyan.copy(alpha = 0.18f)
        focused -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.06f)
    }

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = if (focused || selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .then(
                if (device.isTv) {
                    Modifier.safeClickable(
                        onClick = onClick,
                        showFocusBorder = false,
                        interactionSource = interaction
                    )
                } else {
                    Modifier
                        .focusable(interactionSource = interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                }
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected || focused) AuroraText else AuroraTextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun AuroraProgressRow(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AuroraCyan,
            trackColor = Color.White.copy(alpha = 0.12f)
        )
        Text(
            text = label,
            color = AuroraTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
