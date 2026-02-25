package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalOrderFlowUseCaseTest {

    @Test
    fun `Given blank title When create order Then returns validation failure`() = runTest {
        val repository = InMemoryOrdersRepository()
        val useCase = CreateOrderUseCase(
            ordersRepository = repository,
            currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER))
        )

        val result = useCase(OrderDraftFactory.create(title = " "))

        assertTrue(result is UseCaseResult.Failure)
        assertEquals("Название заказа не может быть пустым", (result as UseCaseResult.Failure).reason)
    }

    @Test
    fun `Given busy loader in another order When select applicant Then returns already assigned`() = runTest {
        val repository = InMemoryOrdersRepository(
            orders = listOf(
                staffingOrder(orderId = 1L, selected = false),
                inProgressOrder(orderId = 2L, loaderId = "loader-1")
            )
        )
        val useCase = SelectApplicantUseCase(
            repository = repository,
            currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER)),
            stateMachine = OrderStateMachine(OrdersLimits())
        )

        val result = useCase.select(orderId = 1L, loaderId = "loader-1")

        assertTrue(result is SelectApplicantResult.AlreadyAssigned)
    }

    @Test
    fun `Given selected busy loader When start order Then returns assignee busy failure`() = runTest {
        val repository = InMemoryOrdersRepository(
            orders = listOf(
                staffingOrder(orderId = 1L, selected = true),
                inProgressOrder(orderId = 2L, loaderId = "loader-1")
            )
        )
        val useCase = StartOrderUseCase(
            repository = repository,
            currentUserProvider = StaticCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER)),
            stateMachine = OrderStateMachine(OrdersLimits())
        )

        val result = useCase.start(orderId = 1L, now = 1000L)

        assertTrue(result is StartOrderResult.AssigneeAlreadyBusy)
    }

    @Test
    fun `Given loader without active assignment When complete order Then returns forbidden failure`() = runTest {
        val repository = InMemoryOrdersRepository(
            orders = listOf(
                staffingOrder(orderId = 1L, selected = false).copy(status = OrderStatus.IN_PROGRESS)
            )
        )
        val useCase = CompleteOrderUseCase(
            repository = repository,
            currentUserProvider = StaticCurrentUserProvider(CurrentUser("loader-2", Role.LOADER)),
            stateMachine = OrderStateMachine(OrdersLimits())
        )

        val result = useCase(orderId = 1L)

        assertTrue(result is UseCaseResult.Failure)
        assertTrue((result as UseCaseResult.Failure).reason.isNotBlank())
    }

    private class StaticCurrentUserProvider(
        private val user: CurrentUser
    ) : CurrentUserProvider {
        private val state = MutableStateFlow<CurrentUser?>(user)
        override fun observeCurrentUser(): Flow<CurrentUser?> = state
        override suspend fun getCurrentUserOrNull(): CurrentUser? = state.value
        override suspend fun requireCurrentUserOnce(): CurrentUser = state.value ?: error("No user")
    }

    private class InMemoryOrdersRepository(
        orders: List<Order> = emptyList()
    ) : OrdersRepository {
        private val state = MutableStateFlow(orders)

        override fun observeOrders(): Flow<List<Order>> = state
        override suspend fun createOrder(order: Order) { state.update { it + order } }
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) {
            state.update { list -> list.map { if (it.id == id) it.copy(status = OrderStatus.COMPLETED) else it } }
        }
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) {
            state.update { orders ->
                orders.map { order ->
                    if (order.id != orderId) order else order.copy(
                        applications = order.applications.map { app ->
                            if (app.loaderId == loaderId) app.copy(status = OrderApplicationStatus.SELECTED) else app
                        }
                    )
                }
            }
        }
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
            state.update { list -> list.map { if (it.id == orderId) it.copy(status = OrderStatus.IN_PROGRESS) else it } }
        }
        override suspend fun hasActiveAssignment(loaderId: String): Boolean =
            state.value.any { order ->
                order.status == OrderStatus.IN_PROGRESS &&
                    order.assignments.any { it.loaderId == loaderId && it.status == OrderAssignmentStatus.ACTIVE }
            }
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> =
            state.value.asSequence()
                .filter { it.status == OrderStatus.IN_PROGRESS }
                .flatMap { order ->
                    order.assignments.asSequence()
                        .filter { it.status == OrderAssignmentStatus.ACTIVE && it.loaderId in loaderIds }
                        .map { it.loaderId to order.id }
                }
                .toMap()

        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean =
            state.value.firstOrNull { it.id == orderId }
                ?.assignments
                ?.any { it.loaderId == loaderId && it.status == OrderAssignmentStatus.ACTIVE }
                ?: false

        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = 0
    }

    private object OrderDraftFactory {
        fun create(title: String) = com.loaderapp.features.orders.domain.OrderDraft(
            title = title,
            address = "Address",
            pricePerHour = 100.0,
            orderTime = OrderTime.Soon,
            durationMin = 60,
            workersCurrent = 0,
            workersTotal = 1,
            tags = emptyList(),
            meta = emptyMap(),
            comment = null
        )
    }

    private fun staffingOrder(orderId: Long, selected: Boolean): Order {
        val status = if (selected) OrderApplicationStatus.SELECTED else OrderApplicationStatus.APPLIED
        return Order(
            id = orderId,
            title = "Order $orderId",
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
                    orderId = orderId,
                    loaderId = "loader-1",
                    status = status,
                    appliedAtMillis = 0L
                )
            )
        )
    }

    private fun inProgressOrder(orderId: Long, loaderId: String): Order = Order(
        id = orderId,
        title = "Busy order",
        address = "Address",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 1,
        workersTotal = 1,
        tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"),
        status = OrderStatus.IN_PROGRESS,
        createdByUserId = "dispatcher-2",
        assignments = listOf(
            com.loaderapp.features.orders.domain.OrderAssignment(
                orderId = orderId,
                loaderId = loaderId,
                status = OrderAssignmentStatus.ACTIVE,
                assignedAtMillis = 0L,
                startedAtMillis = 0L
            )
        )
    )
}
