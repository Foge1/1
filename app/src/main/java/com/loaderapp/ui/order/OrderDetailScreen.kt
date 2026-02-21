package com.loaderapp.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.dimensionResource
import com.loaderapp.R
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderStatusChip
import com.loaderapp.ui.components.formatOrderDateTime
import com.loaderapp.ui.theme.orderBodyStyle
import com.loaderapp.ui.theme.orderLabelStyle
import com.loaderapp.ui.theme.orderTitleLargeStyle
import com.loaderapp.ui.theme.orderTitleMediumStyle

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
                            else -> "Детали заказа"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = orderState) {
            is UiState.Loading -> LoadingView(message = "Загрузка заказа...")
            is UiState.Error -> ErrorView(message = state.message, onRetry = null)
            is UiState.Success -> OrderDetailContent(
                order = state.data,
                workerCount = workerCount,
                onOpenChat = onOpenChat,
                modifier = Modifier.padding(padding)
            )
            is UiState.Idle -> Unit
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderModel,
    workerCount: Int,
    onOpenChat: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val canOpenChat = order.status == OrderStatusModel.TAKEN || order.status == OrderStatusModel.IN_PROGRESS

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.order_detail_screen_padding))
    ) {
        UnifiedDetailsContainer(order = order, workerCount = workerCount, canOpenChat = canOpenChat, onOpenChat = onOpenChat)
    }
}

@Composable
private fun UnifiedDetailsContainer(
    order: OrderModel,
    workerCount: Int,
    canOpenChat: Boolean,
    onOpenChat: (Long) -> Unit
) {
    val cornerRadius = dimensionResource(id = R.dimen.order_card_corner_radius)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.order_spacing_4))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.order_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_24))
        ) {
            HeaderSection(order = order)
            WorkParamsSection(order = order, workerCount = workerCount)
            if (order.comment.isNotBlank()) {
                CommentSection(comment = order.comment)
            }
            if (canOpenChat) {
                Button(onClick = { onOpenChat(order.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)))
                    Spacer(Modifier.size(dimensionResource(id = R.dimen.order_spacing_8)))
                    Text("Чат")
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(order: OrderModel) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_12))) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            OrderStatusChip(status = order.status)
            Text("${order.pricePerHour.toInt()} ₽/ч", style = orderTitleMediumStyle(), color = MaterialTheme.colorScheme.primary)
        }
        Text(order.address, style = orderTitleLargeStyle())
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8)), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
            )
            Text(
                formatOrderDateTime(order.dateTime),
                style = orderLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkParamsSection(order: OrderModel, workerCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_12))) {
        Text("Параметры работы", style = MaterialTheme.typography.titleMedium)
        DetailRow(Icons.Default.Inventory2, "Груз", order.cargoDescription)
        DetailRow(Icons.Default.Timer, "Минимум", "${order.estimatedHours} часов")
        DetailRow(Icons.Default.Groups, "Состав", "$workerCount / ${order.requiredWorkers} грузчиков")
    }
}

@Composable
private fun CommentSection(comment: String) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8))) {
        Text("Комментарий", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8)), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
            )
            Text(comment, style = orderBodyStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_4))) {
            Text(text = label, style = orderLabelStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = orderBodyStyle())
        }
    }
}
