package com.loaderapp.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.ui.common.asString
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderCTA
import com.loaderapp.ui.components.OrderCardHeader
import com.loaderapp.ui.components.OrderCardTitle
import com.loaderapp.ui.components.OrderComment
import com.loaderapp.ui.components.OrderDateTimeRow
import com.loaderapp.ui.components.OrderMetaChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit
) {
    val orderState by viewModel.orderState.collectAsState()
    val workerCount by viewModel.workerCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (orderState) {
                            is UiState.Success -> "Заказ #${(orderState as UiState.Success<OrderModel>).data.id}"
                            else -> "Заказ"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = orderState) {
            is UiState.Loading -> LoadingView(message = "Загрузка заказа...")
            is UiState.Error -> ErrorView(message = state.message.asString(), onRetry = null)
            is UiState.Success -> OrderDetailsContent(
                order = state.data,
                workerCount = workerCount,
                onPrimaryAction = { onOpenChat(state.data.id) },
                modifier = Modifier.padding(padding)
            )

            is UiState.Idle -> Unit
        }
    }
}

@Composable
private fun OrderDetailsContent(
    order: OrderModel,
    workerCount: Int,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canOpenChat = order.status == OrderStatusModel.TAKEN || order.status == OrderStatusModel.IN_PROGRESS

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrderHeroBlock(order = order, workerCount = workerCount)
        OrderDetailsSection(order = order, workerCount = workerCount)
        OrderDetailsActions(
            order = order,
            canOpenChat = canOpenChat,
            onPrimaryAction = onPrimaryAction
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OrderHeroBlock(order: OrderModel, workerCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OrderCardHeader(order = order)
            OrderCardTitle(order = order)
            OrderDateTimeRow(order = order)
            OrderMetaChips(order = order, workerCount = workerCount)
            if (order.comment.isNotBlank()) {
                OrderComment(comment = order.comment)
            }
        }
    }
}

@Composable
private fun OrderDetailsSection(order: OrderModel, workerCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Детали",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        OrderDetailItemCard(title = "Что везём", icon = Icons.Rounded.LocalShipping, value = order.cargoDescription)
        OrderDetailItemCard(title = "Кол-во людей", icon = Icons.Rounded.Groups, value = "$workerCount/${order.requiredWorkers}")
        OrderDetailItemCard(title = "Длительность", icon = Icons.Rounded.Timer, value = "${order.estimatedHours}ч мин")
        OrderDetailItemCard(title = "Мин. рейтинг", icon = Icons.Rounded.Star, value = "${order.minWorkerRating}")
    }
}

@Composable
private fun OrderDetailItemCard(title: String, icon: ImageVector, value: String) {
    if (value.isBlank()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsActions(
    order: OrderModel,
    canOpenChat: Boolean,
    onPrimaryAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OrderCTA(
                order = order,
                onClick = onPrimaryAction,
                enabled = canOpenChat
            )
        }
    }
}
