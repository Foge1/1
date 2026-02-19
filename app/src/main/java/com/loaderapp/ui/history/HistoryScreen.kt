package com.loaderapp.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран истории заказов.
 * Заглушка с градиентным фоном — готова к наполнению функционалом.
 */
@Composable
fun HistoryScreen() {
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
