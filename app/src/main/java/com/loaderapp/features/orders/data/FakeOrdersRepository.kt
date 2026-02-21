package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTransitionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeOrdersRepository @Inject constructor() : OrdersRepository {
    private val orders = MutableStateFlow<List<Order>>(emptyList())
    private val idGenerator = AtomicLong(1)

    override fun observeOrders(): Flow<List<Order>> = orders.asStateFlow()

    override suspend fun createOrder(order: Order) {
        simulateLatency()
        val resolvedId = if (order.id > 0) {
            idGenerator.updateAndGet { current ->
                maxOf(current, order.id + 1)
            }
            order.id
        } else {
            idGenerator.getAndIncrement()
        }

        val newOrder = order.copy(
            id = resolvedId,
            status = OrderStatus.AVAILABLE
        )
        orders.update { current ->
            current + newOrder
        }
    }

    override suspend fun acceptOrder(id: Long) {
        simulateLatency()
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.IN_PROGRESS)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        simulateLatency()
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.CANCELED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun completeOrder(id: Long) {
        simulateLatency()
        mutateOrder(id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.COMPLETED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun refresh() {
        simulateLatency()
        val now = System.currentTimeMillis()
        orders.update { current ->
            current.map { order ->
                if (order.status == OrderStatus.AVAILABLE && order.dateTime < now) {
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

    private fun mutateOrder(id: Long, transform: (Order) -> Order) {
        orders.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                return@update current
            }

            current.mapIndexed { currentIndex, order ->
                if (currentIndex == index) transform(order) else order
            }
        }
    }

    private suspend fun simulateLatency() {
        delay(Random.nextLong(150L, 401L))
    }
}
