package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.dynamicframe.presentation.device.LocalDeviceProfile

private val WallTone = Color(0xFF1E1A17)
private val GrooveDark = Color(0xFF3D2E1F)
private val GoldDeep = Color(0xFF8B6914)
private val GoldMid = Color(0xFFC9A227)
private val GoldBright = Color(0xFFFFE566)
private val GoldShadow = Color(0xFF6B4F12)
private val MatCream = Color(0xFFF6F0E4)
private val MatInner = Color(0xFFEDE4D4)

private val OuterGoldBrush = Brush.linearGradient(
    colors = listOf(GoldShadow, GoldMid, GoldBright, GoldMid, GoldDeep)
)
private val InnerGoldBrush = Brush.linearGradient(
    colors = listOf(GoldBright, GoldMid, GoldShadow)
)

/** Marco ornamental dorado con paspartú, estilo cuadro clásico. */
@Composable
fun PictureFrame(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val device = LocalDeviceProfile.current
    val scale = (if (device.isTv) 1.35f else 1f) * scaleFactor.coerceIn(0.4f, 2f)
    val outerFrame = (20f * scale).dp
    val groove = (3f * scale).dp
    val innerBand = (7f * scale).dp
    val mat = (16f * scale).dp
    val lip = (2f * scale).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WallTone)
            .padding(outerFrame)
            .background(OuterGoldBrush)
            .padding(groove)
            .background(GrooveDark)
            .padding(innerBand)
            .background(InnerGoldBrush)
            .padding(mat)
            .background(MatCream)
            .border(width = lip, color = GoldMid.copy(alpha = 0.85f))
            .padding(lip)
            .background(MatInner)
            .border(width = 1.dp, color = GoldShadow.copy(alpha = 0.45f))
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .background(Color.Black),
            content = content
        )
    }
}
