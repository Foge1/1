package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LazyColumn с плавным fade-переходом сверху и снизу.
 *
 * ## Принцип работы
 *
 * [CompositingStrategy.Offscreen] рендерит контент во временный буфер.
 * [BlendMode.DstIn] обрезает альфа-канал буфера по форме градиента —
 * контент «растворяется» у краёв без каких-либо View-хаков или оверлеев.
 *
 * Оба fade рисуются в одном [drawWithContent]-вызове:
 * - Верх: карточки уходят под frosted топбар
 * - Низ: карточки уходят под навбар без видимой границы
 *
 * @param topFadeHeight    Высота верхнего fade (должна совпадать с topBarHeight AppScaffold)
 * @param bottomFadeHeight Высота нижнего fade
 * @param contentPadding   Padding контента (top должен >= topFadeHeight, bottom >= bottomFadeHeight)
 * @param state            Состояние списка для внешнего управления прокруткой
 */
@Composable
fun FadingEdgeLazyColumn(
    modifier: Modifier = Modifier,
    topFadeHeight: Dp = 88.dp,
    bottomFadeHeight: Dp = 36.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        LazyColumn(
            state          = state,
            contentPadding = contentPadding,
            modifier       = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()

                    // Верхний fade: прозрачный -> непрозрачный
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.40f to Color.Black.copy(alpha = 0.6f),
                                1.00f to Color.Black
                            ),
                            startY = 0f,
                            endY   = topFadeHeight.toPx()
                        ),
                        blendMode = BlendMode.DstIn
                    )

                    // Нижний fade: непрозрачный -> прозрачный
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - bottomFadeHeight.toPx(),
                            endY   = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            content = content
        )
    }
}
