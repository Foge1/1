package com.loaderapp.features.orders.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationsDao {
    @Query("SELECT * FROM order_applications ORDER BY appliedAtMillis DESC")
    fun observeApplications(): Flow<List<OrderApplicationEntity>>

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId")
    suspend fun getApplicationsByOrder(orderId: Long): List<OrderApplicationEntity>

    @Query("SELECT * FROM order_applications WHERE orderId = :orderId AND loaderId = :loaderId LIMIT 1")
    suspend fun getApplication(orderId: Long, loaderId: String): OrderApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: OrderApplicationEntity)

    @Query("UPDATE order_applications SET status = :newStatus WHERE orderId = :orderId AND loaderId = :loaderId")
    suspend fun updateApplicationStatus(orderId: Long, loaderId: String, newStatus: String)

    @Query("UPDATE order_applications SET status = :toStatus WHERE orderId = :orderId AND status = :fromStatus")
    suspend fun updateApplicationsStatusByOrder(orderId: Long, fromStatus: String, toStatus: String)

    @Query("SELECT COUNT(*) FROM order_applications WHERE loaderId = :loaderId AND status IN (:statuses)")
    suspend fun countApplicationsByLoaderAndStatuses(loaderId: String, statuses: List<String>): Int
}
