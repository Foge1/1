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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import java.util.Locale

private object OrderDetailDefaults {
    val ScreenHorizontalPadding = AppSpacing.lg
    val SectionSpacing = AppSpacing.md
    val ItemSpacing = AppSpacing.sm
    val CardPadding = AppSpacing.md
}

private data class OrderDetailActions(
    val onOpenChat: () -> Unit,
    val onApply: () -> Unit,
    val onWithdraw: () -> Unit,
    val onStart: () -> Unit,
    val onCancel: () -> Unit,
    val onComplete: () -> Unit,
    val onSelectApplicant: (String) -> Unit,
    val onUnselectApplicant: (String) -> Unit,
)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.order?.let { "Заказ #${it.order.id}" } ?: "Заказ") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) {
                        Text(text = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        OrderDetailContent(
            state = state,
            contentPadding = padding,
            actions =
                OrderDetailActions(
                    onOpenChat = { state.order?.let { onOpenChat(it.order.id) } },
                    onApply = viewModel::onApply,
                    onWithdraw = viewModel::onWithdraw,
                    onStart = viewModel::onStart,
                    onCancel = viewModel::onCancel,
                    onComplete = viewModel::onComplete,
                    onSelectApplicant = viewModel::onSelectApplicant,
                    onUnselectApplicant = viewModel::onUnselectApplicant,
                ),
        )
    }
}

@Composable
private fun OrderDetailContent(
    state: OrderDetailUiState,
    contentPadding: PaddingValues,
    actions: OrderDetailActions,
) {
    when {
        state.loading -> {
            LoadingState(contentPadding = contentPadding)
        }

        state.requiresUserSelection -> {
            InlineStateMessage(
                text = "Выберите пользователя",
                contentPadding = contentPadding,
            )
        }

        state.errorMessage != null && state.order == null -> {
            InlineStateMessage(
                text = state.errorMessage.orEmpty(),
                contentPadding = contentPadding,
            )
        }

        state.order != null -> {
            OrderDetailSections(
                order = state.order,
                inProgress = state.isActionInProgress,
                contentPadding = contentPadding,
                actions = actions,
            )
        }
    }
}

