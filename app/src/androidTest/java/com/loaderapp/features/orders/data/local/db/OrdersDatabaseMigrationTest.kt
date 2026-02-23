package com.loaderapp.features.orders.data.local.db

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrdersDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_keepsOrdersAndConvertsAvailableToStaffing() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL(
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
                    createdByUserId TEXT NOT NULL,
                    acceptedByUserId TEXT,
                    acceptedAtMillis INTEGER
                )
                """.trimIndent()
            )
            insert(
                "orders",
                0,
                ContentValues().apply {
                    put("id", 1L)
                    put("title", "old")
                    put("address", "addr")
                    put("pricePerHour", 120.0)
                    put("orderTimeType", "soon")
                    putNull("orderTimeExactMillis")
                    put("durationMin", 60)
                    put("workersCurrent", 0)
                    put("workersTotal", 2)
                    put("tags", "[]")
                    put("meta", "{}")
                    putNull("comment")
                    put("status", "AVAILABLE")
                    put("createdByUserId", "dispatcher-1")
                    putNull("acceptedByUserId")
                    putNull("acceptedAtMillis")
                }
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(DB_NAME, 3, true, AppDatabase.MIGRATION_2_3)
        migratedDb.query("SELECT status FROM orders WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("STAFFING", cursor.getString(0))
        }
    }

    private companion object {
        const val DB_NAME = "orders-migration-test"
    }
}
