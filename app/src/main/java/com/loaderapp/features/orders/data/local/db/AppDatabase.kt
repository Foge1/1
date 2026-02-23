package com.loaderapp.features.orders.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.data.local.entity.OrdersConverters

@Database(
    entities = [
        OrderEntity::class,
        OrderApplicationEntity::class,
        OrderAssignmentEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(OrdersConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao
}

