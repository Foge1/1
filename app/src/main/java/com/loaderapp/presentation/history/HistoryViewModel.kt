package com.loaderapp.presentation.history

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.toAppError
import com.loaderapp.core.common.UiText
import com.loaderapp.presentation.common.toUiText
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.order.GetDispatcherHistoryParams
import com.loaderapp.domain.usecase.order.GetDispatcherHistoryUseCase
import com.loaderapp.domain.usecase.order.GetOrderHistoryParams
import com.loaderapp.domain.usecase.order.GetOrderHistoryUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
class HistoryViewModel @Inject constructor(
    private val getOrderHistoryUseCase: GetOrderHistoryUseCase,
    private val getDispatcherHistoryUseCase: GetDispatcherHistoryUseCase
) : BaseViewModel() {

    private data class InitParams(val userId: Long, val role: UserRoleModel)

    private val _initParams = MutableStateFlow<InitParams?>(null)

    private val _historyState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<OrderModel>>> = _historyState.asStateFlow()

    init {
        _initParams
            .filterNotNull()
            .flatMapLatest { (userId, role) ->
                when (role) {
                    UserRoleModel.DISPATCHER -> getDispatcherHistoryUseCase(GetDispatcherHistoryParams(userId))
                    UserRoleModel.LOADER     -> getOrderHistoryUseCase(GetOrderHistoryParams(userId))
                }
            }
            .onEach  { orders -> _historyState.value = UiState.Success(orders) }
            .catch   { e -> _historyState.value = UiState.Error(e.toAppError().toUiText()) }
            .launchIn(viewModelScope)
    }

    /** Идемпотентен: повторный вызов с теми же параметрами не пересоздаёт подписку. */
    fun initialize(userId: Long, role: UserRoleModel) {
        _initParams.value = InitParams(userId, role)
    }
}
