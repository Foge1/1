package com.loaderapp.presentation.dispatcher

import com.loaderapp.domain.model.OrderRules
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderDraft
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val currentUserProvider: CurrentUserProvider,
) : BaseViewModel() {

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(defaultState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    fun onDayOptionSelected(option: OrderDayOption) {
        val current = _uiState.value
        if (option == OrderDayOption.SOON) {
            _uiState.value = current.copy(selectedDayOption = option, isSoon = true)
            return
        }
        val resolvedDate = when (option) {
            OrderDayOption.TODAY -> nowAtDayOffset(0)
            OrderDayOption.TOMORROW -> nowAtDayOffset(1)
            OrderDayOption.OTHER_DATE -> current.selectedDateMillis
            OrderDayOption.SOON -> current.selectedDateMillis
        }
        _uiState.value = current.copy(selectedDayOption = option, selectedDateMillis = resolvedDate, isSoon = false)
        validateAndAutoAdjustTime()
    }

    fun onDateSelected(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(
            selectedDateMillis = dateMillis,
            selectedDayOption = resolveDayOption(dateMillis),
            isSoon = false
        )
        validateAndAutoAdjustTime()
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(selectedHour = hour, selectedMinute = minute)
        validateAndAutoAdjustTime()
    }

    fun incrementHours() {
        val state = _uiState.value
        if (state.estimatedHours < OrderRules.MAX_ESTIMATED_HOURS) {
            _uiState.value = state.copy(estimatedHours = state.estimatedHours + 1)
        }
    }

    fun decrementHours() {
        val state = _uiState.value
        if (state.estimatedHours > OrderRules.MIN_ESTIMATED_HOURS) {
            _uiState.value = state.copy(estimatedHours = state.estimatedHours - 1)
        }
    }

    /**
     * Создать заказ. Идентификатор диспетчера-создателя берётся из [CurrentUserProvider]:
     * ViewModel сама знает контекст сессии и не требует его от UI.
     */
    fun createOrder(
        address: String,
        cargoDescription: String,
        pricePerHour: Double,
        requiredWorkers: Int,
        minWorkerRating: Float,
        comment: String
    ) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val exactDateTime = buildDateTimeMillis(state.selectedDateMillis, state.selectedHour, state.selectedMinute)
        val normalizedOrderTime = when {
            state.isSoon -> OrderTime.Soon
            exactDateTime <= now -> {
                showSnackbar("Нельзя выбрать прошедшее или текущее время")
                return
            }
            exactDateTime < now + MIN_EXACT_DELAY_MS -> OrderTime.Soon
            else -> OrderTime.Exact(exactDateTime)
        }

        launchSafe {
            val currentUser = currentUserProvider.requireCurrentUserOnce()

            val orderDraft = OrderDraft(
                title = cargoDescription.trim().ifBlank { "Заказ" },
                address = address.trim(),
                pricePerHour = pricePerHour,
                orderTime = normalizedOrderTime,
                durationMin = state.estimatedHours
                    .coerceIn(OrderRules.MIN_ESTIMATED_HOURS, OrderRules.MAX_ESTIMATED_HOURS) * 60,
                workersCurrent = 0,
                workersTotal = requiredWorkers,
                tags = listOf(cargoDescription.trim()).filter { it.isNotBlank() },
                meta = mapOf(
                    "dispatcherId" to currentUser.id,
                    "minWorkerRating" to minWorkerRating.coerceIn(0f, 5f).toString(),
                    Order.CREATED_AT_KEY to now.toString(),
                    Order.TIME_TYPE_KEY to if (normalizedOrderTime == OrderTime.Soon) Order.TIME_TYPE_SOON else "exact"
                ),
                comment = comment.trim()
            )

            when (val result = createOrderUseCase(orderDraft)) {
                is UseCaseResult.Success -> {
                    showSnackbar("Заказ создан успешно")
                    _navigationEvent.send(NavigationEvent.NavigateUp)
                }
                is UseCaseResult.Failure -> showSnackbar(result.reason)
            }
        }
    }

    private fun validateAndAutoAdjustTime() {
        val state = _uiState.value
        if (state.isSoon) return
        val now = System.currentTimeMillis()
        val selectedDateTime = buildDateTimeMillis(state.selectedDateMillis, state.selectedHour, state.selectedMinute)
        when {
            selectedDateTime <= now -> {
                val safe = defaultExactDateTime()
                _uiState.value = state.copy(
                    selectedDateMillis = safe.first,
                    selectedHour = safe.second,
                    selectedMinute = safe.third
                )
            }
            selectedDateTime < now + MIN_EXACT_DELAY_MS -> {
                _uiState.value = state.copy(selectedDayOption = OrderDayOption.SOON, isSoon = true)
            }
        }
    }

    private fun resolveDayOption(dateMillis: Long): OrderDayOption {
        val today = normalizeToStartOfDay(nowAtDayOffset(0))
        val tomorrow = normalizeToStartOfDay(nowAtDayOffset(1))
        val selected = normalizeToStartOfDay(dateMillis)
        return when (selected) {
            today -> OrderDayOption.TODAY
            tomorrow -> OrderDayOption.TOMORROW
            else -> OrderDayOption.OTHER_DATE
        }
    }

    private fun nowAtDayOffset(offset: Int): Long =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.timeInMillis

    private fun normalizeToStartOfDay(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun buildDateTimeMillis(dateMillis: Long, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun defaultState(): CreateOrderUiState {
        val exact = defaultExactDateTime()
        return CreateOrderUiState(
            selectedDateMillis = exact.first,
            selectedHour = exact.second,
            selectedMinute = exact.third,
            estimatedHours = OrderRules.MIN_ESTIMATED_HOURS
        )
    }

    private fun defaultExactDateTime(): Triple<Long, Int, Int> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + MIN_EXACT_DELAY_MS
            set(Calendar.MINUTE, ((get(Calendar.MINUTE) + 4) / 5) * 5)
            if (get(Calendar.MINUTE) == 60) {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Triple(calendar.timeInMillis, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private companion object {
        const val MIN_EXACT_DELAY_MS = 60 * 60 * 1000L
    }
}

data class CreateOrderUiState(
    val selectedDayOption: OrderDayOption = OrderDayOption.TODAY,
    val selectedDateMillis: Long,
    val selectedHour: Int,
    val selectedMinute: Int,
    val estimatedHours: Int,
    val isSoon: Boolean = false
)

enum class OrderDayOption {
    TODAY,
    TOMORROW,
    SOON,
    OTHER_DATE
}

sealed class NavigationEvent {
    data object NavigateUp : NavigationEvent()
}
