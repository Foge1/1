package com.loaderapp.features.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.usecase.ObserveOrdersForRoleUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val observeOrdersUseCase: ObserveOrdersForRoleUseCase,
    private val ordersOrchestrator: OrdersOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<OrdersUiEvent>()
    val uiEvents: SharedFlow<OrdersUiEvent> = _uiEvents.asSharedFlow()

    val snackbarMessage: SharedFlow<String> = uiEvents
        .filterIsInstance<OrdersUiEvent.ShowSnackbar>()
        .map { it.message }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 0)

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

    fun refresh() = submit(OrdersCommand.Refresh)

    fun onAcceptClicked(id: Long) = submit(OrdersCommand.Accept(id), id)

    fun onCancelClicked(id: Long, reason: String? = null) = submit(OrdersCommand.Cancel(id, reason), id)

    fun onCompleteClicked(id: Long) = submit(OrdersCommand.Complete(id), id)

    private fun submit(command: OrdersCommand, pendingOrderId: Long? = null) {
        startExecution(command, pendingOrderId)

        viewModelScope.launch {
            var failureReason: String? = null
            try {
                when (val result = ordersOrchestrator.execute(command)) {
                    is UseCaseResult.Success -> Unit
                    is UseCaseResult.Failure -> failureReason = result.reason
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failureReason = e.message ?: "Неизвестная ошибка"
            } finally {
                finishExecution(command, pendingOrderId)
            }

            failureReason?.let { reason ->
                _uiState.update { it.copy(errorMessage = reason) }
                _uiEvents.emit(OrdersUiEvent.ShowSnackbar(reason))
            }
        }
    }

    private fun startExecution(command: OrdersCommand, pendingOrderId: Long?) {
        if (command is OrdersCommand.Refresh) {
            _uiState.update { it.copy(refreshing = true) }
        }
        pendingOrderId?.let { orderId ->
            _uiState.update { it.copy(pendingActions = it.pendingActions + orderId) }
        }
    }

    private fun finishExecution(command: OrdersCommand, pendingOrderId: Long?) {
        if (command is OrdersCommand.Refresh) {
            _uiState.update { it.copy(refreshing = false) }
        }
        pendingOrderId?.let { orderId ->
            _uiState.update { it.copy(pendingActions = it.pendingActions - orderId) }
        }
    }
}
