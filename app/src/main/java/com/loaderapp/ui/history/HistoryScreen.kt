package com.loaderapp.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран истории заказов.
 * Заглушка — готова к наполнению функционалом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("История") }) }
    ) { padding ->
        PlaceholderContent(
            icon     = Icons.Default.History,
            title    = "История заказов",
            subtitle = "Здесь будет отображаться история выполненных заказов",
            modifier = Modifier.padding(padding)
        )
    }
}
