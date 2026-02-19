package com.loaderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/**
 * Переиспользуемый полноэкранный градиентный фон.
 *
 * Цвета: primaryContainer (верх, слабый оттенок) → background (низ).
 * Автоматически адаптируется к светлой и тёмной теме.
 *
 * Используется на всех экранах кроме Профиля, где контент скроллится.
 * Для скролл-экранов используй [scrollableGradientBackground].
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val topColor    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    val bottomColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor))),
        content = content
    )
}

/**
 * Modifier-расширение для скролл-контейнеров с бесшовным градиентом.
 *
 * Ключевое отличие от [GradientBackground]:
 * `endY = Float.POSITIVE_INFINITY` привязывает конец градиента к реальной
 * высоте контента, а не к высоте viewport'а. Это устраняет жёсткую цветовую
 * границу при скролле — градиент тянется вместе с контентом бесконечно.
 *
 * Порядок модификаторов важен: [scrollableGradientBackground] нужно применять
 * ДО [androidx.compose.foundation.verticalScroll], чтобы фон рисовался
 * под контентом, а не поверх него.
 *
 * Пример:
 * ```kotlin
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .scrollableGradientBackground()   // ← сначала фон
 *         .verticalScroll(rememberScrollState())
 * ) { ... }
 * ```
 */
@Composable
fun Modifier.scrollableGradientBackground(): Modifier {
    val topColor    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    val bottomColor = MaterialTheme.colorScheme.background
    return this.background(
        Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            endY   = Float.POSITIVE_INFINITY
        )
    )
}
