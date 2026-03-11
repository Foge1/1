package com.loaderapp.features.orders.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus

private object OrderDetailDefaults {
    val ScreenHorizontalPadding = AppSpacing.lg
    val SectionSpacing = AppSpacing.md
    val ItemSpacing = AppSpacing.sm
    val CardPadding = 14.dp
}

object OrderDetailRoute {
    const val ORDER_ID_ARG = "orderId"
    const val ROUTE_PATTERN = "feature-orders/order/{$ORDER_ID_ARG}"

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
        route = OrderDetailRoute.ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(OrderDetailRoute.ORDER_ID_ARG) {
                    type = NavType.LongType
                },
            ),
    ) {
        val viewModel: OrderDetailViewModel = hiltViewModel()
        OrderDetailScreen(
            viewModel = viewModel,
            onBack = onBack,
            onOpenChat = onOpenChat,
        )
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
                title = { Text(text = if (order == null) "Заказ" else "Заказ #${order.order.id}") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.requiresUserSelection -> {
                Text(
                    text = "Выберите пользователя",
                    modifier = Modifier.padding(padding).padding(OrderDetailDefaults.ScreenHorizontalPadding),
                )
            }

            state.errorMessage != null && order == null -> {
                Text(
                    text = state.errorMessage.orEmpty(),
                    modifier = Modifier.padding(padding).padding(OrderDetailDefaults.ScreenHorizontalPadding),
                )
            }

            order != null -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.SectionSpacing),
                    contentPadding =
                        PaddingValues(
                            horizontal = OrderDetailDefaults.ScreenHorizontalPadding,
                            vertical = OrderDetailDefaults.ScreenHorizontalPadding,
                        ),
                ) {
                    item { HeroCard(order = order) }
                    item { DetailsGrid(order = order) }
                    if (order.visibleApplicants.isNotEmpty()) {
                        item {
                            ApplicantRow(
                                count = order.visibleApplicants.size,
                                selected = order.selectedApplicantsCount,
                            )
                        }
                    }
                    item {
                        ActionPanel(
                            model = order,
                            inProgress = state.isActionInProgress,
                            onOpenChat = { onOpenChat(order.order.id) },
                            onApply = viewModel::onApply,
                            onWithdraw = viewModel::onWithdraw,
                            onStart = viewModel::onStart,
                            onCancel = viewModel::onCancel,
                            onComplete = viewModel::onComplete,
                        )
                    }
                    if (order.canSelect || order.canUnselect) {
                        items(order.visibleApplicants, key = { it.loaderId }) { applicant ->
                            ApplicantItem(
                                loaderId = applicant.loaderId,
                                status = applicant.status,
                                canSelect = order.canSelect,
                                canUnselect = order.canUnselect,
                                onSelect = { viewModel.onSelectApplicant(applicant.loaderId) },
                                onUnselect = { viewModel.onUnselectApplicant(applicant.loaderId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(order: OrderUiModel) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            Text(order.order.title, style = MaterialTheme.typography.titleLarge)
            Text(order.order.address, style = MaterialTheme.typography.bodyMedium)
            Text("Статус: ${order.order.status}", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DetailsGrid(order: OrderUiModel) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ставка")
                Text("${order.order.pricePerHour}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Рабочие")
                Text("${order.order.workersCurrent}/${order.order.workersTotal}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Отклики")
                Text(order.appliedApplicantsCount.toString())
            }
        }
    }
}

@Composable
private fun ApplicantRow(
    count: Int,
    selected: Int,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Кандидаты: $count")
            Text("Выбрано: $selected")
        }
    }
}

@Composable
private fun ActionPanel(
    model: OrderUiModel,
    inProgress: Boolean,
    onOpenChat: () -> Unit,
    onApply: () -> Unit,
    onWithdraw: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            if (model.canOpenChat) {
                Button(onClick = onOpenChat, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть чат")
                }
            }
            if (model.canApply) {
                Button(onClick = onApply, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Откликнуться")
                }
            }
            if (model.canWithdraw) {
                OutlinedButton(onClick = onWithdraw, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Отозвать отклик")
                }
            }
            if (model.canStart && model.order.status == OrderStatus.STAFFING) {
                Button(onClick = onStart, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Начать")
                }
            }
            if (model.canComplete && model.order.status == OrderStatus.IN_PROGRESS) {
                Button(onClick = onComplete, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Завершить")
                }
            }
            if (model.canCancel) {
                OutlinedButton(onClick = onCancel, enabled = !inProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Отменить")
                }
            }
        }
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
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            Text(text = "Грузчик: $loaderId")
            Text(text = "Статус отклика: $status")

            if (canSelect && status == OrderApplicationStatus.APPLIED) {
                Button(onClick = onSelect) {
                    Text("Выбрать")
                }
            }
            if (canUnselect && status == OrderApplicationStatus.SELECTED) {
                OutlinedButton(onClick = onUnselect) {
                    Text("Снять")
                }
            }
        }
    }
}
