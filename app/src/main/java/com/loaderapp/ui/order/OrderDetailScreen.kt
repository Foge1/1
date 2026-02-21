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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.loaderapp.ui.theme.addressTextStyle
import com.loaderapp.ui.theme.dateTextStyle
import com.loaderapp.ui.theme.metaTextStyle
import com.loaderapp.ui.theme.rateTextStyle

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
            .padding(dimensionResource(id = R.dimen.order_detail_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_detail_block_spacing))
    ) {
        UnifiedDetailsContainer(order = order, workerCount = workerCount)

        if (canOpenChat) {
            Button(onClick = { onOpenChat(order.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Chat, contentDescription = null)
                Spacer(Modifier.size(dimensionResource(id = R.dimen.order_card_inner_spacing)))
                Text("Чат")
            }
        }
    }
}

@Composable
private fun UnifiedDetailsContainer(order: OrderModel, workerCount: Int) {
    val cornerRadius = dimensionResource(id = R.dimen.order_card_corner_radius)
    val blockSpacing = dimensionResource(id = R.dimen.order_detail_block_spacing)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = blockSpacing / 2)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.order_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_card_inner_spacing))
        ) {
            SectionTitle("Заголовок заказа")
            OrderStatusChip(status = order.status)
            Text(order.address, style = addressTextStyle())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)))
                Spacer(Modifier.size(dimensionResource(id = R.dimen.order_card_inner_spacing) / 2))
                Text(formatOrderDateTime(order.dateTime), style = dateTextStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${order.pricePerHour.toInt()} ₽/ч", style = rateTextStyle())

            SectionDivider()

            SectionTitle("Параметры работы")
            DetailRow(Icons.Default.Inventory2, "Груз", order.cargoDescription)
            DetailRow(Icons.Default.Timer, "Время работы", "${order.estimatedHours} ч")
            DetailRow(Icons.Default.Group, "Нужно грузчиков", "$workerCount / ${order.requiredWorkers}")

            SectionDivider()

            SectionTitle("Требования")
            DetailRow(Icons.Default.Star, "Требуемый рейтинг", "${order.minWorkerRating}")

            if (order.comment.isNotBlank()) {
                SectionDivider()
                SectionTitle("Комментарий")
                DetailRow(Icons.Default.Comment, "Комментарий от диспетчера", order.comment)
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.order_card_inner_spacing) / 4),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_card_inner_spacing))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size))
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_card_inner_spacing) / 4)) {
            Text(text = label, style = metaTextStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = dateTextStyle())
        }
    }
}
