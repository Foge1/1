package com.loaderapp.features.orders.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OrdersDatabaseMigrationJvmTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun migrate2To3_preservesOrderAndCreatesApplicationsAndAssignments() {
        val dbFile = tempFolder.newFile("orders-migration.db")

        createVersionedDb(dbFile, 2) { db ->
            db.execSQL(
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
                """.trimIndent(),
            )
            db.execSQL(
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
                """.trimIndent(),
            )
        }

        openRoomDb(dbFile).use { roomDb ->
            val sqliteDb = roomDb.openHelper.writableDatabase
            sqliteDb.query("SELECT status FROM orders WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("STAFFING", cursor.getString(0))
            }
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'order_applications'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'order_assignments'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun ordersDatabase_exportsSchemaAndCommittedSchemaExists() {
        val annotation = OrdersDatabase::class.java.getAnnotation(Database::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.exportSchema)
        assertEquals(3, annotation.version)

        val schemaFile =
            resolveSchemaPath(
                "schemas/com.loaderapp.features.orders.data.local.db.OrdersDatabase/3.json",
                "feature-orders/schemas/com.loaderapp.features.orders.data.local.db.OrdersDatabase/3.json",
            )
        assertTrue("Expected committed schema file for OrdersDatabase v3", schemaFile.exists())
    }

    private fun createVersionedDb(
        dbFile: File,
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit,
    ) {
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbFile.absolutePath)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun openRoomDb(dbFile: File): OrdersDatabase =
        Room
            .databaseBuilder(context, OrdersDatabase::class.java, dbFile.absolutePath)
            .addMigrations(*OrdersMigrations.ALL)
            .build()

    private fun resolveSchemaPath(vararg candidates: String): File =
        candidates
            .asSequence()
            .map(::File)
            .firstOrNull(File::exists)
            ?: File(candidates.first())

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
}
