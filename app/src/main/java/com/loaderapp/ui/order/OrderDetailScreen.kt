package com.loaderapp.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderCardHeader
import com.loaderapp.ui.components.OrderCardTitle
import com.loaderapp.ui.components.OrderComment
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
            .padding(16.dp)
    ) {
        OrderDetailsCard(
            order = order,
            workerCount = workerCount,
            canOpenChat = canOpenChat,
            onOpenChat = onOpenChat
        )
    }
}

@Composable
private fun OrderDetailsCard(
    order: OrderModel,
    workerCount: Int,
    canOpenChat: Boolean,
    onOpenChat: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrderCardHeader(order = order)
            OrderCardTitle(order = order)
            OrderMetaChips(order = order, workerCount = workerCount)
            if (order.comment.isNotBlank()) {
                OrderComment(comment = order.comment)
            }
            Button(
                onClick = { onOpenChat(order.id) },
                enabled = canOpenChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(imageVector = Icons.Rounded.Chat, contentDescription = null)
                Text(text = "Чат", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
