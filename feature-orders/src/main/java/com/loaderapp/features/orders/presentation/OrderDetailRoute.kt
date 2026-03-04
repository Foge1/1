package com.loaderapp.features.orders.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus

object OrderDetailRoute {
    const val ORDER_ID_ARG = "orderId"
    const val routePattern = "feature-orders/order/{$ORDER_ID_ARG}"

    fun createRoute(orderId: Long): String = "feature-orders/order/$orderId"
}

fun NavController.navigateToOrderDetail(orderId: Long) {
    navigate(OrderDetailRoute.createRoute(orderId))
}

fun NavGraphBuilder.orderDetailRoute(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
) {
    composable(
        route = OrderDetailRoute.routePattern,
        arguments = listOf(navArgument(OrderDetailRoute.ORDER_ID_ARG) { type = NavType.LongType }),
    ) {
        val viewModel: OrderDetailViewModel = hiltViewModel()
        OrderDetailScreen(viewModel = viewModel, onBack = onBack, onOpenChat = onOpenChat)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val order = state.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (order == null) "Заказ" else "Заказ #${order.order.id}") },
                navigationIcon = { Button(onClick = onBack, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Назад") } },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
            state.requiresUserSelection -> Text("Выберите пользователя", modifier = Modifier.padding(16.dp).padding(padding))
            state.errorMessage != null && order == null -> Text(state.errorMessage.orEmpty(), modifier = Modifier.padding(16.dp).padding(padding))
            order != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(order.order.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(order.order.address)
                        Text("Статус: ${order.order.status}")
                        Text("Отклики: ${order.appliedApplicantsCount}, выбрано: ${order.selectedApplicantsCount}")
                    }
                    item {
                        if (order.canOpenChat) {
                            Button(onClick = { onOpenChat(order.order.id) }) { Text("Открыть чат") }
                        }
                    }
                    item {
                        OrderPrimaryActions(
                            model = order,
                            inProgress = state.isActionInProgress,
                            onApply = viewModel::onApply,
                            onWithdraw = viewModel::onWithdraw,
                            onStart = viewModel::onStart,
                            onCancel = { viewModel.onCancel() },
                            onComplete = viewModel::onComplete,
                        )
                    }
                    if (order.canSelect || order.canUnselect) {
                        items(order.visibleApplicants, key = { it.loaderId }) { applicant ->
                            ApplicantItem(
                                loaderId = applicant.loaderId,
                                status = applicant.status,
                                onSelect = { viewModel.onSelectApplicant(applicant.loaderId) },
                                onUnselect = { viewModel.onUnselectApplicant(applicant.loaderId) },
                                canSelect = order.canSelect,
                                canUnselect = order.canUnselect,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderPrimaryActions(
    model: OrderUiModel,
    inProgress: Boolean,
    onApply: () -> Unit,
    onWithdraw: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (model.canApply) Button(onClick = onApply, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) { Text("Откликнуться") }
        if (model.canWithdraw) Button(onClick = onWithdraw, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) { Text("Отозвать отклик") }
        if (model.canStart && model.order.status == OrderStatus.STAFFING) Button(onClick = onStart, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) { Text("Начать") }
        if (model.canComplete && model.order.status == OrderStatus.IN_PROGRESS) Button(onClick = onComplete, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) { Text("Завершить") }
        if (model.canCancel) Button(onClick = onCancel, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) { Text("Отменить") }
    }
}

@Composable
private fun ApplicantItem(
    loaderId: String,
    status: OrderApplicationStatus,
    canSelect: Boolean,
    canUnselect: Boolean,
    onSelect: () -> Unit,
    onUnselect: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Грузчик: $loaderId")
        Text("Статус отклика: $status")
        if (canSelect && status == OrderApplicationStatus.APPLIED) {
            Button(onClick = onSelect) { Text("Выбрать") }
        }
        if (canUnselect && status == OrderApplicationStatus.SELECTED) {
            Button(onClick = onUnselect) { Text("Снять") }
        }
    }
}
