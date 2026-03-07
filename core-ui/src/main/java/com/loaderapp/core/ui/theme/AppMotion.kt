package com.loaderapp.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AppMotion {
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 200
    const val DURATION_LONG = 280
    const val DURATION_SCREEN = 200

    val EASING_STANDARD: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EASING_DECELERATE: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val EASING_ACCELERATE: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    val SPRING_MEDIUM_BOUNCE: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        )

    val SPRING_LOW_BOUNCE: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        )

    fun tweenMedium(): AnimationSpec<Float> =
        tween(
            durationMillis = DURATION_MEDIUM,
            easing = EASING_STANDARD,
        )

    fun tweenLong(): TweenSpec<Float> =
        tween(
            durationMillis = DURATION_LONG,
            easing = EASING_STANDARD,
        )
}
