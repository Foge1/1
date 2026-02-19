package com.loaderapp.presentation.history

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.GetOrdersByWorkerParams
import com.loaderapp.domain.usecase.order.GetOrdersByWorkerUseCase
import com.loaderapp.domain.usecase.order.GetOrdersByDispatcherUseCase
import com.loaderapp.domain.usecase.order.GetOrdersByDispatcherParams
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getOrdersByWorkerUseCase: GetOrdersByWorkerUseCase,
    private val getOrdersByDispatcherUseCase: GetOrdersByDispatcherUseCase
) : BaseViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Idle)
    val ordersState: StateFlow<UiState<List<OrderModel>>> = _ordersState.asStateFlow()

    fun initialize(userId: Long, isDispatcher: Boolean) {
        viewModelScope.launch {
            _ordersState.setLoading()
            try {
                if (isDispatcher) {
                    getOrdersByDispatcherUseCase(GetOrdersByDispatcherParams(userId))
                        .collect { _ordersState.setSuccess(it) }
                } else {
                    getOrdersByWorkerUseCase(GetOrdersByWorkerParams(userId))
                        .collect { _ordersState.setSuccess(it) }
                }
            } catch (e: Exception) {
                _ordersState.setError("Ошибка загрузки истории")
            }
        }
    }
}
