package com.loaderapp.presentation.dispatcher

import com.loaderapp.domain.model.OrderRules
import com.loaderapp.presentation.base.BaseViewModel
import com.loaderapp.features.orders.data.OrdersRepository
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : BaseViewModel() {

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val now = Calendar.getInstance()
    private val _uiState = MutableStateFlow(
        CreateOrderUiState(
            selectedDateMillis = now.timeInMillis,
            selectedHour = now.get(Calendar.HOUR_OF_DAY),
            selectedMinute = now.get(Calendar.MINUTE),
            estimatedHours = OrderRules.MIN_ESTIMATED_HOURS
        )
    )
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    fun onDayOptionSelected(option: OrderDayOption) {
        val current = _uiState.value
        val resolvedDate = when (option) {
            OrderDayOption.TODAY -> nowAtDayOffset(0)
            OrderDayOption.TOMORROW -> nowAtDayOffset(1)
            OrderDayOption.OTHER_DATE -> current.selectedDateMillis
        }
        _uiState.value = current.copy(selectedDayOption = option, selectedDateMillis = resolvedDate)
    }

    fun onDateSelected(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(
            selectedDateMillis = dateMillis,
            selectedDayOption = resolveDayOption(dateMillis)
        )
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(selectedHour = hour, selectedMinute = minute)
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

    fun createOrder(
        dispatcherId: Long,
        address: String,
        cargoDescription: String,
        pricePerHour: Double,
        requiredWorkers: Int,
        minWorkerRating: Float,
        comment: String
    ) {
        val state = _uiState.value
        val order = Order(
            id = 0,
            title = cargoDescription.trim().ifBlank { "Заказ" },
            address = address.trim(),
            pricePerHour = pricePerHour,
            dateTime = buildDateTimeMillis(state.selectedDateMillis, state.selectedHour, state.selectedMinute),
            durationMin = state.estimatedHours.coerceIn(OrderRules.MIN_ESTIMATED_HOURS, OrderRules.MAX_ESTIMATED_HOURS) * 60,
            workersCurrent = 0,
            workersTotal = requiredWorkers,
            tags = listOf(cargoDescription.trim()).filter { it.isNotBlank() },
            meta = mapOf("dispatcherId" to dispatcherId.toString(), "minWorkerRating" to minWorkerRating.coerceIn(0f, 5f).toString()),
            comment = comment.trim(),
            status = OrderStatus.AVAILABLE
        )

        launchSafe {
            ordersRepository.createOrder(order)
            showSnackbar("Заказ создан успешно")
            _navigationEvent.send(NavigationEvent.NavigateUp)
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

    private fun nowAtDayOffset(offset: Int): Long {
        return Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.timeInMillis
    }

    private fun normalizeToStartOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun buildDateTimeMillis(dateMillis: Long, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

data class CreateOrderUiState(
    val selectedDayOption: OrderDayOption = OrderDayOption.TODAY,
    val selectedDateMillis: Long,
    val selectedHour: Int,
    val selectedMinute: Int,
    val estimatedHours: Int
)

enum class OrderDayOption {
    TODAY,
    TOMORROW,
    OTHER_DATE
}

sealed class NavigationEvent {
    data object NavigateUp : NavigationEvent()
}
