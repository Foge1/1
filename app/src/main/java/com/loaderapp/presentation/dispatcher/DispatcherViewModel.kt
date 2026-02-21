package com.loaderapp.presentation.dispatcher

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.usecase.order.DispatcherStats
import com.loaderapp.features.orders.data.OrdersRepository
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
@HiltViewModel
class DispatcherViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : BaseViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val ordersState: StateFlow<UiState<List<OrderModel>>> = _ordersState.asStateFlow()

    private val _statsState = MutableStateFlow<UiState<DispatcherStats>>(UiState.Idle)
    val statsState: StateFlow<UiState<DispatcherStats>> = _statsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    init {
        observeOrders()
        observeStats()
    }

    fun initialize(dispatcherId: Long) = Unit

    private fun observeOrders() {
        combine(ordersRepository.observeOrders(), _searchQuery.debounce(300)) { orders, query ->
            if (query.isBlank()) orders else orders.filter { order ->
                order.address.contains(query, ignoreCase = true) ||
                    order.title.contains(query, ignoreCase = true) ||
                    order.comment?.contains(query, ignoreCase = true) == true
            }
        }
            .onEach { orders -> _ordersState.value = UiState.Success(orders.map { it.toOrderModel() }) }
            .launchIn(viewModelScope)
    }

    private fun observeStats() {
        ordersRepository.observeOrders()
            .onEach { orders ->
                _statsState.value = UiState.Success(
                    DispatcherStats(
                        completedOrders = orders.count { it.status == OrderStatus.COMPLETED },
                        activeOrders = orders.count {
                            it.status == OrderStatus.AVAILABLE || it.status == OrderStatus.IN_PROGRESS
                        }
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    fun createOrder(order: OrderModel) {
        launchSafe {
            ordersRepository.createOrder(order.toFeatureOrder())
            showSnackbar("Заказ создан успешно")
        }
    }

    fun cancelOrder(order: OrderModel) {
        launchSafe {
            ordersRepository.cancelOrder(order.id)
            showSnackbar("Заказ отменён")
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }
}

private fun Order.toOrderModel(): OrderModel {
    val status = when (status) {
        OrderStatus.AVAILABLE -> OrderStatusModel.AVAILABLE
        OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
        OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
        OrderStatus.CANCELED, OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
    }

    return OrderModel(
        id = id,
        address = address,
        dateTime = dateTime,
        cargoDescription = title,
        pricePerHour = pricePerHour,
        estimatedHours = (durationMin / 60).coerceAtLeast(1),
        requiredWorkers = workersTotal,
        minWorkerRating = 0f,
        status = status,
        createdAt = dateTime,
        completedAt = null,
        workerId = if (workersCurrent > 0) 1L else null,
        dispatcherId = 0L,
        workerRating = null,
        comment = comment.orEmpty()
    )
}

private fun OrderModel.toFeatureOrder(): Order = Order(
    id = id,
    title = cargoDescription.ifBlank { "Заказ" },
    address = address,
    pricePerHour = pricePerHour,
    dateTime = dateTime,
    durationMin = estimatedHours * 60,
    workersCurrent = if (workerId == null) 0 else 1,
    workersTotal = requiredWorkers,
    tags = listOf(cargoDescription),
    meta = emptyMap(),
    comment = comment,
    status = OrderStatus.AVAILABLE
)
