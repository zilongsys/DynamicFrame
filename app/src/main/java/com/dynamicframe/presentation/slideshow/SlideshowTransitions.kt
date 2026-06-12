package com.dynamicframe.presentation.slideshow

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset
import com.dynamicframe.domain.model.MediaItem
import com.dynamicframe.domain.model.TransitionType

private val SoftEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val DramaticEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val SnapEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

fun transitionMillis(type: TransitionType, configuredMs: Int): Int {
    val base = configuredMs.coerceIn(600, 3200)
    return when (type) {
        TransitionType.NONE -> 0
        TransitionType.DISSOLVE, TransitionType.CROSSFADE, TransitionType.KEN_BURNS ->
            base.coerceIn(900, 3200)
        TransitionType.BLUR_FADE -> (base * 1.1f).toInt()
        else -> base
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun AnimatedContentTransitionScope<MediaItem>.slideshowTransitionSpec(
    type: TransitionType,
    durationMs: Int
): ContentTransform {
    val ms = transitionMillis(type, durationMs)
    if (ms <= 0) return EnterTransition.None togetherWith ExitTransition.None

    val enterTween = tween<Float>(durationMillis = ms, easing = SoftEasing)
    val exitTween = tween<Float>(durationMillis = (ms * 0.88f).toInt(), easing = SoftEasing)
    val enterOffsetTween = tween<IntOffset>(durationMillis = ms, easing = DramaticEasing)
    val exitOffsetTween = tween<IntOffset>(durationMillis = (ms * 0.88f).toInt(), easing = DramaticEasing)
    val wipeTween = tween<IntOffset>(durationMillis = ms, easing = SnapEasing)

    val softEnter = fadeIn(enterTween) + scaleIn(initialScale = 1.02f, animationSpec = enterTween)
    val softExit = fadeOut(exitTween) + scaleOut(targetScale = 0.98f, animationSpec = exitTween)

    return when (type) {
        TransitionType.CROSSFADE -> softEnter togetherWith softExit

        TransitionType.FADE -> fadeIn(enterTween) togetherWith fadeOut(exitTween)

        TransitionType.DISSOLVE -> {
            fadeIn(tween(ms, easing = LinearEasing)) togetherWith
                fadeOut(tween((ms * 1.2f).toInt(), easing = LinearEasing))
        }

        TransitionType.KEN_BURNS -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 1.0f, animationSpec = tween(ms, easing = LinearEasing)))
                .togetherWith(fadeOut(exitTween))
        }

        TransitionType.BLUR_FADE -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 1.05f, animationSpec = enterTween))
                .togetherWith(fadeOut(exitTween) + scaleOut(targetScale = 0.94f, animationSpec = exitTween))
        }

        TransitionType.SLIDE_LEFT -> {
            (slideInHorizontally(enterOffsetTween) { it / 3 } + fadeIn(enterTween))
                .togetherWith(slideOutHorizontally(exitOffsetTween) { -it / 3 } + fadeOut(exitTween))
        }

        TransitionType.SLIDE_RIGHT -> {
            (slideInHorizontally(enterOffsetTween) { -it / 3 } + fadeIn(enterTween))
                .togetherWith(slideOutHorizontally(exitOffsetTween) { it / 3 } + fadeOut(exitTween))
        }

        TransitionType.SLIDE_UP -> {
            (slideInVertically(enterOffsetTween) { it / 4 } + fadeIn(enterTween))
                .togetherWith(slideOutVertically(exitOffsetTween) { -it / 4 } + fadeOut(exitTween))
        }

        TransitionType.SLIDE_DOWN -> {
            (slideInVertically(enterOffsetTween) { -it / 4 } + fadeIn(enterTween))
                .togetherWith(slideOutVertically(exitOffsetTween) { it / 4 } + fadeOut(exitTween))
        }

        TransitionType.ZOOM_IN -> {
            (scaleIn(initialScale = 0.88f, animationSpec = enterTween) + fadeIn(enterTween))
                .togetherWith(scaleOut(targetScale = 1.08f, animationSpec = exitTween) + fadeOut(exitTween))
        }

        TransitionType.ZOOM_OUT -> {
            (scaleIn(initialScale = 1.12f, animationSpec = enterTween) + fadeIn(enterTween))
                .togetherWith(scaleOut(targetScale = 0.82f, animationSpec = exitTween) + fadeOut(exitTween))
        }

        TransitionType.ROTATE -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 0.75f, animationSpec = enterTween) +
                slideInHorizontally(enterOffsetTween) { it / 6 })
                .togetherWith(
                    fadeOut(exitTween) + scaleOut(targetScale = 1.15f, animationSpec = exitTween) +
                        slideOutHorizontally(exitOffsetTween) { -it / 6 }
                )
        }

        TransitionType.FLIP_HORIZONTAL -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 0.01f, animationSpec = tween(ms, easing = DramaticEasing)))
                .togetherWith(
                    fadeOut(exitTween) + scaleOut(targetScale = 0.01f, animationSpec = tween(ms, easing = DramaticEasing))
                )
        }

        TransitionType.FLIP_VERTICAL -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 0.85f, animationSpec = enterTween) +
                slideInVertically(enterOffsetTween) { it / 2 })
                .togetherWith(
                    fadeOut(exitTween) + scaleOut(targetScale = 0.85f, animationSpec = exitTween) +
                        slideOutVertically(exitOffsetTween) { -it / 2 }
                )
        }

        TransitionType.WIPE_LEFT -> {
            slideInHorizontally(wipeTween) { it } togetherWith slideOutHorizontally(wipeTween) { -it }
        }

        TransitionType.WIPE_RIGHT -> {
            slideInHorizontally(wipeTween) { -it } togetherWith slideOutHorizontally(wipeTween) { it }
        }

        TransitionType.DEPTH -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 0.82f, animationSpec = enterTween) +
                slideInHorizontally(enterOffsetTween) { it / 5 })
                .togetherWith(
                    fadeOut(exitTween) + scaleOut(targetScale = 1.18f, animationSpec = exitTween) +
                        slideOutHorizontally(exitOffsetTween) { -it / 8 }
                )
        }

        TransitionType.STACK -> {
            (slideInHorizontally(enterOffsetTween) { it / 2 } + fadeIn(enterTween) +
                scaleIn(initialScale = 0.94f, animationSpec = enterTween))
                .togetherWith(
                    slideOutHorizontally(exitOffsetTween) { -it / 4 } + fadeOut(exitTween) +
                        scaleOut(targetScale = 0.9f, animationSpec = exitTween)
                )
        }

        TransitionType.PARALLAX -> {
            (slideInHorizontally(enterOffsetTween) { it / 6 } + fadeIn(enterTween))
                .togetherWith(
                    slideOutHorizontally(exitOffsetTween) { -it / 2 } + fadeOut(exitTween) +
                        scaleOut(targetScale = 0.96f, animationSpec = exitTween)
                )
        }

        TransitionType.CUBE -> {
            (fadeIn(enterTween) + scaleIn(initialScale = 0.9f, animationSpec = enterTween) +
                slideInHorizontally(enterOffsetTween) { it / 2 })
                .togetherWith(
                    fadeOut(exitTween) + scaleOut(targetScale = 0.9f, animationSpec = exitTween) +
                        slideOutHorizontally(exitOffsetTween) { -it / 2 }
                )
        }

        TransitionType.NONE -> EnterTransition.None togetherWith ExitTransition.None
    }
}
