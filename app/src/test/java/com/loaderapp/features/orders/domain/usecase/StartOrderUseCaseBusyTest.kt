package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.OrdersLimits
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartOrderUseCaseBusyTest {

    @Test
    fun `start returns AssigneeAlreadyBusy and does not change order status`() = runBlocking {
        val repo = TrackingStartRepository(busyAssignments = mapOf("loader-1" to 99L))
        val useCase = StartOrderUseCase(
            repository = repo,
            currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER)),
            stateMachine = OrderStateMachine(OrdersLimits())
        )

        val result = useCase.start(orderId = 1L)

        assertTrue(result is StartOrderResult.AssigneeAlreadyBusy)
        assertEquals(0, repo.startCalls)
        assertEquals(OrderStatus.STAFFING, repo.currentStatus())
    }

    private class StaticCurrentUserProvider(currentUser: CurrentUser) : CurrentUserProvider {
        private val state = MutableStateFlow<CurrentUser?>(currentUser)
        override fun observeCurrentUser(): Flow<CurrentUser?> = state
        override suspend fun getCurrentUserOrNull(): CurrentUser? = state.value
        override suspend fun requireCurrentUserOnce(): CurrentUser = state.value ?: error("Current user is not selected")
    }

    private class TrackingStartRepository(
        private val busyAssignments: Map<String, Long>
    ) : OrdersRepository {
        private val state = MutableStateFlow(
            listOf(
                Order(
                    id = 1L,
                    title = "Order 1",
                    address = "Address",
                    pricePerHour = 100.0,
                    orderTime = OrderTime.Soon,
                    durationMin = 60,
                    workersCurrent = 0,
                    workersTotal = 1,
                    tags = emptyList(),
                    meta = mapOf(Order.CREATED_AT_KEY to "0"),
                    status = OrderStatus.STAFFING,
                    createdByUserId = "dispatcher-1",
                    applications = listOf(
                        OrderApplication(
                            orderId = 1L,
                            loaderId = "loader-1",
                            status = OrderApplicationStatus.SELECTED,
                            appliedAtMillis = 0L
                        )
                    )
                )
            )
        )

        var startCalls = 0

        override fun observeOrders(): Flow<List<Order>> = state
        override suspend fun createOrder(order: Order) = Unit
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit

        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
            startCalls += 1
            state.update { orders ->
                orders.map { order ->
                    if (order.id == orderId) order.copy(status = OrderStatus.IN_PROGRESS) else order
                }
            }
        }

        override suspend fun hasActiveAssignment(loaderId: String): Boolean = busyAssignments.containsKey(loaderId)
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> =
            busyAssignments.filterKeys { it in loaderIds }
        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean = false
        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = 0

        fun currentStatus(): OrderStatus = state.value.first().status
    }
}
