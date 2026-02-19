package com.loaderapp.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.GetOrderHistoryParams
import com.loaderapp.domain.usecase.order.GetOrderHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel экрана истории заказов.
 *
 * Использует [GetOrderHistoryUseCase] для получения отфильтрованного
 * и отсортированного списка завершённых/отменённых заказов.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getOrderHistoryUseCase: GetOrderHistoryUseCase
) : ViewModel() {

    private val _historyState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<OrderModel>>> = _historyState.asStateFlow()

    fun initialize(userId: Long) {
        viewModelScope.launch {
            getOrderHistoryUseCase(GetOrderHistoryParams(userId))
                .collect { orders ->
                    _historyState.value = UiState.Success(orders)
                }
        }
    }
}
