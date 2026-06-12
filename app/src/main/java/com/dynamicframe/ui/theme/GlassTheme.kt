package com.dynamicframe.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.dynamicframe.presentation.device.LocalDeviceProfile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GlassWhite = Color.White.copy(alpha = 0.14f)
val GlassWhiteStrong = Color.White.copy(alpha = 0.22f)
val GlassBorder = Color.White.copy(alpha = 0.28f)
val GlassText = Color.White.copy(alpha = 0.95f)
val GlassTextMuted = Color.White.copy(alpha = 0.72f)

private val AuroraGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4A2C3D),
        Color(0xFFB85C78),
        Color(0xFFE8A0BF),
        Color(0xFFFFF0F3)
    )
)

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "aurora")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF3D2230),
                    Color(0xFF8E4A62).copy(alpha = 0.9f + shift * 0.1f),
                    Color(0xFFD4738F),
                    Color(0xFFFFD6E6)
                )
            )
        )
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassAlbumPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        selected -> Color.White.copy(alpha = 0.32f)
        focused -> Color.White.copy(alpha = 0.24f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    val borderColor = when {
        focused -> Color.White.copy(alpha = 0.95f)
        selected -> Color.White.copy(alpha = 0.55f)
        else -> GlassBorder
    }

    val device = LocalDeviceProfile.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .then(
                if (device.isTv) Modifier.safeClickable(onClick = onClick, showFocusBorder = false)
                else Modifier
                    .focusable()
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) GlassText else GlassTextMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun GlassAlbumPillRow(
    pills: List<Pair<String?, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(pills, key = { it.first ?: "all" }) { (id, label) ->
            GlassAlbumPill(
                label = label,
                selected = selectedId == id || (id == null && selectedId == null),
                onClick = { onSelect(id) }
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val device = LocalDeviceProfile.current
    val shape = RoundedCornerShape(16.dp)
    val bg = if (prominent) GlassWhiteStrong else GlassWhite

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else GlassBorder,
                shape = shape
            )
            .then(
                if (device.isTv) Modifier.safeClickable(onClick = onClick, showFocusBorder = false)
                else Modifier
                    .focusable()
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            )
            .padding(horizontal = if (label != null) 14.dp else 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GlassText)
        if (label != null) {
            Text(label, color = GlassText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GlassWhite)
            .border(1.dp, GlassBorder, CircleShape)
            .safeClickable(onClick = onClick, showFocusBorder = false),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = GlassText)
    }
}
