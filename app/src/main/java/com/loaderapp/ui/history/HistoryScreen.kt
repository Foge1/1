package com.loaderapp.ui.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.PlaceholderContent

@Composable
fun HistoryScreen(
    userId: Long,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { viewModel.initialize(userId) }

    AppScaffold(title = "История") {
        PlaceholderContent(
            icon     = Icons.Default.History,
            title    = "История заказов",
            subtitle = "Здесь будет отображаться история выполненных заказов",
            modifier = Modifier.fillMaxSize()
        )
    }
}
