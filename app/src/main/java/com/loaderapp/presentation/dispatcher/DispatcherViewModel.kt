package com.loaderapp.presentation.dispatcher

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.*
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DispatcherViewModel @Inject constructor(
    private val getOrdersByDispatcherUseCase: GetOrdersByDispatcherUseCase,
    private val searchOrdersByDispatcherUseCase: SearchOrdersByDispatcherUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase
) : BaseViewModel() {

    private val _dispatcherId = MutableStateFlow<Long?>(null)

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

    /**
     * Idempotent: [MutableStateFlow] discards duplicate values, so repeated
     * calls with the same [dispatcherId] do not restart any Flow pipelines.
     */
    fun initialize(dispatcherId: Long) {
        _dispatcherId.value = dispatcherId
    }

    // ── Observations ─────────────────────────────────────────────────────────

    private fun observeOrders() {
        combine(_dispatcherId.filterNotNull(), _searchQuery.debounce(300)) { id, query -> id to query }
            .flatMapLatest { (id, query) ->
                if (query.isBlank()) {
                    getOrdersByDispatcherUseCase(GetOrdersByDispatcherParams(id))
                } else {
                    // Delegate search to the repository/DAO — no in-memory filtering
                    searchOrdersByDispatcherUseCase(SearchOrdersByDispatcherParams(id, query))
                }
            }
            .onEach { orders -> _ordersState.setSuccess(orders) }
            .catch  { e -> _ordersState.setError("Ошибка загрузки заказов: ${e.message}") }
            .launchIn(viewModelScope)
    }

    private fun observeStats() {
        _dispatcherId
            .filterNotNull()
            .flatMapLatest { id -> getDispatcherStatsUseCase(GetDispatcherStatsParams(id)) }
            .onEach { stats -> _statsState.setSuccess(stats) }
            .catch  { e -> _statsState.setError("Ошибка загрузки статистики: ${e.message}") }
            .launchIn(viewModelScope)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Creates an order. On success, a snackbar is shown and the orders list
     * updates automatically via the Room Flow. No callback needed.
     */
    fun createOrder(order: OrderModel) {
        launchSafe {
            createOrderUseCase(CreateOrderParams(order))
                .onSuccess { _ -> showSnackbar("Заказ создан успешно") }
                .onError   { msg, _ -> showSnackbar(msg) }
        }
    }

    fun cancelOrder(order: OrderModel) {
        launchSafe {
            cancelOrderUseCase(CancelOrderParams(order.id))
                .onSuccess { _ -> showSnackbar("Заказ отменён") }
                .onError   { msg, _ -> showSnackbar(msg) }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }
}
