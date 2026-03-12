package com.loaderapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppMotion
import kotlinx.coroutines.delay

private const val MAX_STAGGERED_ITEMS = 6
private const val STAGGER_DELAY_DIVISOR = 5

@Composable
fun Modifier.staggeredListItemAppearance(index: Int): Modifier {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        if (!visible) {
            val itemDelay = (AppMotion.DURATION_SHORT / STAGGER_DELAY_DIVISOR) * index.coerceAtMost(MAX_STAGGERED_ITEMS)
            delay(itemDelay.toLong())
            visible = true
        }
    }

    val alpha by
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = AppMotion.tweenMedium(),
            label = "list_item_alpha",
        )
    val offsetY by
        animateFloatAsState(
            targetValue = if (visible) 0f else 8f,
            animationSpec = AppMotion.tweenMedium(),
            label = "list_item_offset",
        )

    return this
        .graphicsLayer { this.alpha = alpha }
        .offset(y = offsetY.dp)
}