@Composable
private fun LoadingState(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun InlineStateMessage(
    text: String,
    contentPadding: PaddingValues,
) {
    Text(
        text = text,
        modifier =
            Modifier
                .padding(contentPadding)
                .padding(OrderDetailDefaults.ScreenHorizontalPadding),
    )
}

@Composable
private fun OrderDetailSections(
    order: OrderUiModel,
    inProgress: Boolean,
    contentPadding: PaddingValues,
    actions: OrderDetailActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
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
            item { ApplicantsSectionHeader(order = order) }
        }
        applicantSection(
            order = order,
            onSelectApplicant = actions.onSelectApplicant,
            onUnselectApplicant = actions.onUnselectApplicant,
        )
        item {
            ActionPanel(
                model = order,
                inProgress = inProgress,
                onOpenChat = actions.onOpenChat,
                onApply = actions.onApply,
                onWithdraw = actions.onWithdraw,
                onStart = actions.onStart,
                onCancel = actions.onCancel,
                onComplete = actions.onComplete,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.applicantSection(
    order: OrderUiModel,
    onSelectApplicant: (String) -> Unit,
    onUnselectApplicant: (String) -> Unit,
) {
    if (!order.canSelect && !order.canUnselect) {
        return
    }

    items(order.visibleApplicants, key = { it.loaderId }) { applicant ->
        ApplicantRow(
            applicant = applicant,
            canSelect = order.canSelect,
            canUnselect = order.canUnselect,
            onSelect = { onSelectApplicant(applicant.loaderId) },
            onUnselect = { onUnselectApplicant(applicant.loaderId) },
        )
    }
}

@Composable
private fun HeroCard(order: OrderUiModel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            Text(
                text = order.order.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = order.order.address,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Статус: ${order.order.status.toUiLabel()}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DetailsGrid(order: OrderUiModel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            DetailGridRow(
                label = "Ставка",
                value = order.order.pricePerHour.toPriceLabel(),
            )
            DetailGridRow(
                label = "Рабочие",
                value = "${order.order.workersCurrent}/${order.order.workersTotal}",
            )
            DetailGridRow(
                label = "Отклики",
                value = order.appliedApplicantsCount.toString(),
            )
        }
    }
}

@Composable
private fun DetailGridRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun ApplicantsSectionHeader(order: OrderUiModel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Кандидаты: ${order.visibleApplicants.size}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Выбрано: ${order.selectedApplicantsCount}",
                style = MaterialTheme.typography.bodyMedium,
            )
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
            PrimaryActionButton(
                visible = model.canOpenChat,
                text = "Открыть чат",
                enabled = !inProgress,
                onClick = onOpenChat,
            )
            PrimaryActionButton(
                visible = model.canApply,
                text = "Откликнуться",
                enabled = !inProgress,
                onClick = onApply,
            )
            SecondaryActionButton(
                visible = model.canWithdraw,
                text = "Отозвать отклик",
                enabled = !inProgress,
                onClick = onWithdraw,
            )
            StartActionButton(
                model = model,
                enabled = !inProgress,
                onStart = onStart,
            )
            CompleteActionButton(
                model = model,
                enabled = !inProgress,
                onComplete = onComplete,
            )
            SecondaryActionButton(
                visible = model.canCancel,
                text = "Отменить",
                enabled = !inProgress,
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun StartActionButton(
    model: OrderUiModel,
    enabled: Boolean,
    onStart: () -> Unit,
) {
    PrimaryActionButton(
        visible = model.canStart && model.order.status == OrderStatus.STAFFING,
        text = "Начать",
        enabled = enabled,
        onClick = onStart,
    )
}

@Composable
private fun CompleteActionButton(
    model: OrderUiModel,
    enabled: Boolean,
    onComplete: () -> Unit,
) {
    PrimaryActionButton(
        visible = model.canComplete && model.order.status == OrderStatus.IN_PROGRESS,
        text = "Завершить",
        enabled = enabled,
        onClick = onComplete,
    )
}

@Composable
private fun PrimaryActionButton(
    visible: Boolean,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}

@Composable
private fun SecondaryActionButton(
    visible: Boolean,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}

@Composable
private fun ApplicantRow(
    applicant: OrderApplication,
    canSelect: Boolean,
    canUnselect: Boolean,
    onSelect: () -> Unit,
    onUnselect: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(OrderDetailDefaults.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OrderDetailDefaults.ItemSpacing),
        ) {
            Text(
                text = "Грузчик: ${applicant.loaderId}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Статус отклика: ${applicant.status.toUiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (canSelect && applicant.status == OrderApplicationStatus.APPLIED) {
                Button(onClick = onSelect) {
                    Text(text = "Выбрать")
                }
            }
            if (canUnselect && applicant.status == OrderApplicationStatus.SELECTED) {
                OutlinedButton(onClick = onUnselect) {
                    Text(text = "Снять")
                }
            }
        }
    }
}

private fun OrderStatus.toUiLabel(): String =
    when (this) {
        OrderStatus.STAFFING -> "Набор"
        OrderStatus.IN_PROGRESS -> "В работе"
        OrderStatus.COMPLETED -> "Завершён"
        OrderStatus.CANCELED -> "Отменён"
        OrderStatus.EXPIRED -> "Истёк"
    }

private fun OrderApplicationStatus.toUiLabel(): String =
    when (this) {
        OrderApplicationStatus.APPLIED -> "Откликнулся"
        OrderApplicationStatus.SELECTED -> "Выбран"
        OrderApplicationStatus.REJECTED -> "Отклонён"
        OrderApplicationStatus.WITHDRAWN -> "Отозван"
    }

private fun Double.toPriceLabel(): String = String.format(Locale.US, "%.0f ₽/ч", this)
