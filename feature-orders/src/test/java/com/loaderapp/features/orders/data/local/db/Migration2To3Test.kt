package com.loaderapp.features.orders.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit verification of migration SQL constants. The actual migration correctness
 * (SQLite execution) is validated in instrumented tests with MigrationTestHelper.
 * Here we verify the version numbers and table names are correct.
 */
class Migration2To3Test {

    @Test
    fun `orders migrations list contains 2 to 3`() {
        val pair = OrdersMigrations.ALL.map { it.startVersion to it.endVersion }
        assertEquals(listOf(2 to 3), pair)
    }


    @Test
    fun `migration start version is 2`() {
        assertEquals(2, Migration2To3.startVersion)
    }

    @Test
    fun `migration end version is 3`() {
        assertEquals(3, Migration2To3.endVersion)
    }
}
