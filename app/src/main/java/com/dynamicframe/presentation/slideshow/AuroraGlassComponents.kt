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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.dynamicframe.ui.components.AppAsyncImage
import com.dynamicframe.ui.theme.FocusHintEffect
import com.dynamicframe.ui.theme.safeClickable

/** Paleta Aurora Glass — idéntica al mockup (cyan eléctrico + cristal azul oscuro). */
val AuroraCyan = Color(0xFF00E5FF)
val AuroraCyanBright = Color(0xFF66F0FF)
val AuroraCyanDim = Color(0x9900E5FF)
val AuroraGlassDark = Color(0xF0101828)
val AuroraGlassDeep = Color(0xD8121E30)
val AuroraGlassBorder = Color(0x6600E5FF)
val AuroraGlassBorderBright = Color(0xCC00E5FF)
val AuroraText = Color(0xFFFFFFFF)
val AuroraTextMuted = Color(0xADFFFFFF)
val AuroraTrack = Color(0x1FFFFFFF)

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
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCC121E30),
                        Color(0xF0101828)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AuroraGlassBorderBright.copy(alpha = 0.55f),
                        AuroraGlassBorder.copy(alpha = 0.25f),
                        AuroraGlassBorderBright.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    onFocusHint: ((String) -> Unit)? = null,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Box(
        modifier = modifier
            .size(buttonSize)
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
        Icon(icon, contentDescription, tint = AuroraText, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun AuroraVolumeAdjustButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    hintDescription: String,
    onFocusHint: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})

    Box(
        modifier = modifier
            .size(size)
            .scale(if (focused && device.isTv) 1.08f else 1f)
            .clip(CircleShape)
            .background(if (focused) AuroraCyan.copy(alpha = 0.22f) else Color(0x28FFFFFF))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) AuroraCyan else Color.White.copy(alpha = 0.22f),
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
        Icon(icon, contentDescription, tint = AuroraText, modifier = Modifier.size(18.dp))
    }
}

/** Slider con bolita de nivel. En [compact] va en la fila de transporte; incluye botones ± enfocables. */
@Composable
fun AuroraGlassVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    step: Float = 0.05f,
    compact: Boolean = false,
    inlineTrackWidth: Dp = if (compact) 88.dp else Dp.Unspecified,
    showStepButtons: Boolean = compact,
    decreaseHintDescription: String = "Bajar volumen",
    increaseHintDescription: String = "Subir volumen",
    hintDescription: String = "Volumen",
    onFocusHint: ((String) -> Unit)? = null
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val trackNavigable = device.isTv && !showStepButtons
    if (trackNavigable) {
        FocusHintEffect(focused = focused, description = hintDescription, onHint = onFocusHint ?: {})
    }

    val iconSize = if (compact) 16.dp else 20.dp
    val trackHeight = if (compact) 5.dp else if (focused && trackNavigable) 6.dp else 5.dp
    val thumbSize = if (compact) 12.dp else 12.dp
    val clamped = value.coerceIn(0f, 1f)
    val decrease = { onValueChange((value - step).coerceIn(0f, 1f)) }
    val increase = { onValueChange((value + step).coerceIn(0f, 1f)) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 10.dp)
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (trackNavigable && focused) AuroraCyanBright else AuroraTextMuted,
            modifier = Modifier.size(iconSize)
        )
        if (showStepButtons) {
            AuroraVolumeAdjustButton(
                icon = Icons.Default.Remove,
                contentDescription = decreaseHintDescription,
                hintDescription = decreaseHintDescription,
                onFocusHint = onFocusHint,
                onClick = decrease
            )
        }
        if (device.isTv) {
            val trackModifier = if (compact && inlineTrackWidth != Dp.Unspecified) {
                Modifier.width(inlineTrackWidth)
            } else {
                Modifier.weight(1f)
            }
            BoxWithConstraints(
                modifier = trackModifier.height(thumbSize + 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val density = LocalDensity.current
                val trackPx = with(density) { maxWidth.toPx() }
                val thumbPx = with(density) { thumbSize.toPx() }
                val thumbOffsetPx = ((trackPx - thumbPx).coerceAtLeast(0f)) * clamped
                val thumbOffset = with(density) { thumbOffsetPx.toDp() }

                val trackInteractionModifier = if (trackNavigable) {
                    Modifier
                        .focusable(interactionSource = interaction)
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> { decrease(); true }
                                Key.DirectionRight -> { increase(); true }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.ButtonA -> {
                                    increase(); true
                                }
                                else -> false
                            }
                        }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = increase
                        )
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AuroraTrack)
                        .border(
                            width = if (trackNavigable && focused) 1.dp else 0.dp,
                            color = AuroraCyan.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(3.dp)
                        )
                        .then(trackInteractionModifier)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(clamped.coerceAtLeast(0.02f))
                        .height(trackHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AuroraCyan.copy(alpha = 0.45f), AuroraCyanBright)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset, y = 0.dp)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(AuroraCyanBright)
                        .border(
                            width = if (trackNavigable && focused) 2.dp else 1.dp,
                            color = if (trackNavigable && focused) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            shape = CircleShape
                        )
                )
            }
        } else {
            Slider(
                value = clamped,
                onValueChange = onValueChange,
                modifier = if (compact && inlineTrackWidth != Dp.Unspecified) {
                    Modifier.width(inlineTrackWidth)
                } else {
                    Modifier.weight(1f)
                },
                colors = SliderDefaults.colors(
                    thumbColor = AuroraCyanBright,
                    activeTrackColor = AuroraCyan,
                    inactiveTrackColor = AuroraTrack
                )
            )
        }
        if (showStepButtons) {
            AuroraVolumeAdjustButton(
                icon = Icons.Default.Add,
                contentDescription = increaseHintDescription,
                hintDescription = increaseHintDescription,
                onFocusHint = onFocusHint,
                onClick = increase
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
    pills: List<AlbumPillOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = LocalDeviceProfile.current
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .then(if (device.isTv) Modifier.focusGroup() else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(pills, key = { pill -> "${pill.id ?: "all"}::${pill.label}" }) { pill ->
            val selected = selectedId == pill.id || (pill.id == null && selectedId == null)
            AuroraAlbumCard(
                pill = pill,
                selected = selected,
                onClick = { onSelect(pill.id) }
            )
        }
    }
}

@Composable
private fun AuroraAlbumCard(
    pill: AlbumPillOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val device = LocalDeviceProfile.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        focused -> AuroraCyanBright
        selected -> AuroraCyan
        else -> Color.White.copy(alpha = 0.18f)
    }
    val bg = when {
        selected -> AuroraCyan.copy(alpha = 0.12f)
        focused -> Color.White.copy(alpha = 0.08f)
        else -> Color(0x18FFFFFF)
    }
    val shortLabel = pill.label
        .removePrefix("Fotos: ")
        .removePrefix("Videos: ")

    Row(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 188.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                width = if (focused || selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
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
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AuroraTrack)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
        ) {
            if (pill.thumbnailUri != null) {
                AppAsyncImage(
                    uri = pill.thumbnailUri,
                    contentDescription = shortLabel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    decodeWidth = 128,
                    decodeHeight = 128,
                    crossfadeMillis = 200
                )
            } else {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = AuroraTextMuted,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortLabel,
                color = if (selected || focused) AuroraText else AuroraTextMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (pill.itemCount > 0) {
                val countLabel = if (pill.label.startsWith("Videos:")) {
                    "${pill.itemCount} vídeos"
                } else {
                    "${pill.itemCount} fotos"
                }
                Text(
                    text = countLabel,
                    color = AuroraTextMuted.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
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
                .height(3.dp)
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
