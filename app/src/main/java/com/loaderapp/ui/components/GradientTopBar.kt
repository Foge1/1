package com.loaderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Единый TopBar с градиентом для всего приложения.
 *
 * Градиент: surface → прозрачный (сверху вниз), без границ и теней.
 * Используется на всех экранах для консистентного вида.
 *
 * @param title       Заголовок экрана
 * @param navigationIcon  Иконка назад (null = не показывать)
 * @param onNavigationClick  Клик по navigationIcon
 * @param actions     Слот для action-кнопок справа
 */
@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor,
                        surfaceColor.copy(alpha = 0f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Навигационная иконка слева
        if (navigationIcon != null) {
            IconButton(
                onClick  = onNavigationClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector        = navigationIcon,
                    contentDescription = "Назад",
                    tint               = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Заголовок — центр или с отступом если есть nav icon
        Text(
            text       = title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = if (navigationIcon != null) 56.dp else 12.dp,
                    end   = 56.dp
                )
        )

        // Actions справа
        Row(
            modifier          = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content           = actions
        )
    }
}
