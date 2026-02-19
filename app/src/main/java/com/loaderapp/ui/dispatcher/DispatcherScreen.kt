package com.loaderapp.ui.dispatcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.usecase.order.DispatcherStats
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран диспетчера (новая версия с UiState)
 * 
 * Функции:
 * - Просмотр всех заказов
 * - Создание нового заказа
 * - Поиск заказов
 * - Отмена заказа
 * - Просмотр статистики
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalMaterialApi::class)
@Composable
fun DispatcherScreen(
    viewModel: DispatcherViewModel,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val ordersState by viewModel.ordersState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Обработка Snackbar сообщений
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        topBar = {
            DispatcherTopBar(
                statsState = statsState,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { viewModel.setSearchActive(it) },
                onSearchQueryChange = { viewModel.updateSearchQuery(it) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, "Создать заказ") },
                text = { Text("Новый заказ") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OrdersList(
                state = ordersState,
                onOrderClick = onOrderClick,
                onCancelOrder = { order ->
                    viewModel.cancelOrder(order)
                }
            )
        }
    }
    
    // Диалог создания заказа
    if (showCreateDialog) {
        CreateOrderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { order ->
                viewModel.createOrder(order) {
                    showCreateDialog = false
                }
            }
        )
    }
}

/**
 * TopBar с поиском и статистикой
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatcherTopBar(
    statsState: UiState<DispatcherStats>,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    if (isSearchActive) {
        // Поисковая строка
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { focusManager.clearFocus() },
            active = true,
            onActiveChange = {
                if (!it) {
                    onSearchActiveChange(false)
                    onSearchQueryChange("")
                }
            },
            placeholder = { Text("Поиск по адресу, грузу...") },
            leadingIcon = {
                IconButton(onClick = {
                    onSearchActiveChange(false)
                    onSearchQueryChange("")
                }) {
                    Icon(Icons.Default.ArrowBack, "Назад")
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Очистить")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        ) {}
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    } else {
        // Обычный TopBar
        SmallTopAppBar(
            title = {
                Column {
                    Text(
                        text = "Диспетчер",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    when (statsState) {
                        is UiState.Success -> {
                            Text(
                                text = "${statsState.data.activeOrders} активных • ${statsState.data.completedOrders} завершено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "Загрузка...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Default.Search, "Поиск")
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

/**
 * Список заказов
 */
@Composable
private fun OrdersList(
    state: UiState<List<OrderModel>>,
    onOrderClick: (Long) -> Unit,
    onCancelOrder: (OrderModel) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            LoadingView()
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Assignment,
                    title = "Нет заказов",
                    message = "Создайте первый заказ нажав на кнопку +"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        DispatcherOrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            onCancel = { onCancelOrder(order) }
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
                icon = Icons.Default.Assignment,
                title = "Мои заказы",
                message = "Здесь будут все ваши заказы"
            )
        }
    }
}

/**
 * Карточка заказа для диспетчера
 */
@Composable
private fun DispatcherOrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    
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
            
            // Описание
            Text(
                text = order.cargoDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Дата
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDateTime(order.dateTime),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Параметры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OrderParam(icon = Icons.Default.Person, value = "${order.requiredWorkers} чел")
                OrderParam(icon = Icons.Default.Schedule, value = "${order.estimatedHours} ч")
                OrderParam(icon = Icons.Default.AttachMoney, value = "${order.pricePerHour}₽/ч")
                OrderParam(icon = Icons.Default.Star, value = "≥${order.minWorkerRating}")
            }
            
            // Кнопка отмены
            if (order.status == OrderStatusModel.AVAILABLE || order.status == OrderStatusModel.TAKEN) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Отменить заказ")
                }
            }
        }
    }
    
    // Диалог подтверждения отмены
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Вы уверены что хотите отменить этот заказ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Отменить заказ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Назад")
                }
            }
        )
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
            null,
            Modifier.size(16.dp),
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
