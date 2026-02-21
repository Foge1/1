package com.loaderapp.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import com.loaderapp.R
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderStatusChip
import com.loaderapp.ui.theme.addressTextStyle
import com.loaderapp.ui.theme.dateTextStyle
import com.loaderapp.ui.theme.metaTextStyle
import com.loaderapp.ui.theme.rateTextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val canOpenChat = order.status == OrderStatusModel.TAKEN || order.status == OrderStatusModel.IN_PROGRESS

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.order_detail_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_detail_block_spacing))
    ) {
        HeroBlock(order = order, dateFormat = dateFormat)
        ParameterBlock(order = order, workerCount = workerCount)
        ConditionBlock(order = order)
        if (order.comment.isNotBlank()) {
            CommentBlock(comment = order.comment)
        }
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
private fun HeroBlock(order: OrderModel, dateFormat: SimpleDateFormat) {
    InfoSectionCard {
        OrderStatusChip(status = order.status)
        Text(order.address, style = addressTextStyle())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)))
            Spacer(Modifier.size(dimensionResource(id = R.dimen.order_card_inner_spacing)))
            Text(dateFormat.format(Date(order.dateTime)), style = dateTextStyle())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PriceHighlight(label = "Ставка", value = "${order.pricePerHour.toInt()} ₽/ч")
            PriceHighlight(label = "Итого", value = "${order.totalPrice.toInt()} ₽")
        }
    }
}

@Composable
private fun ParameterBlock(order: OrderModel, workerCount: Int) {
    InfoSectionCard(title = "Параметры работы") {
        DetailRow(Icons.Default.Inventory2, "Груз", order.cargoDescription)
        DetailRow(Icons.Default.Timer, "Длительность", "${order.estimatedHours} ч")
        DetailRow(Icons.Default.Group, "Грузчики", "$workerCount / ${order.requiredWorkers}")
    }
}

@Composable
private fun ConditionBlock(order: OrderModel) {
    InfoSectionCard(title = "Условия") {
        DetailRow(Icons.Default.Star, "Минимальный рейтинг", "${order.minWorkerRating}")
    }
}

@Composable
private fun CommentBlock(comment: String) {
    InfoSectionCard(title = "Комментарий") {
        DetailRow(Icons.Default.Comment, "Комментарий от диспетчера", comment)
    }
}

@Composable
private fun InfoSectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.order_card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.order_detail_block_spacing) / 6)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.order_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_card_inner_spacing))
        ) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun PriceHighlight(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.order_card_inner_spacing) / 2)
    ) {
        Text(text = label, style = metaTextStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)))
            Spacer(Modifier.size(dimensionResource(id = R.dimen.order_card_inner_spacing) / 2))
            Text(text = value, style = rateTextStyle())
        }
    }
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
