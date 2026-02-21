package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTransitionResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class OrdersRepositoryImpl @Inject constructor() : OrdersRepository {
    private val ordersFlow = MutableStateFlow(seedOrders())

    override fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    override suspend fun createOrder(order: Order) {
        val nextId = (ordersFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val createdOrder = order.copy(id = nextId, status = OrderStatus.AVAILABLE)
        ordersFlow.update { current -> current + createdOrder }
    }

    override suspend fun acceptOrder(id: Long) {
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.IN_PROGRESS)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.CANCELED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun completeOrder(id: Long) {
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.COMPLETED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun refresh() {
        val threshold = System.currentTimeMillis() + ONE_HOUR_MS
        ordersFlow.update { current ->
            current.map { order ->
                if (order.status == OrderStatus.AVAILABLE && order.dateTime < threshold) {
                    when (val result = OrderStateMachine.transition(order, OrderStatus.EXPIRED)) {
                        is OrderTransitionResult.Success -> result.order
                        is OrderTransitionResult.Failure -> order
                    }
                } else {
                    order
                }
            }
        }
    }

    private fun mutateOrder(id: Long, mutate: (Order) -> Order) {
        ordersFlow.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                return@update current
            }
            current.mapIndexed { orderIndex, order ->
                if (orderIndex == index) mutate(order) else order
            }
        }
    }

    private fun seedOrders(): List<Order> {
        val now = System.currentTimeMillis()
        return listOf(
            Order(1, "Разгрузка фуры", "Москва, Тверская 12", 700.0, now + 20 * 60_000, 180, 0, 3, listOf("Хрупкое"), mapOf("cargo" to "Бытовая техника"), "Позвонить за 15 минут", OrderStatus.AVAILABLE),
            Order(2, "Переезд офиса", "Москва, Арбат 18", 850.0, now + 2 * 60 * 60_000, 240, 1, 4, listOf("Подъем"), mapOf("floor" to "5"), null, OrderStatus.AVAILABLE),
            Order(3, "Паллеты на склад", "Химки, Заводская 7", 650.0, now + 3 * 60 * 60_000, 120, 1, 2, listOf("Срочно"), mapOf("dock" to "B2"), "Въезд со двора", OrderStatus.IN_PROGRESS),
            Order(4, "Разбор мебели", "Мытищи, Юбилейная 9", 600.0, now - 6 * 60 * 60_000, 180, 2, 2, listOf("Инструмент"), mapOf("type" to "шкафы"), null, OrderStatus.COMPLETED),
            Order(5, "Срочная выгрузка", "Москва, Лобачевского 24", 900.0, now - 90 * 60_000, 90, 0, 2, listOf("Срочно"), emptyMap(), "Опоздание критично", OrderStatus.EXPIRED)
        )
    }

    private companion object {
        const val ONE_HOUR_MS = 60 * 60 * 1000L
    }
}
