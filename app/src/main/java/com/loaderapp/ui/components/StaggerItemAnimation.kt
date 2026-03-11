package com.loaderapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppMotion

@Composable
fun Modifier.staggeredItemAppearance(
    index: Int,
): Modifier {
    val alpha = remember { Animatable(0f) }
    val translationOffset = remember { Animatable(StaggerDefaults.INITIAL_TRANSLATION_DP.value) }

    LaunchedEffect(index) {
        val delayMillis = (index * StaggerDefaults.DELAY_STEP_MILLIS).coerceAtMost(StaggerDefaults.MAX_DELAY_MILLIS)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = AppMotion.DURATION_MEDIUM,
                delayMillis = delayMillis,
                easing = AppMotion.EASING_STANDARD,
            ),
        )
        translationOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = AppMotion.DURATION_MEDIUM,
                delayMillis = delayMillis,
                easing = AppMotion.EASING_STANDARD,
            ),
        )
    }

    return this
        .alpha(alpha.value)
        .graphicsLayer {
            translationY = translationOffset.value * density
        }
}

private object StaggerDefaults {
    val INITIAL_TRANSLATION_DP = 10.dp
    const val DELAY_STEP_MILLIS = 24
    const val MAX_DELAY_MILLIS = 168
}
