package com.loaderapp.presentation.order

import androidx.lifecycle.SavedStateHandle
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.GetOrderByIdParams
import com.loaderapp.domain.usecase.order.GetOrderByIdUseCase
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
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
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
        loadOrder()
        observeWorkerCount()
    }

    private fun loadOrder() {
        launchSafe {
            getOrderByIdUseCase(GetOrderByIdParams(orderId))
                .onSuccess { order -> _orderState.setSuccess(order) }
                .onError   { msg, _ -> _orderState.setError(msg) }
        }
    }

    private fun observeWorkerCount() {
        getWorkerCountUseCase(GetWorkerCountParams(orderId))
            .onEach { count -> _workerCount.value = count }
            .catch  { /* worker count failure is non-critical */ }
            .launchIn(androidx.lifecycle.viewModelScope)
    }
}
