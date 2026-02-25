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

class SelectUnselectApplicantUseCaseTest {

    @Test
    fun `select and unselect call repository and update order state`() = runBlocking {
        val repo = TrackingOrdersRepository()
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER))
        val stateMachine = OrderStateMachine(OrdersLimits())

        val select = SelectApplicantUseCase(repo, currentUserProvider, stateMachine)
        val unselect = UnselectApplicantUseCase(repo, currentUserProvider, stateMachine)

        val selectResult = select(1L, "loader-1")
        assertTrue(selectResult is UseCaseResult.Success)
        assertEquals(1, repo.selectCalls)
        assertEquals(OrderApplicationStatus.SELECTED, repo.currentApplicationStatus())

        val unselectResult = unselect(1L, "loader-1")
        assertTrue(unselectResult is UseCaseResult.Success)
        assertEquals(1, repo.unselectCalls)
        assertEquals(OrderApplicationStatus.APPLIED, repo.currentApplicationStatus())
    }


    @Test
    fun `select returns AlreadyAssigned and does not mutate repository when loader is busy`() = runBlocking {
        val repo = TrackingOrdersRepository(busyAssignments = mapOf("loader-1" to 77L))
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER))
        val stateMachine = OrderStateMachine(OrdersLimits())

        val select = SelectApplicantUseCase(repo, currentUserProvider, stateMachine)

        val result = select.select(1L, "loader-1")

        assertTrue(result is SelectApplicantResult.AlreadyAssigned)
        assertEquals(0, repo.selectCalls)
        assertEquals(OrderApplicationStatus.APPLIED, repo.currentApplicationStatus())
    }
    private class StaticCurrentUserProvider(currentUser: CurrentUser) : CurrentUserProvider {
        private val state = MutableStateFlow<CurrentUser?>(currentUser)
        override fun observeCurrentUser(): Flow<CurrentUser?> = state
        override suspend fun getCurrentUserOrNull(): CurrentUser? = state.value
        override suspend fun requireCurrentUserOnce(): CurrentUser = state.value ?: error("Current user is not selected")
    }

    private class TrackingOrdersRepository(
        private val busyAssignments: Map<String, Long> = emptyMap()
    ) : OrdersRepository {
        private val state = MutableStateFlow(listOf(baseOrder()))
        var selectCalls: Int = 0
        var unselectCalls: Int = 0

        override fun observeOrders(): Flow<List<Order>> = state

        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }

        override suspend fun selectApplicant(orderId: Long, loaderId: String) {
            selectCalls += 1
            state.update { orders ->
                orders.map { order ->
                    if (order.id != orderId) return@map order
                    order.copy(applications = order.applications.map { application ->
                        if (application.loaderId == loaderId) {
                            application.copy(status = OrderApplicationStatus.SELECTED)
                        } else application
                    })
                }
            }
        }

        override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
            unselectCalls += 1
            state.update { orders ->
                orders.map { order ->
                    if (order.id != orderId) return@map order
                    order.copy(applications = order.applications.map { application ->
                        if (application.loaderId == loaderId) {
                            application.copy(status = OrderApplicationStatus.APPLIED)
                        } else application
                    })
                }
            }
        }

        fun currentApplicationStatus(): OrderApplicationStatus =
            state.value.first().applications.first().status

        override suspend fun createOrder(order: Order) = Unit
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit
        override suspend fun hasActiveAssignment(loaderId: String): Boolean = busyAssignments.containsKey(loaderId)
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> = busyAssignments.filterKeys { it in loaderIds }
        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean = false
        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = 0
    }

    companion object {
        private fun baseOrder() = Order(
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
                    status = OrderApplicationStatus.APPLIED,
                    appliedAtMillis = 0L
                )
            )
        )
    }
}
