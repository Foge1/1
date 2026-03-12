package com.loaderapp.core.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

data class PressScaleConfig(
    val scaleDown: Float = 0.97f,
    val duration: Int = AppMotion.DURATION_MEDIUM,
    val easing: Easing = AppMotion.EASING_STANDARD,
)

fun <T> tweenMedium(): TweenSpec<T> =
    tween(
        durationMillis = AppMotion.DURATION_MEDIUM,
        easing = AppMotion.EASING_STANDARD,
    )

fun <T> tweenLong(): TweenSpec<T> =
    tween(
        durationMillis = AppMotion.DURATION_LONG,
        easing = AppMotion.EASING_STANDARD,
    )

fun Modifier.pressScale(
    enabled: Boolean = true,
    config: PressScaleConfig = PressScaleConfig(),
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
): Modifier =
    if (!enabled) {
        this
    } else {
        composed {
            val resolvedInteractionSource = remember(interactionSource) { interactionSource }
            val isPressed by resolvedInteractionSource.collectIsPressedAsState()
            val scale by
                animateFloatAsState(
                    targetValue = if (isPressed) config.scaleDown else 1f,
                    animationSpec = tween(durationMillis = config.duration, easing = config.easing),
                    label = "press_scale",
                )

            graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        }
    }
