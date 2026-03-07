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
    const val durationShort = 150
    const val durationMedium = 200
    const val durationLong = 280
    const val durationScreen = 200

    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val easingAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    val springMediumBounce: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        )

    val springLowBounce: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        )

    fun tweenMedium(): AnimationSpec<Float> =
        tween(
            durationMillis = durationMedium,
            easing = easingStandard,
        )

    fun tweenLong(): TweenSpec<Float> =
        tween(
            durationMillis = durationLong,
            easing = easingStandard,
        )
}
