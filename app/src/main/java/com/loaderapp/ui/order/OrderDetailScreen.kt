package com.loaderapp.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.loaderapp.ui.theme.orderTitleLargeStyle

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
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
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
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.order_spacing_8))
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
                Button(
                    onClick = { onOpenChat(order.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.order_action_button_height)),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.order_action_button_radius))
                ) {
                    Icon(
                        Icons.Rounded.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
                    )
                    Spacer(Modifier.size(dimensionResource(id = R.dimen.order_spacing_8)))
                    Text("Чат", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(order: OrderModel) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_12))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            OrderStatusChip(status = order.status)
            Text(
                "${order.pricePerHour.toInt()} ₽/ч",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            order.address,
            style = orderTitleLargeStyle(),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
            )
            Text(
                formatOrderDateTime(order.dateTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkParamsSection(order: OrderModel, workerCount: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8))
    ) {
        ParameterChip(Icons.Rounded.Inventory2, order.cargoDescription)
        ParameterChip(Icons.Rounded.Timer, "Минимум ${order.estimatedHours} часа")
        ParameterChip(Icons.Rounded.Groups, "$workerCount / ${order.requiredWorkers} грузчиков")
    }
}

@Composable
private fun CommentSection(comment: String) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8))) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8)), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Rounded.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
            )
            Text(comment, style = orderBodyStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ParameterChip(icon: ImageVector, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.order_spacing_12),
                vertical = dimensionResource(id = R.dimen.order_spacing_8)
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_spacing_8))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
