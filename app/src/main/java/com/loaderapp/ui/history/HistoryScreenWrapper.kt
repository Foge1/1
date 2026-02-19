package com.loaderapp.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.data.mapper.OrderMapper
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView

/**
 * Вкладка «История».
 * Данные получает через HistoryViewModel → UseCase → Repository.
 * Нет прямых обращений к AppRepository или LocalContext.
 */
@Composable
fun HistoryScreen(userId: Long, isDispatcher: Boolean = false) {
    val viewModel: HistoryViewModel = hiltViewModel()

    LaunchedEffect(userId) { viewModel.initialize(userId, isDispatcher) }

    val state by viewModel.ordersState.collectAsState()

    when (state) {
        is UiState.Loading, UiState.Idle -> LoadingView()
        is UiState.Error -> ErrorView(message = (state as UiState.Error).message)
        is UiState.Success -> {
            val domainOrders = (state as UiState.Success).data
            // Маппим domain → data модель для существующего HistoryScreen
            val dataOrders = domainOrders.map { OrderMapper.toEntity(it) }
            HistoryScreen(
                orders = dataOrders,
                onMenuClick = {},
                onBackClick = {}
            )
        }
        else -> {}
    }
}
