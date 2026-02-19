package com.loaderapp.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.components.*
import com.loaderapp.ui.main.LocalBottomNavHeight

@Composable
fun HistoryScreen(
    userId: Long,
    userRole: UserRoleModel,
    onOrderClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(userId, userRole) { viewModel.initialize(userId, userRole) }

    val historyState by viewModel.historyState.collectAsState()

    AppScaffold(title = "История") {
        val topBarHeightPx  = LocalTopBarHeightPx.current
        val density         = LocalDensity.current
        val topBarHeight    = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        when (val state = historyState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error   -> ErrorView(message = state.message)
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyStateView(
                        icon    = Icons.Default.History,
                        title   = "История пуста",
                        message = "Завершённые и отменённые заказы появятся здесь"
                    )
                } else {
                    FadingEdgeLazyColumn(
                        modifier         = Modifier.fillMaxSize(),
                        topFadeHeight    = 0.dp,
                        bottomFadeHeight = 36.dp,
                        contentPadding   = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            top    = topBarHeight + 8.dp,
                            bottom = bottomNavHeight + 48.dp
                        )
                    ) {
                        items(state.data, key = { it.id }) { order ->
                            OrderCard(
                                order   = order,
                                onClick = { onOrderClick(order.id) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            is UiState.Idle -> Unit
        }
    }
}
