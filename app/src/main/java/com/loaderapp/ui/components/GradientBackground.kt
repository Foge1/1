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
 * Цвета берутся из текущей [MaterialTheme.colorScheme]:
 *  - [androidx.compose.material3.ColorScheme.primaryContainer] (верх, слабый оттенок)
 *  - [androidx.compose.material3.ColorScheme.background]       (низ, чистый фон)
 *
 * Такой подход автоматически подхватывает и светлую, и тёмную тему без изменений.
 *
 * Использование:
 * ```
 * GradientBackground {
 *     // содержимое экрана
 * }
 * ```
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
