package com.loaderapp.features.orders.data.local.db

import androidx.room.Database
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OrdersMigrationsTest {

    @Test
    fun `orders database version is 3`() {
        val annotation = OrdersDatabase::class.java.getAnnotation(Database::class.java)
        assertEquals(3, annotation.version)
    }

    @Test
    fun `orders migrations are explicitly registered`() {
        assertFalse(OrdersMigrations.ALL.isEmpty())
        assertEquals(listOf(2 to 3), OrdersMigrations.ALL.map { it.startVersion to it.endVersion })
    }
}
