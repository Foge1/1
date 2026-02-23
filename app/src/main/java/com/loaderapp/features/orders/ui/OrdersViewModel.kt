package com.loaderapp.features.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
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
    private val observeOrderUiModels: ObserveOrderUiModelsUseCase,
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
            observeOrderUiModels().collect { uiModels ->
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = null,
                        availableOrders = uiModels.filter {
                            OrdersTab.Available.matches(it.order.status)
                        },
                        inProgressOrders = uiModels.filter {
                            OrdersTab.InProgress.matches(it.order.status)
                        },
                        historyOrders = uiModels.filter {
                            OrdersTab.History.matches(it.order.status)
                        }
                    )
                }
            }
        }
    }

    // ── Public commands ───────────────────────────────────────────────────────

    fun refresh() = submit(OrdersCommand.Refresh)

    /** Грузчик откликается на заказ. */
    fun onApplyClicked(orderId: Long) = submit(OrdersCommand.Apply(orderId), orderId)

    /** Грузчик отзывает отклик. */
    fun onWithdrawClicked(orderId: Long) = submit(OrdersCommand.Withdraw(orderId), orderId)

    /** Диспетчер-создатель выбирает грузчика. */
    fun onSelectApplicant(orderId: Long, loaderId: String) =
        submit(OrdersCommand.Select(orderId, loaderId), orderId)

    /** Диспетчер-создатель снимает выбор грузчика. */
    fun onUnselectApplicant(orderId: Long, loaderId: String) =
        submit(OrdersCommand.Unselect(orderId, loaderId), orderId)

    /** Диспетчер-создатель запускает заказ (STAFFING → IN_PROGRESS). */
    fun onStartClicked(orderId: Long) = submit(OrdersCommand.Start(orderId), orderId)

    fun onCancelClicked(orderId: Long, reason: String? = null) =
        submit(OrdersCommand.Cancel(orderId, reason), orderId)

    fun onCompleteClicked(orderId: Long) = submit(OrdersCommand.Complete(orderId), orderId)

    // ── Internal ──────────────────────────────────────────────────────────────

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
        pendingOrderId?.let { id ->
            _uiState.update { it.copy(pendingActions = it.pendingActions + id) }
        }
    }

    private fun finishExecution(command: OrdersCommand, pendingOrderId: Long?) {
        if (command is OrdersCommand.Refresh) {
            _uiState.update { it.copy(refreshing = false) }
        }
        pendingOrderId?.let { id ->
            _uiState.update { it.copy(pendingActions = it.pendingActions - id) }
        }
    }
}
