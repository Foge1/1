package com.loaderapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT NOT NULL,
                    `role` TEXT NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                "ALTER TABLE `orders` ADD COLUMN `dispatcherId` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `orders` ADD COLUMN `requiredWorkers` INTEGER NOT NULL DEFAULT 1"
            )
            database.execSQL(
                "ALTER TABLE `orders` ADD COLUMN `minWorkerRating` REAL NOT NULL DEFAULT 0"
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `order_workers` (
                    `orderId` INTEGER NOT NULL,
                    `workerId` INTEGER NOT NULL,
                    `takenAt` INTEGER NOT NULL,
                    PRIMARY KEY(`orderId`, `workerId`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `orders` ADD COLUMN `workerRating` REAL"
            )
            database.execSQL(
                "ALTER TABLE `orders` ADD COLUMN `comment` TEXT NOT NULL DEFAULT ''"
            )

            database.execSQL(
                "ALTER TABLE `users` ADD COLUMN `rating` REAL NOT NULL DEFAULT 5.0"
            )
            database.execSQL(
                "ALTER TABLE `users` ADD COLUMN `birthDate` INTEGER"
            )
            database.execSQL(
                "ALTER TABLE `users` ADD COLUMN `avatarInitials` TEXT NOT NULL DEFAULT ''"
            )
            database.execSQL(
                "ALTER TABLE `users` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `chat_messages` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `senderId` INTEGER NOT NULL,
                    `senderName` TEXT NOT NULL,
                    `senderRole` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `sentAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5
    )
}
