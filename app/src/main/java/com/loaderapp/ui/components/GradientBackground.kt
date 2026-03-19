package com.loaderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val GradientBackgroundStart = Color(0xFF0F2227)
private val GradientBackgroundEnd = Color(0xFF0F1416)

internal fun appScreenGradientBrush(endY: Float = Float.POSITIVE_INFINITY): Brush =
    Brush.verticalGradient(
        colors = listOf(GradientBackgroundStart, GradientBackgroundEnd),
        endY = endY,
    )

internal fun appScreenBackgroundBottomColor(): Color = GradientBackgroundEnd

/**
 * Переиспользуемый полноэкранный градиентный фон.
 *
 * Цвета: #0F2227 (верх) → #0F1416 (низ).
 *
 * Используется на всех экранах кроме Профиля, где контент скроллится.
 * Для скролл-экранов используй [scrollableGradientBackground].
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(appScreenGradientBrush()),
        content = content,
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
fun Modifier.scrollableGradientBackground(): Modifier = this.background(appScreenGradientBrush())
