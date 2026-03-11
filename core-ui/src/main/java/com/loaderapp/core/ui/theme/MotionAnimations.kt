package com.loaderapp.core.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.TweenSpec

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
