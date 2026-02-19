package com.loaderapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Единый TopBar для всего приложения.
 *
 * Полностью прозрачный фон — TopBar «плавает» поверх градиентного
 * контента экрана. Цвета шрифта и иконок берутся из [MaterialTheme.colorScheme.onSurface]
 * и одинаково читаемы на светлом градиентном фоне.
 *
 * Почему убран gradient-фон самого TopBar:
 * На экране Профиля контент скроллится, и градиент GradientTopBar (surface→transparent)
 * накладывался поверх градиента контента (primaryContainer→background), создавая
 * видимую «полосу». Правильное решение — TopBar прозрачный, единственный
 * источник цвета фона — сам контент/GradientBackground.
 *
 * @param title              Заголовок экрана
 * @param navigationIcon     Иконка назад (null = не показывать)
 * @param onNavigationClick  Обработчик клика по navigationIcon
 * @param actions            Слот для action-кнопок справа
 */
@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
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

        Row(
            modifier          = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content           = actions
        )
    }
}
