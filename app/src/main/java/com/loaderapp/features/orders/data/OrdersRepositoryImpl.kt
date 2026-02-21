package com.loaderapp.features.orders.data

import android.util.Log
import com.loaderapp.BuildConfig
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
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    override fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    override suspend fun createOrder(order: Order) {
        val nextId = (ordersFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val createdOrder = order.copy(id = nextId, status = OrderStatus.AVAILABLE)
        ordersFlow.update { current -> current + createdOrder }
        logDebug("createOrder", createdOrder.id, createdOrder.status)
    }

    override suspend fun acceptOrder(id: Long) {
        mutateOrder("acceptOrder", id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.IN_PROGRESS)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        mutateOrder("cancelOrder", id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.CANCELED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun completeOrder(id: Long) {
        mutateOrder("completeOrder", id) { order ->
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
        if (BuildConfig.DEBUG) {
            ordersFlow.value.forEach { order ->
                logDebug("refresh", order.id, order.status)
            }
        }
    }

    private fun mutateOrder(action: String, id: Long, mutate: (Order) -> Order) {
        var updatedStatus: OrderStatus? = null
        ordersFlow.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                return@update current
            }
            current.mapIndexed { orderIndex, order ->
                if (orderIndex == index) {
                    val updated = mutate(order)
                    updatedStatus = updated.status
                    updated
                } else {
                    order
                }
            }
        }
        updatedStatus?.let { status -> logDebug(action, id, status) }
    }

    private fun logDebug(action: String, orderId: Long, status: OrderStatus) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "$action: id=$orderId, status=$status")
    }

    private companion object {
        const val ONE_HOUR_MS = 60 * 60 * 1000L
        const val TAG = "OrdersRepository"
    }
}
