package com.loaderapp.features.orders.data.local.db

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
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrdersDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_preservesOrdersAndCreatesNewTables() {
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
            execSQL(
                """
                INSERT INTO orders (
                    id, title, address, pricePerHour, orderTimeType, orderTimeExactMillis,
                    durationMin, workersCurrent, workersTotal, tags, meta, comment,
                    status, createdByUserId, acceptedByUserId, acceptedAtMillis
                ) VALUES (
                    1, 'Order 1', 'Address', 500.0, 'exact', 1,
                    120, 0, 2, '[]', '{}', 'note',
                    'AVAILABLE', 'dispatcher', NULL, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, *OrdersMigrations.ALL)

        db.query("SELECT status FROM orders WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("STAFFING", cursor.getString(0))
        }

        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'order_applications'").use { cursor ->
            assertEquals(1, cursor.count)
        }

        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'order_assignments'").use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    private companion object {
        const val DB_NAME = "orders-migration-test"
    }
}
