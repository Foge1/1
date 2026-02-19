package com.loaderapp.ui.rating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.presentation.rating.RatingViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView

/**
 * Вкладка «Рейтинг».
 * Данные получает через RatingViewModel → UseCases → Repositories.
 */
@Composable
fun RatingScreen(userId: Long, isDispatcher: Boolean = false) {
    val viewModel: RatingViewModel = hiltViewModel()

    LaunchedEffect(userId) { viewModel.initialize(userId, isDispatcher) }

    val state by viewModel.state.collectAsState()

    when (state) {
        is UiState.Loading, UiState.Idle -> LoadingView()
        is UiState.Error -> ErrorView(message = (state as UiState.Error).message)
        is UiState.Success -> {
            val data = (state as UiState.Success).data
            RatingScreen(
                userName = data.user.name,
                userRating = data.user.rating,
                onMenuClick = {},
                onBackClick = {},
                completedCount = data.workerStats?.completedOrders ?: 0,
                totalEarnings = data.workerStats?.totalEarnings ?: 0.0,
                averageRating = data.workerStats?.averageRating ?: data.user.rating.toFloat(),
                dispatcherCompletedCount = data.dispatcherStats?.completedOrders ?: 0,
                dispatcherActiveCount = data.dispatcherStats?.activeOrders ?: 0,
                isDispatcher = isDispatcher
            )
        }
        else -> {}
    }
}
