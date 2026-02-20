package com.loaderapp.presentation.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.GetOrderByIdFlowParams
import com.loaderapp.domain.usecase.order.GetOrderByIdFlowUseCase
import com.loaderapp.domain.usecase.order.GetWorkerCountParams
import com.loaderapp.domain.usecase.order.GetWorkerCountUseCase
import com.loaderapp.navigation.NavArgs
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for the order detail screen.
 *
 * ## Architecture
 * - Injects [GetOrderByIdUseCase] and [GetWorkerCountUseCase] — not [OrderRepository] directly.
 *   ViewModels must never reference Repository interfaces; that breaks Clean Architecture.
 * - Reads [orderId] from [SavedStateHandle] in [init] — no [loadOrder] method exposed to UI.
 *   This is the canonical pattern: the ViewModel is self-sufficient from construction,
 *   no [LaunchedEffect] wiring required in the composable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOrderByIdFlowUseCase: GetOrderByIdFlowUseCase,
    private val getWorkerCountUseCase: GetWorkerCountUseCase
) : BaseViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle[NavArgs.ORDER_ID]) {
        "OrderDetailViewModel requires '${NavArgs.ORDER_ID}' in SavedStateHandle"
    }

    private val _orderState = MutableStateFlow<UiState<OrderModel>>(UiState.Loading)
    val orderState: StateFlow<UiState<OrderModel>> = _orderState.asStateFlow()

    private val _workerCount = MutableStateFlow(0)
    val workerCount: StateFlow<Int> = _workerCount.asStateFlow()

    init {
        observeOrder()
        observeWorkerCount()
    }

    private fun observeOrder() {
        getOrderByIdFlowUseCase(GetOrderByIdFlowParams(orderId))
            .onEach { order ->
                _orderState.value = order?.let { UiState.Success(it) } ?: UiState.Error("Заказ не найден")
            }
            .catch { e -> _orderState.value = UiState.Error("Ошибка загрузки заказа: ${e.message}") }
            .launchIn(viewModelScope)
    }

    private fun observeWorkerCount() {
        getWorkerCountUseCase(GetWorkerCountParams(orderId))
            .onEach { count -> _workerCount.value = count }
            .catch  { /* worker count failure is non-critical */ }
            .launchIn(viewModelScope)
    }
}
