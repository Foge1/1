package com.loaderapp.presentation.history

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.toAppError
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.order.GetDispatcherHistoryParams
import com.loaderapp.domain.usecase.order.GetDispatcherHistoryUseCase
import com.loaderapp.domain.usecase.order.GetOrderHistoryParams
import com.loaderapp.domain.usecase.order.GetOrderHistoryUseCase
import com.loaderapp.presentation.base.BaseViewModel
import com.loaderapp.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel экрана истории заказов.
 *
 * Поддерживает обе роли через единый интерфейс.
 * Применяет тот же паттерн [MutableStateFlow] + [filterNotNull] + [flatMapLatest]:
 * подписка стартует ровно один раз, идемпотентна при рекомпозиции.
 *
 * [_initParams] хранит пару (userId, role) — Flow реагирует на изменение
 * любого из параметров, что нужно при переключении ролей без пересоздания VM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val getOrderHistoryUseCase: GetOrderHistoryUseCase,
        private val getDispatcherHistoryUseCase: GetDispatcherHistoryUseCase,
    ) : BaseViewModel() {
        private data class HistorySourceState(
            val orders: List<OrderModel> = emptyList(),
            val error: Throwable? = null,
        )

        private data class InitParams(
            val userId: Long,
            val role: UserRoleModel,
        )

        private val _initParams = MutableStateFlow<InitParams?>(null)
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private val _historyState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
        val historyState: StateFlow<UiState<List<OrderModel>>> = _historyState.asStateFlow()

        init {
            _initParams
                .filterNotNull()
                .flatMapLatest { (userId, role) ->
                    when (role) {
                        UserRoleModel.DISPATCHER -> getDispatcherHistoryUseCase(GetDispatcherHistoryParams(userId))
                        UserRoleModel.LOADER -> getOrderHistoryUseCase(GetOrderHistoryParams(userId))
                    }
                }.map { orders ->
                    HistorySourceState(orders = orders)
                }.catch { throwable ->
                    emit(HistorySourceState(error = throwable))
                }.combine(_query) { sourceState, query ->
                    if (sourceState.error != null) {
                        UiState.Error(sourceState.error.toAppError().toUiText())
                    } else {
                        UiState.Success(sourceState.orders.filterByQuery(query))
                    }
                }.onEach { _historyState.value = it }
                .launchIn(viewModelScope)
        }

        /** Идемпотентен: повторный вызов с теми же параметрами не пересоздаёт подписку. */
        fun initialize(
            userId: Long,
            role: UserRoleModel,
        ) {
            _initParams.value = InitParams(userId, role)
        }

        fun onQueryChanged(query: String) {
            _query.value = query
        }

        private fun List<OrderModel>.filterByQuery(query: String): List<OrderModel> {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return this
            }
            return filter { order ->
                order.title.contains(normalizedQuery, ignoreCase = true) ||
                    order.address.contains(normalizedQuery, ignoreCase = true) ||
                    order.comment?.contains(normalizedQuery, ignoreCase = true) == true
            }
        }
    }
