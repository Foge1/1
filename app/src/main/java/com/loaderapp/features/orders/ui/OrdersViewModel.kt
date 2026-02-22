package com.loaderapp.features.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
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
    private val observeOrdersUseCase: ObserveOrdersUseCase,
    private val acceptOrderUseCase: AcceptOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        observeOrders()
        refresh()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            observeOrdersUseCase().collect { orders ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                        availableOrders = OrdersTab.Available.filter(orders).map { order -> order.toUiModel() },
                        inProgressOrders = OrdersTab.InProgress.filter(orders).map { order -> order.toUiModel() },
                        historyOrders = OrdersTab.History.filter(orders).map { order -> order.toUiModel() }
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            when (val result = refreshOrdersUseCase()) {
                is UseCaseResult.Success -> {
                    _uiState.update { it.copy(refreshing = false) }
                }

                is UseCaseResult.Failure -> {
                    _uiState.update { it.copy(refreshing = false, errorMessage = result.reason) }
                }
            }
        }
    }

    fun onAcceptClicked(id: Long) = submitAction(id) { acceptOrderUseCase(id) }

    fun onCancelClicked(id: Long, reason: String? = null) = submitAction(id) {
        cancelOrderUseCase(id, reason)
    }

    fun onCompleteClicked(id: Long) = submitAction(id) { completeOrderUseCase(id) }

    private fun submitAction(orderId: Long, action: suspend () -> UseCaseResult<Unit>) {
        _uiState.update { it.copy(pendingActions = it.pendingActions + orderId) }
        viewModelScope.launch {
            when (val result = action()) {
                is UseCaseResult.Success -> {
                    _uiState.update { it.copy(pendingActions = it.pendingActions - orderId) }
                }

                is UseCaseResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.reason,
                            pendingActions = it.pendingActions - orderId
                        )
                    }
                    _snackbarMessage.emit(result.reason)
                }
            }
        }
    }
}
