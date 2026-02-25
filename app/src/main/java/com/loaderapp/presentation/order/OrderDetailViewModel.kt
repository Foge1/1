package com.loaderapp.presentation.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.toAppError
import com.loaderapp.core.common.UiText
import com.loaderapp.presentation.common.toUiText
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.GetWorkerCountParams
import com.loaderapp.domain.usecase.order.GetWorkerCountUseCase
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.data.mappers.toLegacyOrderModel
import com.loaderapp.navigation.NavArgs
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ordersRepository: OrdersRepository,
    private val getWorkerCountUseCase: GetWorkerCountUseCase,
    private val appLogger: AppLogger
) : BaseViewModel() {

    private val orderId: Long = savedStateHandle.get<Any?>(NavArgs.ORDER_ID)
        ?.let(::parseOrderId)
        ?: error("OrderDetailViewModel requires valid '${NavArgs.ORDER_ID}' in SavedStateHandle")

    private val _orderState = MutableStateFlow<UiState<OrderModel>>(UiState.Loading)
    val orderState: StateFlow<UiState<OrderModel>> = _orderState.asStateFlow()

    private val _workerCount = MutableStateFlow(0)
    val workerCount: StateFlow<Int> = _workerCount.asStateFlow()

    init {
        appLogger.d(LOG_TAG, "load orderId=$orderId")
        observeOrder()
        observeWorkerCount()
    }

    private fun observeOrder() {
        ordersRepository.observeOrders()
            .map { orders -> orders.firstOrNull { it.id == orderId } }
            .onEach { order ->
                appLogger.d(LOG_TAG, "load orderId=$orderId, found=${order != null}")
                _orderState.value = order
                    ?.toLegacyOrderModel()
                    ?.let { UiState.Success(it) }
                    ?: UiState.Error(UiText.Dynamic("Заказ не найден"))
            }
            .catch { e -> _orderState.value = UiState.Error(e.toAppError().toUiText()) }
            .launchIn(viewModelScope)
    }

    private fun observeWorkerCount() {
        getWorkerCountUseCase(GetWorkerCountParams(orderId))
            .onEach { count -> _workerCount.value = count }
            .catch  { }
            .launchIn(viewModelScope)
    }
    private fun parseOrderId(raw: Any): Long? = when (raw) {
        is Long -> raw
        is Int -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }

    private companion object {
        const val LOG_TAG = "OrderDetailVM"
    }

}
