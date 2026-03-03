package com.loaderapp.data.repository

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Unit тесты для UserRepositoryImpl.
 * Временный smoke-level набор: полноценные изолированные тесты потребуют in-memory Room и фикстур DAO.
 */
class UserRepositoryImplTest {
    @Test
    @Ignore("TECH-DEBT-007: Требуется in-memory Room для проверки createUser без зависимости от real DB")
    fun `createUser inserts and returns user`() {
        assertTrue(true)
    }

    @Test
    @Ignore("TECH-DEBT-007: Требуется in-memory Room для проверки getUserById в изоляции")
    fun `getUserById returns null for non-existing user`() {
        assertTrue(true)
    }
}
