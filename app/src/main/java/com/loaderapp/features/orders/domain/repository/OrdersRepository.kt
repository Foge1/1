package com.loaderapp.features.orders.domain.repository

import com.loaderapp.features.orders.domain.Order
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    // ── Observation ────────────────────────────────────────────────────────────
    fun observeOrders(): Flow<List<Order>>

    // ── Order lifecycle ────────────────────────────────────────────────────────
    suspend fun createOrder(order: Order)
    suspend fun cancelOrder(id: Long, reason: String? = null)
    suspend fun completeOrder(id: Long)
    suspend fun refresh()
    suspend fun getOrderById(id: Long): Order?

    // ── Application flow (gruzchik side) ──────────────────────────────────────
    suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long)
    suspend fun withdrawApplication(orderId: Long, loaderId: String)

    // ── Selection flow (dispatcher side) ──────────────────────────────────────
    suspend fun selectApplicant(orderId: Long, loaderId: String)
    suspend fun unselectApplicant(orderId: Long, loaderId: String)

    // ── Start (dispatcher triggers, creates assignments) ───────────────────────
    suspend fun startOrder(orderId: Long, startedAtMillis: Long)

    // ── Invariant helpers ─────────────────────────────────────────────────────
    suspend fun hasActiveAssignment(loaderId: String): Boolean
    suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long>
    suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean
    suspend fun countActiveApplicationsForLimit(loaderId: String): Int

}
