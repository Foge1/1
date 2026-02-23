package com.loaderapp.features.orders.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdersDao {

    // ── Orders ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders")
    suspend fun getOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    // ── Applications ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM order_applications ORDER BY appliedAtMillis DESC")
    fun observeApplications(): Flow<List<OrderApplicationEntity>>

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId")
    suspend fun getApplicationsByOrder(orderId: Long): List<OrderApplicationEntity>

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId AND loaderId = :loaderId LIMIT 1")
    suspend fun getApplication(orderId: Long, loaderId: String): OrderApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: OrderApplicationEntity)

    @Query(
        """
        UPDATE order_applications
        SET status = :newStatus
        WHERE orderId = :orderId AND loaderId = :loaderId
        """
    )
    suspend fun updateApplicationStatus(orderId: Long, loaderId: String, newStatus: String)

    /**
     * Bulk-transition applications for a given order from [fromStatus] to [toStatus].
     * Used e.g. to REJECT all APPLIED applicants when order starts.
     */
    @Query(
        """
        UPDATE order_applications
        SET status = :toStatus
        WHERE orderId = :orderId AND status = :fromStatus
        """
    )
    suspend fun updateApplicationsStatusByOrder(orderId: Long, fromStatus: String, toStatus: String)

    /**
     * Count APPLIED (or any status) applications for a specific loader.
     * Used for the "max 3 active applications" invariant check.
     */
    @Query(
        """
        SELECT COUNT(*) FROM order_applications
        WHERE loaderId = :loaderId AND status = :status
        """
    )
    suspend fun countApplicationsByLoaderAndStatus(loaderId: String, status: String): Int

    // ── Assignments ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM order_assignments ORDER BY assignedAtMillis DESC")
    fun observeAssignments(): Flow<List<OrderAssignmentEntity>>

    @Query("SELECT * FROM order_assignments WHERE orderId = :orderId")
    suspend fun getAssignmentsByOrder(orderId: Long): List<OrderAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<OrderAssignmentEntity>)

    /**
     * Bulk-update status for all assignments of a given order.
     * Used when order is canceled or completed.
     */
    @Query(
        """
        UPDATE order_assignments
        SET status = :newStatus
        WHERE orderId = :orderId
        """
    )
    suspend fun updateAssignmentsStatusByOrder(orderId: Long, newStatus: String)

    /**
     * Count ACTIVE assignments for a specific loader.
     * Used for the "max 1 active order" invariant check.
     */
    @Query(
        """
        SELECT COUNT(*) FROM order_assignments
        WHERE loaderId = :loaderId AND status = :status
        """
    )
    suspend fun countAssignmentsByLoaderAndStatus(loaderId: String, status: String): Int
}

