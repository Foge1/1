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
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_applications")
    fun observeApplications(): Flow<List<OrderApplicationEntity>>

    @Query("SELECT * FROM order_assignments")
    fun observeAssignments(): Flow<List<OrderAssignmentEntity>>

    @Query("SELECT * FROM orders")
    suspend fun getOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId")
    suspend fun getApplicationsByOrder(orderId: Long): List<OrderApplicationEntity>

    @Query("SELECT * FROM order_assignments WHERE orderId = :orderId")
    suspend fun getAssignmentsByOrder(orderId: Long): List<OrderAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: OrderApplicationEntity)

    @Query("UPDATE order_applications SET status = :status WHERE orderId = :orderId AND loaderId = :loaderId")
    suspend fun updateApplicationStatus(orderId: Long, loaderId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<OrderAssignmentEntity>)

    @Query("UPDATE order_applications SET status = :toStatus WHERE orderId = :orderId AND status = :fromStatus")
    suspend fun updateApplicationsStatusByOrder(orderId: Long, fromStatus: String, toStatus: String)

    @Query("UPDATE order_assignments SET status = :newStatus WHERE orderId = :orderId")
    suspend fun updateAssignmentsStatusByOrder(orderId: Long, newStatus: String)

    @Query("SELECT COUNT(*) FROM order_assignments WHERE loaderId = :loaderId AND status = :status")
    suspend fun countAssignmentsByLoaderAndStatus(loaderId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM order_applications WHERE loaderId = :loaderId AND status = :status")
    suspend fun countApplicationsByLoaderAndStatus(loaderId: String, status: String): Int

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId AND loaderId = :loaderId LIMIT 1")
    suspend fun getApplication(orderId: Long, loaderId: String): OrderApplicationEntity?
}
