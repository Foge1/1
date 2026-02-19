package com.loaderapp.ui.dispatcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.loaderapp.ui.components.DispatcherOrderCard
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderStatusChip
import com.loaderapp.ui.components.OrderParamChip
import com.loaderapp.ui.components.formatOrderDateTime
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
    onOrderClick: (Long) -> Unit
) {
    val ordersState by viewModel.ordersState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
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
            DispatcherTopBar() },
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
private fun DispatcherTopBar() {
    GradientTopBar(title = "Диспетчер")
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



