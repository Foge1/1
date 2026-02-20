package com.loaderapp.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalTopBarHeightPx = compositionLocalOf { 0 }

@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    GradientBackground(modifier = modifier) {
        SubcomposeLayout { constraints ->
            val topBarPlaceables = subcompose("topBar") {
                AppTopBar(title = title, actions = actions)
            }.map { it.measure(constraints.copy(minHeight = 0)) }
            val topBarHeightPx = topBarPlaceables.maxOfOrNull { it.height } ?: 0

            val contentPlaceables = subcompose("content") {
                CompositionLocalProvider(LocalTopBarHeightPx provides topBarHeightPx) {
                    Box(modifier = Modifier.fillMaxSize(), content = content)
                }
            }.map { it.measure(constraints) }

            val blurPlaceables = subcompose("blur") {
                TopFadeOverlay(heightPx = topBarHeightPx)
            }.map { it.measure(constraints.copy(minHeight = 0)) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                contentPlaceables.forEach { it.placeRelative(0, 0) }
                blurPlaceables.forEach { it.placeRelative(0, topBarHeightPx) }
                topBarPlaceables.forEach { it.placeRelative(0, 0) }
            }
        }
    }
}

@Composable
private fun TopFadeOverlay(heightPx: Int) {
    if (heightPx == 0) return

    val bgColor = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val overlayHeight = with(density) { heightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(overlayHeight)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
            .background(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) bgColor.copy(alpha = 0.46f)
                else bgColor.copy(alpha = 0.88f)
            )
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black,
                            0.65f to Color.Black,
                            1.00f to Color.Transparent
                        )
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

@Composable
private fun AppTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
