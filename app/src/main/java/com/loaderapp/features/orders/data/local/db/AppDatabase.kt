package com.loaderapp.features.orders.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    companion object {
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE orders RENAME TO orders_old")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        address TEXT NOT NULL,
                        pricePerHour REAL NOT NULL,
                        orderTimeType TEXT NOT NULL,
                        orderTimeExactMillis INTEGER,
                        durationMin INTEGER NOT NULL,
                        workersCurrent INTEGER NOT NULL,
                        workersTotal INTEGER NOT NULL,
                        tags TEXT NOT NULL,
                        meta TEXT NOT NULL,
                        comment TEXT,
                        status TEXT NOT NULL,
                        createdByUserId TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO orders (
                        id, title, address, pricePerHour, orderTimeType, orderTimeExactMillis,
                        durationMin, workersCurrent, workersTotal, tags, meta, comment, status, createdByUserId
                    )
                    SELECT
                        id, title, address, pricePerHour, orderTimeType, orderTimeExactMillis,
                        durationMin, workersCurrent, workersTotal, tags, meta, comment,
                        CASE
                            WHEN status = 'AVAILABLE' THEN 'STAFFING'
                            WHEN status = 'TAKEN' THEN 'IN_PROGRESS'
                            ELSE status
                        END,
                        createdByUserId
                    FROM orders_old
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE orders_old")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_applications (
                        orderId INTEGER NOT NULL,
                        loaderId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        appliedAtMillis INTEGER NOT NULL,
                        ratingSnapshot REAL,
                        PRIMARY KEY(orderId, loaderId)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_applications_loaderId_status ON order_applications (loaderId, status)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_assignments (
                        orderId INTEGER NOT NULL,
                        loaderId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        assignedAtMillis INTEGER NOT NULL,
                        startedAtMillis INTEGER,
                        PRIMARY KEY(orderId, loaderId)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_assignments_loaderId_status ON order_assignments (loaderId, status)"
                )
            }
        }
    }
}
