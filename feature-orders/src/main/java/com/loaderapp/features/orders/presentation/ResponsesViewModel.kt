package com.loaderapp.features.orders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.usecase.GetRespondersWithAvailabilityUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsResult
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
import com.loaderapp.features.orders.domain.usecase.ResponderAvailability
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ResponsesViewModel @Inject constructor(
    private val observeOrderUiModels: ObserveOrderUiModelsUseCase,
    private val getRespondersWithAvailability: GetRespondersWithAvailabilityUseCase,
    private val ordersOrchestrator: OrdersOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResponsesUiState())
    val uiState: StateFlow<ResponsesUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        observeResponses()
    }

    private fun observeResponses() {
        viewModelScope.launch {
            observeOrderUiModels().collect { result ->
                when (result) {
                    ObserveOrderUiModelsResult.NotSelected -> {
                        _uiState.update { state ->
                            state.copy(
                                loading = false,
                                items = emptyList(),
                                errorMessage = null,
                            )
                        }
                    }

                    is ObserveOrderUiModelsResult.Selected -> {
                        val responderIds = result.orders
                            .asSequence()
                            .filter { it.order.status == OrderStatus.STAFFING }
                            .flatMap { order -> order.visibleApplicants.asSequence().map { it.loaderId } }
                            .distinct()
                            .toList()
                        val availability = getRespondersWithAvailability(responderIds)
                        _uiState.update { state ->
                            state.copy(
                                loading = false,
                                errorMessage = null,
                                items = result.orders.toResponsesItems(availability)
                            )
                        }
                    }
                }
            }
        }
    }

    fun onToggleApplicant(orderId: Long, loaderId: String, isSelected: Boolean) {
        val command = if (isSelected) {
            OrdersCommand.Unselect(orderId, loaderId)
        } else {
            OrdersCommand.Select(orderId, loaderId)
        }
        submit(command, orderId)
    }

    fun onStartClicked(orderId: Long) = submit(OrdersCommand.Start(orderId), orderId)

    private fun submit(command: OrdersCommand, pendingOrderId: Long) {
        _uiState.update { it.copy(pendingActions = it.pendingActions + pendingOrderId) }

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
                _uiState.update { it.copy(pendingActions = it.pendingActions - pendingOrderId) }
            }

            failureReason?.let { reason ->
                _uiState.update { it.copy(errorMessage = reason) }
                _snackbarMessage.emit(reason)
            }
        }
    }
}

internal fun List<OrderUiModel>.toResponsesItems(
    availability: Map<String, ResponderAvailability>
): List<OrderResponsesUiModel> =
    asSequence()
        .filter { it.order.status == OrderStatus.STAFFING }
        .map { order ->
            val responses = order.visibleApplicants.map { application ->
                val isSelected = application.status == OrderApplicationStatus.SELECTED
                val loaderAvailability = availability[application.loaderId]
                val isBusyOutsideCurrentOrder = loaderAvailability?.isBusy == true &&
                    loaderAvailability.busyOrderId != order.order.id
                val canToggle = if (isSelected) {
                    order.canUnselect
                } else {
                    order.canSelect && !isBusyOutsideCurrentOrder
                }
                ResponseRowUiModel(
                    loaderId = application.loaderId,
                    loaderName = application.loaderId,
                    isSelected = isSelected,
                    canToggle = canToggle,
                    toggleDisabledReason = when {
                        canToggle -> null
                        isBusyOutsideCurrentOrder -> "Грузчик уже в работе"
                        else -> "Недоступно"
                    },
                    isBusy = isBusyOutsideCurrentOrder,
                    busyOrderId = loaderAvailability?.busyOrderId
                )
            }
            val selectedCount = order.selectedApplicantsCount
            val requiredCount = order.order.workersTotal
            val selectedHint = "Выберите $selectedCount из $requiredCount"
            OrderResponsesUiModel(
                orderId = order.order.id,
                address = order.order.address,
                cargoText = order.order.title,
                requiredCount = requiredCount,
                selectedCount = selectedCount,
                responsesCount = responses.size,
                responses = responses,
                canStart = order.canStart,
                startDisabledReason = when {
                    order.canStart -> null
                    selectedCount < requiredCount -> selectedHint
                    else -> order.startDisabledReason ?: "Нужно выбрать грузчиков"
                }
            )
        }
        .sortedWith(
            compareByDescending<OrderResponsesUiModel> {
                it.responsesCount > 0 && it.selectedCount < it.requiredCount
            }.thenByDescending { it.responsesCount }
        )
        .toList()
