package com.dynamicframe.presentation.slideshow

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.dynamicframe.domain.model.TransitionType
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent

@Composable
fun SlideshowImageContent(
    uri: Uri,
    transitionType: TransitionType,
    modifier: Modifier = Modifier
) {
    val enableKenBurns = transitionType == TransitionType.KEN_BURNS

    val scale = if (enableKenBurns) {
        val infinite = rememberInfiniteTransition(label = "kenburns")
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "kenburnsScale"
        ).value
    } else 1f

    // Landscapist: carga optimizada; transiciones entre fotos = AnimatedContent (Compose)
    CoilImage(
        imageModel = { uri },
        imageOptions = ImageOptions(
            contentScale = ContentScale.Crop,
            contentDescription = null
        ),
        component = rememberImageComponent { },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
            }
        },
        failure = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
