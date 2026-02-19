package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
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
 * LazyColumn с плавным fade-переходом сверху.
 *
 * ## Принцип работы верхнего фейда
 *
 * Для создания «матового стекла» при скролле используется compositing trick:
 * 1. [graphicsLayer] с [CompositingStrategy.Offscreen] → контент рисуется
 *    во временный offscreen-буфер.
 * 2. [drawWithContent] рисует контент, затем поверх него — Brush с прозрачным
 *    градиентом через [BlendMode.DstIn]. Это обрезает альфа-канал буфера
 *    по форме градиента, создавая плавное затухание.
 *
 * Результат: карточки при уходе вверх становятся прозрачными через «мутное
 * стекло» без каких-либо View-хаков, clip или отдельных overlay-слоёв.
 * Это нативный, GPU-ускоренный способ в Compose.
 *
 * ## Нижний край
 * Нижний fade намеренно отсутствует — контент доходит до навбара без
 * видимой границы. Тень на навбаре добавляется в [AppBottomBar] через
 * [shadowElevation], визуально «отделяя» панель от контента.
 *
 * @param fadeHeight    Высота верхнего fade-перехода
 * @param contentPadding Padding для контента LazyColumn
 * @param state         Состояние списка (для внешнего управления прокруткой)
 */
@Composable
fun FadingEdgeLazyColumn(
    modifier: Modifier = Modifier,
    fadeHeight: Dp = 56.dp,
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
                // Offscreen rendering — обязательно для BlendMode.DstIn
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()

                    // Верхний fade: прозрачный → непрозрачный
                    // BlendMode.DstIn оставляет только пересечение альфа-канала
                    // контента с градиентом → плавное «растворение» карточек вверху
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black
                            ),
                            endY = fadeHeight.toPx()
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            content = content
        )
    }
}
