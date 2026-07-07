package com.dynamicframe.presentation.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de espera mientras se precalcula paleta + imágenes del fondo dinámico.
 */
@Composable
fun DynamicLetterboxLoadingOverlay(
  modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White.copy(alpha = 0.85f),
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Preparando fondo…",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
