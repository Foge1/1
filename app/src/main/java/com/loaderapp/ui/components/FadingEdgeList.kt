package com.loaderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LazyColumn с плавным fade-переходом снизу через Box-оверлей.
 *
 * Градиентный оверлей (прозрачный → цвет фона) накладывается поверх списка
 * как отдельный слой. Подход не затрагивает альфа-канал контента — текст
 * и элементы карточек остаются полностью непрозрачными.
 *
 * @param topFadeHeight    Зарезервирован для совместимости; в текущей реализации не используется
 * @param bottomFadeHeight Высота нижнего fade-оверлея
 * @param contentPadding   Padding контента списка
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
    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background

    Box(modifier = modifier) {
        LazyColumn(
            state          = state,
            contentPadding = contentPadding,
            modifier       = Modifier.fillMaxSize(),
            content        = content
        )

        // Нижний fade-оверлей: прозрачный → фоновый цвет
        // Только визуальный намёк на продолжение, не затрагивает текст карточек
        if (bottomFadeHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomFadeHeight)
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor)
                        )
                    )
            )
        }
    }
}
