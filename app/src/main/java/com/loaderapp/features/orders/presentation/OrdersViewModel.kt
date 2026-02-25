package com.loaderapp.features.orders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsResult
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val observeOrderUiModels: ObserveOrderUiModelsUseCase,
    private val ordersOrchestrator: OrdersOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<OrdersUiEvent>()
    val uiEvents: SharedFlow<OrdersUiEvent> = _uiEvents.asSharedFlow()

    private val historyQuery = MutableStateFlow("")
    private var historyQueryJob: Job? = null

    val snackbarMessage: SharedFlow<String> = uiEvents
        .filterIsInstance<OrdersUiEvent.ShowSnackbar>()
        .map { it.message }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 0)

    init {
        observeOrders()
        observeHistoryQuery()
        refresh()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            observeOrderUiModels().collect { result ->
                _uiState.update { state ->
                    when (result) {
                        ObserveOrderUiModelsResult.NotSelected -> {
                            state.copy(
                                loading = false,
                                refreshing = false,
                                errorMessage = null,
                                requiresUserSelection = true,
                                availableOrders = emptyList(),
                                inProgressOrders = emptyList(),
                                historyOrders = emptyList(),
                                responsesBadge = ResponsesBadgeState(),
                                history = DispatcherHistoryUiState()
                            )
                        }

                        is ObserveOrderUiModelsResult.Selected -> {
                            val availableOrders = result.orders.filter {
                                OrdersTab.Available.matches(it.order.status)
                            }
                            val inProgressOrders = result.orders.filter {
                                OrdersTab.InProgress.matches(it.order.status)
                            }
                            val historyOrders = result.orders.filter {
                                OrdersTab.History.matches(it.order.status)
                            }
                            state.copy(
                                loading = false,
                                refreshing = false,
                                errorMessage = null,
                                requiresUserSelection = false,
                                availableOrders = availableOrders,
                                inProgressOrders = inProgressOrders,
                                historyOrders = historyOrders,
                                responsesBadge = ResponsesBadgeState(
                                    totalResponses = availableOrders.sumOf { it.visibleApplicants.size }
                                ),
                                history = buildHistoryState(
                                    query = historyQuery.value,
                                    historyOrders = historyOrders,
                                    isLoading = false,
                                    error = state.errorMessage
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun refresh() = submit(OrdersCommand.Refresh)

    fun onHistoryQueryChanged(query: String) {
        historyQuery.value = query
    }

    fun onApplyClicked(orderId: Long) = submit(OrdersCommand.Apply(orderId), orderId)

    fun onWithdrawClicked(orderId: Long) = submit(OrdersCommand.Withdraw(orderId), orderId)

    fun onSelectApplicant(orderId: Long, loaderId: String) =
        submit(OrdersCommand.Select(orderId, loaderId), orderId)

    fun onUnselectApplicant(orderId: Long, loaderId: String) =
        submit(OrdersCommand.Unselect(orderId, loaderId), orderId)

    fun onStartClicked(orderId: Long) = submit(OrdersCommand.Start(orderId), orderId)

    fun onCancelClicked(orderId: Long, reason: String? = null) =
        submit(OrdersCommand.Cancel(orderId, reason), orderId)

    fun onCompleteClicked(orderId: Long) = submit(OrdersCommand.Complete(orderId), orderId)


    private fun observeHistoryQuery() {
        historyQueryJob?.cancel()
        historyQueryJob = viewModelScope.launch {
            historyQuery
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce(250)
                .mapLatest { query ->
                    val snapshot = _uiState.value
                    withContext(Dispatchers.Default) {
                        buildHistoryState(
                            query = query,
                            historyOrders = snapshot.historyOrders,
                            isLoading = snapshot.loading,
                            error = snapshot.errorMessage
                        )
                    }
                }
                .collect { historyState ->
                    _uiState.update { state -> state.copy(history = historyState) }
                }
        }
    }

    private fun buildHistoryState(
        query: String,
        historyOrders: List<OrderUiModel>,
        isLoading: Boolean,
        error: String?
    ): DispatcherHistoryUiState {
        val trimmed = query.trim()
        val normalizedQuery = trimmed.lowercase()
        val filtered = if (normalizedQuery.isBlank()) {
            historyOrders
        } else {
            historyOrders.filter { order ->
                order.order.address.contains(normalizedQuery, ignoreCase = true) ||
                    order.order.title.contains(normalizedQuery, ignoreCase = true) ||
                    order.order.tags.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                    (order.order.comment?.contains(normalizedQuery, ignoreCase = true) == true) ||
                    order.order.id.toString().contains(normalizedQuery)
            }
        }

        val zoneId = ZoneId.systemDefault()
        val today = Instant.now().atZone(zoneId).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("ru"))

        val sections = filtered
            .groupBy { Instant.ofEpochMilli(it.order.dateTime).atZone(zoneId).toLocalDate() }
            .toList()
            .sortedByDescending { (date, _) -> date }
            .map { (date, items) ->
                val title = when (ChronoUnit.DAYS.between(date, today)) {
                    0L -> "Сегодня"
                    1L -> "Вчера"
                    else -> date.format(formatter)
                }
                HistorySectionUi(
                    key = date.toString(),
                    title = title,
                    count = items.size,
                    items = items.map { OrderHistoryItemUi(it) }
                )
            }

        return DispatcherHistoryUiState(
            query = trimmed,
            sections = sections,
            isLoading = isLoading,
            error = error
        )
    }

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
