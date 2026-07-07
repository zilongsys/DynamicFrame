package com.dynamicframe.presentation.slideshow

/**
 * Fase de la sesión a pantalla completa.
 * - [Idle]: fuera del visualizador o sesión cerrada.
 * - [Preparing]: precarga de fondo dinámico; sin música, vídeo ni contenido visible.
 * - [Presenting]: slideshow visible y reproducción permitida.
 */
enum class SlideshowPresentationPhase {
    Idle,
    Preparing,
    Presenting,
}
