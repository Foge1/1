package com.loaderapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    userId: Long,
    onMenuClick: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyState by viewModel.historyState.collectAsState()

    LaunchedEffect(userId) { viewModel.initialize(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История заказов") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = historyState) {
            is UiState.Loading -> LoadingView(message = "Загрузка истории...")
            is UiState.Error   -> ErrorView(message = state.message, onRetry = null)
            is UiState.Success -> HistoryContent(orders = state.data, modifier = Modifier.padding(padding))
            is UiState.Idle    -> Unit
        }
    }
}

@Composable
private fun HistoryContent(orders: List<OrderModel>, modifier: Modifier = Modifier) {
    if (orders.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("История пуста", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Завершённые заказы появятся здесь", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }

    val completedOrders = orders.filter { it.status == OrderStatusModel.COMPLETED }
    val totalEarned = completedOrders.sumOf { it.totalPrice }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (totalEarned > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Всего выполнено", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${completedOrders.size} заказов", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Заработано", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalEarned.toInt()} ₽", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        items(orders, key = { it.id }) { order ->
            HistoryOrderCard(order = order)
        }
    }
}

@Composable
private fun HistoryOrderCard(order: OrderModel) {
    val dateFormat  = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val accentColor = when (order.status) {
        OrderStatusModel.COMPLETED -> MaterialTheme.colorScheme.secondary
        OrderStatusModel.CANCELLED -> MaterialTheme.colorScheme.error
        else                       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp), shape = MaterialTheme.shapes.small) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    HistoryStatusChip(order.status)
                }
                Spacer(Modifier.height(6.dp))
                Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.status == OrderStatusModel.COMPLETED && order.estimatedHours > 0) {
                        Text(" · ${order.totalPrice.toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusChip(status: OrderStatusModel) {
    val (text, color) = when (status) {
        OrderStatusModel.COMPLETED -> "Завершён" to MaterialTheme.colorScheme.secondary
        OrderStatusModel.CANCELLED -> "Отменён"  to MaterialTheme.colorScheme.error
        else                       -> "—"         to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
