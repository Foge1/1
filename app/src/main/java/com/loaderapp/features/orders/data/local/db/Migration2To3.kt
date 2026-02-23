package com.loaderapp.features.orders.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 2 → 3:
 *
 * 1. Recreate `orders` table without `acceptedByUserId` / `acceptedAtMillis` columns.
 * 2. Rename legacy status "AVAILABLE" → "STAFFING" in the new table.
 * 3. Create `order_applications` table.
 * 4. Create `order_assignments` table.
 *
 * Data safety: all existing orders are copied; none are deleted.
 */
object Migration2To3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Rename the old table
        db.execSQL("ALTER TABLE orders RENAME TO orders_old")

        // 2. Create new orders table (no acceptedByUserId / acceptedAtMillis)
        db.execSQL(
            """
            CREATE TABLE orders (
                id              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title           TEXT    NOT NULL,
                address         TEXT    NOT NULL,
                pricePerHour    REAL    NOT NULL,
                orderTimeType   TEXT    NOT NULL,
                orderTimeExactMillis INTEGER,
                durationMin     INTEGER NOT NULL,
                workersCurrent  INTEGER NOT NULL,
                workersTotal    INTEGER NOT NULL,
                tags            TEXT    NOT NULL,
                meta            TEXT    NOT NULL,
                comment         TEXT,
                status          TEXT    NOT NULL,
                createdByUserId TEXT    NOT NULL
            )
            """.trimIndent()
        )

        // 3. Copy data, remapping AVAILABLE → STAFFING and TAKEN → IN_PROGRESS
        db.execSQL(
            """
            INSERT INTO orders (
                id, title, address, pricePerHour, orderTimeType, orderTimeExactMillis,
                durationMin, workersCurrent, workersTotal, tags, meta, comment,
                status, createdByUserId
            )
            SELECT
                id, title, address, pricePerHour, orderTimeType, orderTimeExactMillis,
                durationMin, workersCurrent, workersTotal, tags, meta, comment,
                CASE status
                    WHEN 'AVAILABLE' THEN 'STAFFING'
                    WHEN 'TAKEN'     THEN 'IN_PROGRESS'
                    ELSE status
                END,
                createdByUserId
            FROM orders_old
            """.trimIndent()
        )

        // 4. Drop old table
        db.execSQL("DROP TABLE orders_old")

        // 5. Create order_applications table
        db.execSQL(
            """
            CREATE TABLE order_applications (
                orderId         INTEGER NOT NULL,
                loaderId        TEXT    NOT NULL,
                status          TEXT    NOT NULL,
                appliedAtMillis INTEGER NOT NULL,
                ratingSnapshot  REAL,
                PRIMARY KEY (orderId, loaderId)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_order_applications_loader_status ON order_applications (loaderId, status)"
        )

        // 6. Create order_assignments table
        db.execSQL(
            """
            CREATE TABLE order_assignments (
                orderId          INTEGER NOT NULL,
                loaderId         TEXT    NOT NULL,
                status           TEXT    NOT NULL,
                assignedAtMillis INTEGER NOT NULL,
                startedAtMillis  INTEGER,
                PRIMARY KEY (orderId, loaderId)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_order_assignments_loader_status ON order_assignments (loaderId, status)"
        )
    }
}
