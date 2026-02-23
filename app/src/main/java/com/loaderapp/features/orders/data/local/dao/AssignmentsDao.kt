package com.loaderapp.features.orders.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentsDao {
    @Query("SELECT * FROM order_assignments ORDER BY assignedAtMillis DESC")
    fun observeAssignments(): Flow<List<OrderAssignmentEntity>>

    @Query("SELECT * FROM order_assignments WHERE orderId = :orderId")
    suspend fun getAssignmentsByOrder(orderId: Long): List<OrderAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<OrderAssignmentEntity>)

    @Query("UPDATE order_assignments SET status = :newStatus WHERE orderId = :orderId")
    suspend fun updateAssignmentsStatusByOrder(orderId: Long, newStatus: String)

    @Query("SELECT COUNT(*) FROM order_assignments WHERE loaderId = :loaderId AND status = :status")
    suspend fun countAssignmentsByLoaderAndStatus(loaderId: String, status: String): Int
}
