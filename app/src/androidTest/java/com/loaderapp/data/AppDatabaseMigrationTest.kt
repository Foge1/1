package com.loaderapp.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_keepsExistingOrdersAndAddsUsersTable() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
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
            execSQL(
                """
                INSERT INTO `orders`
                (`id`,`address`,`dateTime`,`cargoDescription`,`pricePerHour`,`estimatedHours`,`status`,`createdAt`,`completedAt`,`workerId`)
                VALUES (1,'Street 1',1710000000,'Boxes',100.0,2,'AVAILABLE',1710000000,NULL,NULL)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(testDb, 2, true, AppMigrations.MIGRATION_1_2)
    }

    @Test
    fun migrate4To5_createsChatMessagesTable() {
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `orders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `address` TEXT NOT NULL,
                    `dateTime` INTEGER NOT NULL,
                    `cargoDescription` TEXT NOT NULL,
                    `pricePerHour` REAL NOT NULL,
                    `estimatedHours` INTEGER NOT NULL,
                    `requiredWorkers` INTEGER NOT NULL,
                    `minWorkerRating` REAL NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `workerId` INTEGER,
                    `dispatcherId` INTEGER NOT NULL,
                    `workerRating` REAL,
                    `comment` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `rating` REAL NOT NULL,
                    `birthDate` INTEGER,
                    `avatarInitials` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `order_workers` (
                    `orderId` INTEGER NOT NULL,
                    `workerId` INTEGER NOT NULL,
                    `takenAt` INTEGER NOT NULL,
                    PRIMARY KEY(`orderId`, `workerId`)
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(testDb, 5, true, AppMigrations.MIGRATION_4_5)
    }

    @Test
    fun migrateAllFrom1To5() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
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
            close()
        }

        helper.runMigrationsAndValidate(testDb, 5, true, *AppMigrations.ALL)
    }
}
