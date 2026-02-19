package com.loaderapp.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран истории заказов.
 *
 * Получает состояние из [HistoryViewModel] (Hilt).
 * Заглушка — готова к наполнению: заменить [PlaceholderContent]
 * на список из [viewModel.historyState].
 *
 * @param userId ID текущего пользователя для загрузки истории
 */
@Composable
fun HistoryScreen(
    userId: Long,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { viewModel.initialize(userId) }

    GradientBackground {
        GradientTopBar(title = "История")

        PlaceholderContent(
            icon     = Icons.Default.History,
            title    = "История заказов",
            subtitle = "Здесь будет отображаться история выполненных заказов",
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp)
        )
    }
}
