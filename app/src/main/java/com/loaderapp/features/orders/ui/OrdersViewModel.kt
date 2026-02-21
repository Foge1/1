package com.loaderapp.features.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.data.OrdersRepository
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var latestOrders: List<Order> = emptyList()

    init {
        observeOrders()
        refresh()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            ordersRepository.observeOrders().collect { orders ->
                latestOrders = orders
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                        availableOrders = orders.filterBy(OrderStatus.AVAILABLE),
                        myOrders = orders.filter { order ->
                            order.status == OrderStatus.IN_PROGRESS || order.status == OrderStatus.COMPLETED
                        }.map { order -> order.toUiModel() },
                        inProgressOrders = orders.filterBy(OrderStatus.IN_PROGRESS),
                        historyOrders = orders.filter {
                            it.status == OrderStatus.COMPLETED ||
                                it.status == OrderStatus.CANCELED ||
                                it.status == OrderStatus.EXPIRED
                        }.map { order -> order.toUiModel() }
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            runCatching { ordersRepository.refresh() }
                .onFailure { e -> _uiState.update { it.copy(refreshing = false, errorMessage = e.message) } }
        }
    }

    fun acceptOrder(id: Long) = submitAction(id) { ordersRepository.acceptOrder(id) }

    fun cancelOrder(id: Long, reason: String? = null) = submitAction(id) {
        ordersRepository.cancelOrder(id, reason)
    }

    fun completeOrder(id: Long) = submitAction(id) { ordersRepository.completeOrder(id) }

    private fun submitAction(orderId: Long, action: suspend () -> Unit) {
        val before = latestOrders
        val optimistic = buildOptimisticState(before, orderId)
        _uiState.update {
            it.copy(
                pendingActions = it.pendingActions + orderId,
                availableOrders = optimistic.filterBy(OrderStatus.AVAILABLE),
                myOrders = optimistic.filter { order ->
                    order.status == OrderStatus.IN_PROGRESS || order.status == OrderStatus.COMPLETED
                }.map { order -> order.toUiModel() },
                inProgressOrders = optimistic.filterBy(OrderStatus.IN_PROGRESS),
                historyOrders = optimistic.filter {
                    it.status == OrderStatus.COMPLETED ||
                        it.status == OrderStatus.CANCELED ||
                        it.status == OrderStatus.EXPIRED
                }.map { order -> order.toUiModel() }
            )
        }

        viewModelScope.launch {
            runCatching { action() }
                .onFailure { e ->
                    latestOrders = before
                    _uiState.update {
                        it.copy(
                            errorMessage = e.message,
                            pendingActions = it.pendingActions - orderId,
                            availableOrders = before.filterBy(OrderStatus.AVAILABLE),
                            myOrders = before.filter { order ->
                                order.status == OrderStatus.IN_PROGRESS || order.status == OrderStatus.COMPLETED
                            }.map { order -> order.toUiModel() },
                            inProgressOrders = before.filterBy(OrderStatus.IN_PROGRESS),
                            historyOrders = before.filter {
                                it.status == OrderStatus.COMPLETED ||
                                    it.status == OrderStatus.CANCELED ||
                                    it.status == OrderStatus.EXPIRED
                            }.map { order -> order.toUiModel() }
                        )
                    }
                    _snackbarMessage.emit(e.message ?: "Неизвестная ошибка")
                }
                .onSuccess {
                    _uiState.update { it.copy(pendingActions = it.pendingActions - orderId) }
                }
        }
    }

    private fun buildOptimisticState(orders: List<Order>, id: Long): List<Order> {
        return orders.map { order ->
            if (order.id != id) return@map order
            when {
                OrderStateMachine.actionsFor(order).canAccept -> order.copy(status = OrderStatus.IN_PROGRESS)
                OrderStateMachine.actionsFor(order).canComplete -> order.copy(status = OrderStatus.COMPLETED)
                OrderStateMachine.actionsFor(order).canCancel -> order.copy(status = OrderStatus.CANCELED)
                else -> order
            }
        }
    }
}

private fun List<Order>.filterBy(status: OrderStatus): List<OrderUiModel> =
    filter { it.status == status }.map { it.toUiModel() }
