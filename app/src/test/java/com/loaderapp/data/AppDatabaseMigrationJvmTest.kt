package com.loaderapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationJvmTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun migrateAllFrom1To6_validatesRoomSchemaAndDropsLegacyOrdersTables() {
        val dbFile = tempFolder.newFile("app-migration-all.db")

        createVersionedDb(dbFile, 1) { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `orders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `address` TEXT NOT NULL,
                    `dateTime` INTEGER NOT NULL,
                    `cargoDescription` TEXT NOT NULL,
                    `pricePerHour` REAL NOT NULL,
                    `estimatedHours` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `workerId` INTEGER
                )
                """.trimIndent(),
            )
        }

        openRoomDb(dbFile).use { roomDb ->
            val sqliteDb = roomDb.openHelper.writableDatabase

            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'orders'").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'order_workers'").use { cursor ->
                assertFalse(cursor.moveToFirst())
            }
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'users'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'chat_messages'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun appDatabase_exportsSchemaAndCommittedSchemaExists() {
        val annotation = AppDatabase::class.java.getAnnotation(Database::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.exportSchema)
        assertEquals(6, annotation.version)

        val schemaFile =
            resolveSchemaPath(
                "schemas/com.loaderapp.data.AppDatabase/6.json",
                "app/schemas/com.loaderapp.data.AppDatabase/6.json",
            )
        assertTrue(
            "Expected committed schema file for version 6",
            schemaFile.exists(),
        )
    }

    private fun createVersionedDb(
        dbFile: File,
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit,
    ) {
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
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
                )
                .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun openRoomDb(dbFile: File): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(*AppMigrations.ALL)
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
