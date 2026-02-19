package com.loaderapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _historyState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<OrderModel>>> = _historyState.asStateFlow()

    fun initialize(userId: Long) {
        viewModelScope.launch {
            orderRepository.getOrdersByWorker(userId)
                .map { orders ->
                    orders
                        .filter { it.status == OrderStatusModel.COMPLETED || it.status == OrderStatusModel.CANCELLED }
                        .sortedByDescending { it.completedAt ?: it.createdAt }
                }
                .collect { filtered ->
                    _historyState.value = UiState.Success(filtered)
                }
        }
    }
}
