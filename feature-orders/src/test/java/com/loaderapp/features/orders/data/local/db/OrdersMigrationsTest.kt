package com.loaderapp.features.orders.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersMigrationsTest {

    @Test
    fun `orders migrations target schema version 3`() {
        assertTrue(OrdersMigrations.ALL.any { it.endVersion == 3 })
    }

    @Test
    fun `orders migrations are explicitly registered`() {
        assertFalse(OrdersMigrations.ALL.isEmpty())
        assertEquals(listOf(2 to 3), OrdersMigrations.ALL.map { it.startVersion to it.endVersion })
    }
}
