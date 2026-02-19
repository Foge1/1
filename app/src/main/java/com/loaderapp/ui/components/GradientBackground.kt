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
 * Переиспользуемый фон с вертикальным градиентом для всех экранов.
 *
 * Градиент строится от [MaterialTheme.colorScheme.primaryContainer] (верх)
 * до [MaterialTheme.colorScheme.background] (низ) и автоматически
 * адаптируется к светлой и тёмной теме.
 *
 * Для скролл-экранов (Profile и др.) используй [scrollableGradientModifier] —
 * он растягивает градиент вместе с контентом, устраняя жёсткую границу.
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            ),
        content = content
    )
}

/**
 * Modifier для скролл-контейнеров, которым нужен бесшовный градиент.
 *
 * В отличие от [GradientBackground], градиент здесь привязан не к высоте
 * экрана, а к реальной высоте контента через [endY] = [Float.POSITIVE_INFINITY].
 * Это даёт плавный переход без жёсткой границы при скролле вниз.
 *
 * Использование:
 * ```
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .verticalScroll(rememberScrollState())
 *         .scrollableGradientBackground(topColor, bottomColor)
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
            // Float.POSITIVE_INFINITY → градиент растягивается на всю длину контента,
            // не ограничиваясь высотой видимой области
            endY   = Float.POSITIVE_INFINITY
        )
    )
}
