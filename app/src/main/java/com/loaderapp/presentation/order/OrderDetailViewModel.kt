package com.loaderapp.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<UiState<OrderModel>>(UiState.Loading)
    val orderState: StateFlow<UiState<OrderModel>> = _orderState.asStateFlow()

    /** Количество грузчиков на заказе — живой Flow из Room */
    private val _workerCount = MutableStateFlow(0)
    val workerCount: StateFlow<Int> = _workerCount.asStateFlow()

    fun loadOrder(orderId: Long) {
        viewModelScope.launch {
            _orderState.value = UiState.Loading
            when (val result = orderRepository.getOrderById(orderId)) {
                is Result.Success -> {
                    _orderState.value = UiState.Success(result.data)
                    // Подписываемся на live-обновления счётчика грузчиков
                    launch {
                        orderRepository.getWorkerCountForOrder(orderId)
                            .collect { _workerCount.value = it }
                    }
                }
                is Result.Error   -> _orderState.value = UiState.Error(result.message)
                is Result.Loading -> _orderState.value = UiState.Loading
            }
        }
    }
}
