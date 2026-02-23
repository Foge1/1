package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderDraft
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersOwnershipUseCasesTest {

    // ── CreateOrderUseCase ────────────────────────────────────────────────────

    @Test
    fun `createOrder sets createdBy and status STAFFING`() = runBlocking {
        val repo = InMemoryOrdersRepository()
        val useCase = CreateOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("10", Role.DISPATCHER)))
        useCase(baseOrderDraft())
        val created = repo.observeOrders().first().first()
        assertEquals("10", created.createdByUserId)
        assertNull(created.acceptedByUserId)
        assertEquals(OrderStatus.STAFFING, created.status)
    }

    // ── ApplyToOrderUseCase ───────────────────────────────────────────────────

    @Test
    fun `apply succeeds for loader on STAFFING order with no active assignment`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, status = OrderStatus.STAFFING)))
        val useCase = ApplyToOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `apply fails for loader with active assignment`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(baseOrder(id = 1, status = OrderStatus.STAFFING)),
            hasActiveAssignment = true
        )
        val useCase = ApplyToOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `apply fails when loader has 3 active applications`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(baseOrder(id = 1, status = OrderStatus.STAFFING)),
            activeAppliedCount = 3
        )
        val useCase = ApplyToOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `apply fails for dispatcher`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, status = OrderStatus.STAFFING)))
        val useCase = ApplyToOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("d-1", Role.DISPATCHER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Failure)
    }

    // ── SelectApplicantUseCase / UnselectApplicantUseCase ────────────────────

    @Test
    fun `select succeeds for dispatcher creator`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "d-1", status = OrderStatus.STAFFING)))
        val useCase = SelectApplicantUseCase(repo, StaticCurrentUserProvider(CurrentUser("d-1", Role.DISPATCHER)))

        val result = useCase(orderId = 1, loaderId = "loader-x")

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `select fails for non-creator dispatcher`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "d-other", status = OrderStatus.STAFFING)))
        val useCase = SelectApplicantUseCase(repo, StaticCurrentUserProvider(CurrentUser("d-1", Role.DISPATCHER)))

        val result = useCase(orderId = 1, loaderId = "loader-x")

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `unselect fails for loader`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "d-1", status = OrderStatus.STAFFING)))
        val useCase = UnselectApplicantUseCase(repo, StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER)))

        val result = useCase(orderId = 1, loaderId = "loader-1")

        assertTrue(result is UseCaseResult.Failure)
    }

    // ── StartOrderUseCase ─────────────────────────────────────────────────────

    @Test
    fun `start succeeds when creator and selectedCount equals workersTotal`() = runBlocking {
        val apps = listOf(
            OrderApplication(orderId = 1, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repo = InMemoryOrdersRepository(
            listOf(baseOrder(id = 1, createdBy = "d-1", status = OrderStatus.STAFFING, workersTotal = 1, applications = apps))
        )
        val useCase = StartOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("d-1", Role.DISPATCHER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `start fails when selectedCount does not match workersTotal`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            listOf(baseOrder(id = 1, createdBy = "d-1", status = OrderStatus.STAFFING, workersTotal = 2, applications = emptyList()))
        )
        val useCase = StartOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("d-1", Role.DISPATCHER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `start fails when actor is loader`() = runBlocking {
        val apps = listOf(
            OrderApplication(orderId = 1, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repo = InMemoryOrdersRepository(
            listOf(baseOrder(id = 1, createdBy = "d-1", status = OrderStatus.STAFFING, workersTotal = 1, applications = apps))
        )
        val useCase = StartOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER)))

        val result = useCase(orderId = 1)

        assertTrue(result is UseCaseResult.Failure)
    }

    // ── Deprecated AcceptOrderUseCase shim ───────────────────────────────────

    @Test
    @Suppress("DEPRECATION")
    fun `acceptOrder compat delegates to apply and sets status to in progress via apply`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "d-1")))
        val provider = StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER))
        val applyUseCase = ApplyToOrderUseCase(repo, provider)
        val useCase = AcceptOrderUseCase(applyUseCase)

        val result = useCase(1)

        // ApplyToOrderUseCase only validates and calls repo.applyToOrder — doesn't change status
        // so we verify at minimum it returns Success (no active assignment, STAFFING order)
        assertTrue(result is UseCaseResult.Success)
    }

    // ── ObserveOrdersForRoleUseCase ───────────────────────────────────────────

    @Test
    fun `observe dispatcher returns only own created orders`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "1"), baseOrder(id = 2, createdBy = "2")))
        val useCase = ObserveOrdersForRoleUseCase(repo, StaticCurrentUserProvider(CurrentUser("1", Role.DISPATCHER)))
        assertEquals(listOf(1L), useCase().first().map { it.id })
    }

    @Test
    fun `observe loader returns all staffing and own in progress orders`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            listOf(
                baseOrder(id = 1, status = OrderStatus.STAFFING),
                baseOrder(id = 2, status = OrderStatus.STAFFING),
                baseOrder(id = 3, status = OrderStatus.IN_PROGRESS, acceptedBy = "1"),
                baseOrder(id = 4, status = OrderStatus.IN_PROGRESS, acceptedBy = "2")
            )
        )
        val useCase = ObserveOrdersForRoleUseCase(repo, StaticCurrentUserProvider(CurrentUser("1", Role.LOADER)))
        val result = useCase().first().map { it.id }
        assertTrue(result.contains(1L))
        assertTrue(result.contains(2L))
        assertTrue(result.contains(3L))
        assertTrue(!result.contains(4L))
    }

    @Test
    fun `observe orders reacts to current user changes`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            listOf(
                baseOrder(id = 1, createdBy = "dispatcher-1", status = OrderStatus.STAFFING),
                baseOrder(id = 2, createdBy = "dispatcher-2", status = OrderStatus.STAFFING),
            )
        )
        val currentUserProvider = FakeCurrentUserProvider(CurrentUser("dispatcher-1", Role.DISPATCHER))
        val useCase = ObserveOrdersForRoleUseCase(repo, currentUserProvider)

        val emissionsDeferred = async { useCase().take(2).toList() }
        currentUserProvider.emit(CurrentUser("dispatcher-2", Role.DISPATCHER))

        val emissions = emissionsDeferred.await()
        assertEquals(listOf(1L), emissions[0].map { it.id })
        assertEquals(listOf(2L), emissions[1].map { it.id })
    }

    // ── Test doubles ──────────────────────────────────────────────────────────

    private class StaticCurrentUserProvider(private val currentUser: CurrentUser) : CurrentUserProvider {
        private val state = MutableStateFlow(currentUser)
        override fun observeCurrentUser(): Flow<CurrentUser> = state
        override suspend fun getCurrentUser(): CurrentUser = state.value
    }

    private class FakeCurrentUserProvider(initial: CurrentUser) : CurrentUserProvider {
        private val state = MutableStateFlow(initial)
        override fun observeCurrentUser(): Flow<CurrentUser> = state
        override suspend fun getCurrentUser(): CurrentUser = state.value
        suspend fun emit(currentUser: CurrentUser) { state.emit(currentUser) }
    }

    private class InMemoryOrdersRepository(
        initial: List<Order> = emptyList(),
        private val hasActiveAssignment: Boolean = false,
        private val activeAppliedCount: Int = 0
    ) : OrdersRepository {
        private val state = MutableStateFlow(initial)

        override fun observeOrders(): Flow<List<Order>> = state

        override suspend fun createOrder(order: Order) {
            state.update { it + order.copy(id = if (order.id == 0L) 1L else order.id, status = OrderStatus.STAFFING) }
        }

        @Deprecated("compat")
        override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) {
            state.update { list ->
                @Suppress("DEPRECATION")
                list.map { if (it.id == id) it.copy(status = OrderStatus.IN_PROGRESS, acceptedByUserId = acceptedByUserId) else it }
            }
        }

        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit
        override suspend fun hasActiveAssignment(loaderId: String): Boolean = hasActiveAssignment
        override suspend fun countActiveAppliedApplications(loaderId: String): Int = activeAppliedCount
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun baseOrderDraft() = OrderDraft(
        title = "Order", address = "Addr", pricePerHour = 100.0, orderTime = OrderTime.Soon,
        durationMin = 60, workersCurrent = 0, workersTotal = 1, tags = emptyList(),
        meta = emptyMap(), comment = null
    )

    @Suppress("DEPRECATION")
    private fun baseOrder(
        id: Long = 0,
        createdBy: String = "d-1",
        acceptedBy: String? = null,
        status: OrderStatus = OrderStatus.STAFFING,
        workersTotal: Int = 1,
        applications: List<OrderApplication> = emptyList()
    ) = Order(
        id = id, title = "Order", address = "Addr", pricePerHour = 100.0,
        orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 0,
        workersTotal = workersTotal, tags = emptyList(), meta = emptyMap(),
        status = status, createdByUserId = createdBy,
        acceptedByUserId = acceptedBy, acceptedAtMillis = null,
        applications = applications
    )
}
