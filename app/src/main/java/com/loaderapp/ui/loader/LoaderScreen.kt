package com.loaderapp.ui.loader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.usecase.order.WorkerStats
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран грузчика (новая версия с UiState и domain моделями)
 * 
 * Показывает:
 * - Доступные заказы (вкладка "Доступные")
 * - Мои активные заказы (вкладка "Мои заказы")
 * - Статистику грузчика
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val availableOrdersState by viewModel.availableOrdersState.collectAsState()
    val myOrdersState by viewModel.myOrdersState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Обработка Snackbar сообщений
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        topBar = {
            LoaderTopBar(
                statsState = statsState,
                onRefresh = { viewModel.refresh() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Вкладки
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Доступные") },
                    icon = { Icon(Icons.Default.Search, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Мои заказы") },
                    icon = { Icon(Icons.Default.Work, null) }
                )
            }
            
            // Контент вкладок с Pull-to-Refresh
            val pullRefreshState = rememberPullRefreshState(
                refreshing = isRefreshing,
                onRefresh = { viewModel.refresh() }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when (selectedTab) {
                    0 -> AvailableOrdersTab(
                        state = availableOrdersState,
                        onOrderClick = onOrderClick,
                        onTakeOrder = { order ->
                            viewModel.takeOrder(order) {
                                selectedTab = 1 // Переключаемся на "Мои заказы"
                            }
                        }
                    )
                    1 -> MyOrdersTab(
                        state = myOrdersState,
                        onOrderClick = onOrderClick,
                        onCompleteOrder = { order ->
                            viewModel.completeOrder(order)
                        }
                    )
                }
                
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * TopBar с статистикой грузчика
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoaderTopBar(
    statsState: UiState<WorkerStats>,
    onRefresh: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Column {
                Text(
                    text = "Грузчик",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Показываем статистику
                when (statsState) {
                    is UiState.Success -> {
                        Text(
                            text = "${statsState.data.completedOrders} выполнено • ${statsState.data.totalEarnings.toInt()}₽ • ⭐ ${String.format("%.1f", statsState.data.averageRating)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "Загрузка статистики...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Обновить")
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Вкладка доступных заказов
 */
@Composable
private fun AvailableOrdersTab(
    state: UiState<List<OrderModel>>,
    onOrderClick: (Long) -> Unit,
    onTakeOrder: (OrderModel) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            LoadingView()
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "Нет доступных заказов",
                    message = "Пока нет новых заказов. Обновите страницу позже."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            actionButton = {
                                Button(
                                    onClick = { onTakeOrder(order) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Взять заказ")
                                }
                            }
                        )
                    }
                }
            }
        }
        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { /* Refresh handled by pull-to-refresh */ }
            )
        }
        is UiState.Idle -> {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "Поиск заказов",
                message = "Потяните вниз для обновления"
            )
        }
    }
}

/**
 * Вкладка моих заказов
 */
@Composable
private fun MyOrdersTab(
    state: UiState<List<OrderModel>>,
    onOrderClick: (Long) -> Unit,
    onCompleteOrder: (OrderModel) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            LoadingView()
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.WorkOff,
                    title = "Нет активных заказов",
                    message = "Возьмите заказ из вкладки 'Доступные'"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            actionButton = if (order.status == OrderStatusModel.TAKEN || order.status == OrderStatusModel.IN_PROGRESS) {
                                {
                                    Button(
                                        onClick = { onCompleteOrder(order) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Завершить")
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
        is UiState.Error -> {
            ErrorView(message = state.message)
        }
        is UiState.Idle -> {
            EmptyStateView(
                icon = Icons.Default.Work,
                title = "Мои заказы",
                message = "Здесь будут ваши активные заказы"
            )
        }
    }
}

/**
 * Карточка заказа
 */
@Composable
private fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    actionButton: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                StatusChip(status = order.status)
            }
            
            // Описание груза
            Text(
                text = order.cargoDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Дата и время
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDateTime(order.dateTime),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Параметры заказа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OrderParam(
                    icon = Icons.Default.Person,
                    value = "${order.requiredWorkers} чел"
                )
                OrderParam(
                    icon = Icons.Default.Schedule,
                    value = "${order.estimatedHours} ч"
                )
                OrderParam(
                    icon = Icons.Default.AttachMoney,
                    value = "${order.pricePerHour}₽/ч"
                )
                OrderParam(
                    icon = Icons.Default.Star,
                    value = "≥${order.minWorkerRating}"
                )
            }
            
            // Кнопка действия
            actionButton?.invoke()
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatusModel) {
    val (text, color) = when (status) {
        OrderStatusModel.AVAILABLE -> "Доступен" to Color(0xFF4CAF50)
        OrderStatusModel.TAKEN -> "Взят" to Color(0xFFFF9800)
        OrderStatusModel.IN_PROGRESS -> "В работе" to Color(0xFF2196F3)
        OrderStatusModel.COMPLETED -> "Завершён" to Color(0xFF9C27B0)
        OrderStatusModel.CANCELLED -> "Отменён" to Color(0xFFF44336)
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OrderParam(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("ru"))
    return sdf.format(Date(timestamp))
}
