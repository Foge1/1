package com.loaderapp.features.orders.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.features.orders.domain.usecase.ApplyToOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrderDetailResult
import com.loaderapp.features.orders.domain.usecase.ObserveOrderDetailUseCase
import com.loaderapp.features.orders.domain.usecase.SelectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.StartOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UnselectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import com.loaderapp.features.orders.domain.usecase.WithdrawApplicationUseCase
import com.loaderapp.features.orders.presentation.mapper.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observeOrderDetailUseCase: ObserveOrderDetailUseCase,
        private val applyToOrderUseCase: ApplyToOrderUseCase,
        private val withdrawApplicationUseCase: WithdrawApplicationUseCase,
        private val selectApplicantUseCase: SelectApplicantUseCase,
        private val unselectApplicantUseCase: UnselectApplicantUseCase,
        private val startOrderUseCase: StartOrderUseCase,
        private val cancelOrderUseCase: CancelOrderUseCase,
        private val completeOrderUseCase: CompleteOrderUseCase,
    ) : ViewModel() {
        private val orderId: Long =
            checkNotNull(savedStateHandle[OrderDetailRoute.ORDER_ID_ARG]) {
                "OrderDetailViewModel requires '${OrderDetailRoute.ORDER_ID_ARG}'"
            }

        private val _uiState = MutableStateFlow(OrderDetailUiState())
        val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observeOrderDetailUseCase(orderId).collect { result ->
                    _uiState.update { current ->
                        when (result) {
                            ObserveOrderDetailResult.NotFound -> {
                                current.copy(
                                    loading = false,
                                    order = null,
                                    errorMessage = "Заказ не найден",
                                    requiresUserSelection = false,
                                )
                            }

                            ObserveOrderDetailResult.NotSelected -> {
                                current.copy(
                                    loading = false,
                                    order = null,
                                    errorMessage = null,
                                    requiresUserSelection = true,
                                )
                            }

                            is ObserveOrderDetailResult.Success -> {
                                current.copy(
                                    loading = false,
                                    order = result.order.toUiModel(),
                                    errorMessage = null,
                                    requiresUserSelection = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        fun onApply() = runAction { applyToOrderUseCase(orderId) }

        fun onWithdraw() = runAction { withdrawApplicationUseCase(orderId) }

        fun onSelectApplicant(loaderId: String) = runAction { selectApplicantUseCase(orderId, loaderId) }

        fun onUnselectApplicant(loaderId: String) = runAction { unselectApplicantUseCase(orderId, loaderId) }

        fun onStart() = runAction { startOrderUseCase(orderId) }

        fun onCancel(reason: String? = null) = runAction { cancelOrderUseCase(orderId, reason) }

        fun onComplete() = runAction { completeOrderUseCase(orderId) }

        private fun runAction(action: suspend () -> UseCaseResult<Unit>) {
            viewModelScope.launch {
                _uiState.update { it.copy(isActionInProgress = true) }
                when (val result = action()) {
                    is UseCaseResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isActionInProgress = false,
                                errorMessage = null,
                            )
                        }
                    }

                    is UseCaseResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isActionInProgress = false,
                                errorMessage = result.reason,
                            )
                        }
                    }
                }
            }
        }
    }
