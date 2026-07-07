package com.dynamicframe.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.MediaSource
import com.dynamicframe.domain.model.MediaType
import com.dynamicframe.domain.model.TransitionType
import com.dynamicframe.presentation.slideshow.slideshowTransitionSpec
import kotlinx.coroutines.delay

private val PreviewSlideA = MediaItem(
    id = "transition_preview_a",
    uri = "",
    type = MediaType.IMAGE,
    source = MediaSource.LOCAL,
    name = "A",
)
private val PreviewSlideB = MediaItem(
    id = "transition_preview_b",
    uri = "",
    type = MediaType.IMAGE,
    source = MediaSource.LOCAL,
    name = "B",
)

/** Vista previa en Ajustes: usa el mismo [slideshowTransitionSpec] que el slideshow real. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideshowTransitionPreview(
    transitionType: TransitionType,
    durationMs: Int,
    modifier: Modifier = Modifier,
) {
    var showSecond by remember { mutableStateOf(false) }
    val currentSlide = if (showSecond) PreviewSlideB else PreviewSlideA

    LaunchedEffect(transitionType, durationMs) {
        showSecond = false
        while (true) {
            delay(1_200L)
            showSecond = !showSecond
            delay(durationMs.coerceAtLeast(400).toLong() + 1_000L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
        ) {
            AnimatedContent(
                targetState = currentSlide,
                contentKey = { it.id },
                transitionSpec = {
                    slideshowTransitionSpec(transitionType, durationMs)
                },
                label = "settings_transition_preview",
                modifier = Modifier.fillMaxSize(),
            ) { slide ->
                TransitionPreviewSlide(slide = slide)
            }
        }
        Text(
            text = "Vista previa",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
    }
}

@Composable
private fun TransitionPreviewSlide(slide: MediaItem) {
    val brush = when (slide.id) {
        PreviewSlideA.id -> Brush.linearGradient(
            colors = listOf(Color(0xFF5C7A99), Color(0xFF2A3D52)),
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFF997A5C), Color(0xFF523D2A)),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = slide.name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

/** Descripción del efecto visual que aplica cada transición en reproducción. */
fun TransitionType.effectDescription(): String = when (this) {
    TransitionType.CROSSFADE ->
        "Fundido suave: la foto actual se desvanece con un leve alejamiento mientras la nueva aparece acercándose."
    TransitionType.FADE ->
        "Fundido clásico: solo cambia la opacidad, sin movimiento ni zoom."
    TransitionType.DISSOLVE ->
        "Disolvencia lenta: fundido lineal; la foto saliente tarda un poco más en desaparecer."
    TransitionType.KEN_BURNS ->
        "Ken Burns: la nueva foto entra con zoom lento desde un 92 % hasta tamaño normal."
    TransitionType.BLUR_FADE ->
        "Fundido con zoom: la entrada llega ligeramente grande y la salida se reduce al desvanecerse."
    TransitionType.SLIDE_LEFT ->
        "Deslizar izquierda: la nueva entra desde la derecha y la anterior sale hacia la izquierda."
    TransitionType.SLIDE_RIGHT ->
        "Deslizar derecha: la nueva entra desde la izquierda y la anterior sale hacia la derecha."
    TransitionType.SLIDE_UP ->
        "Deslizar arriba: la nueva sube desde abajo y la anterior sale hacia arriba."
    TransitionType.SLIDE_DOWN ->
        "Deslizar abajo: la nueva baja desde arriba y la anterior sale hacia abajo."
    TransitionType.ZOOM_IN ->
        "Zoom acercar: la nueva foto crece desde pequeña; la saliente se amplía al desaparecer."
    TransitionType.ZOOM_OUT ->
        "Zoom alejar: la nueva llega desde grande; la saliente se encoge al desvanecerse."
    TransitionType.ROTATE ->
        "Rotación suave: entrada con escala reducida y desplazamiento lateral; salida con zoom out."
    TransitionType.FLIP_HORIZONTAL ->
        "Volteo horizontal: efecto de giro comprimiendo la imagen al centro (escala casi cero)."
    TransitionType.FLIP_VERTICAL ->
        "Volteo vertical: compresión vertical con deslizamiento arriba/abajo."
    TransitionType.WIPE_LEFT ->
        "Barrido izquierda: la nueva foto empuja a la anterior hacia la izquierda (sin fundido)."
    TransitionType.WIPE_RIGHT ->
        "Barrido derecha: la nueva foto empuja a la anterior hacia la derecha (sin fundido)."
    TransitionType.DEPTH ->
        "Profundidad 3D: la nueva entra desde lejos (pequeña); la saliente se aleja agrandándose."
    TransitionType.STACK ->
        "Apilado: la nueva se desliza encima con ligero zoom; la anterior se corre y encoge."
    TransitionType.PARALLAX ->
        "Parallax: capas a distinta velocidad — entrada lenta, salida más rápida con leve escala."
    TransitionType.CUBE ->
        "Cubo: deslizamiento lateral con escala, simulando una cara del cubo que rota."
    TransitionType.NONE ->
        "Sin transición: corte instantáneo a la siguiente foto."
}
