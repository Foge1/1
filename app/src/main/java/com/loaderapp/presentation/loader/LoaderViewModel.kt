package com.loaderapp.presentation.loader

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.onError
import com.loaderapp.core.common.onSuccess
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.usecase.order.*
import com.loaderapp.domain.usecase.user.GetUserByIdFlowParams
import com.loaderapp.domain.usecase.user.GetUserByIdFlowUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoaderViewModel @Inject constructor(
    private val getAvailableOrdersUseCase: GetAvailableOrdersUseCase,
    private val getOrdersByWorkerUseCase: GetOrdersByWorkerUseCase,
    private val takeOrderUseCase: TakeOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val getUserByIdFlowUseCase: GetUserByIdFlowUseCase
) : BaseViewModel() {

    private val _workerId = MutableStateFlow<Long?>(null)

    private val _availableOrdersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val availableOrdersState: StateFlow<UiState<List<OrderModel>>> = _availableOrdersState.asStateFlow()

    private val _myOrdersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val myOrdersState: StateFlow<UiState<List<OrderModel>>> = _myOrdersState.asStateFlow()

    private val _statsState = MutableStateFlow<UiState<WorkerStats>>(UiState.Idle)
    val statsState: StateFlow<UiState<WorkerStats>> = _statsState.asStateFlow()

    init {
        observeAvailableOrders()
        observeMyOrders()
        observeStats()
    }

    /** Idempotent: [MutableStateFlow] discards duplicate values. */
    fun initialize(workerId: Long) {
        _workerId.value = workerId
    }

    // ── Observations ─────────────────────────────────────────────────────────

    private fun observeAvailableOrders() {
        _workerId
            .filterNotNull()
            .flatMapLatest { workerId ->
                // Combine available orders and the worker's live rating so that
                // the filter updates reactively if the rating changes.
                combine(
                    getAvailableOrdersUseCase(Unit),
                    getUserByIdFlowUseCase(GetUserByIdFlowParams(workerId))
                ) { orders, user ->
                    val rating = user?.rating?.toFloat() ?: 5.0f
                    orders.filter { it.isAvailableForWorker(rating) }
                }
            }
            .onEach { orders -> _availableOrdersState.setSuccess(orders) }
            .catch  { e -> _availableOrdersState.setError("Ошибка загрузки заказов: ${e.message}") }
            .launchIn(viewModelScope)
    }

    private fun observeMyOrders() {
        _workerId
            .filterNotNull()
            .flatMapLatest { workerId ->
                getOrdersByWorkerUseCase(GetOrdersByWorkerParams(workerId))
                    .map { orders ->
                        orders.filter {
                            it.status == OrderStatusModel.TAKEN ||
                            it.status == OrderStatusModel.IN_PROGRESS
                        }
                    }
            }
            .onEach { orders -> _myOrdersState.setSuccess(orders) }
            .catch  { e -> _myOrdersState.setError("Ошибка загрузки моих заказов: ${e.message}") }
            .launchIn(viewModelScope)
    }

    private fun observeStats() {
        _workerId
            .filterNotNull()
            .flatMapLatest { workerId -> getWorkerStatsUseCase(GetWorkerStatsParams(workerId)) }
            .onEach { stats -> _statsState.setSuccess(stats) }
            .catch  { e -> _statsState.setError("Ошибка загрузки статистики: ${e.message}") }
            .launchIn(viewModelScope)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun takeOrder(order: OrderModel) {
        val workerId = _workerId.value ?: return
        launchSafe {
            takeOrderUseCase(TakeOrderParams(order.id, workerId))
                .onSuccess { _ -> showSnackbar("Заказ успешно взят") }
                .onError   { msg, _ -> showSnackbar(msg) }
        }
    }

    fun completeOrder(order: OrderModel) {
        launchSafe {
            completeOrderUseCase(CompleteOrderParams(order.id))
                .onSuccess { _ -> showSnackbar("Заказ завершён") }
                .onError   { msg, _ -> showSnackbar(msg) }
        }
    }
}
