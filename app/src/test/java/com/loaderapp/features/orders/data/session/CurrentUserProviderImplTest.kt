package com.loaderapp.features.orders.data.session

import app.cash.turbine.test
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.orders.domain.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentUserProviderImplTest {

    @Test
    fun `observeCurrentUser emits null when session has no user`() = runTest {
        val authUserFlow = MutableStateFlow<User?>(null)
        val provider = CurrentUserProviderImpl.createForTests(
            observeCurrentUser = authUserFlow,
            getCurrentUserOrNull = { authUserFlow.value }
        )

        provider.observeCurrentUser().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCurrentUser maps auth user to orders current user`() = runTest {
        val authUserFlow = MutableStateFlow<User?>(null)
        val provider = CurrentUserProviderImpl.createForTests(
            observeCurrentUser = authUserFlow,
            getCurrentUserOrNull = { authUserFlow.value }
        )

        provider.observeCurrentUser().test {
            assertNull(awaitItem())
            authUserFlow.value = user(id = 1L, role = UserRoleModel.LOADER)
            val mapped = awaitItem()
            assertEquals("1", mapped?.id)
            assertEquals(Role.LOADER, mapped?.role)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCurrentUser emits consistent sequence when session user changes`() = runTest {
        val authUserFlow = MutableStateFlow<User?>(user(1L, UserRoleModel.LOADER))
        val provider = CurrentUserProviderImpl.createForTests(
            observeCurrentUser = authUserFlow,
            getCurrentUserOrNull = { authUserFlow.value }
        )

        provider.observeCurrentUser().test {
            assertEquals("1", awaitItem()?.id)
            authUserFlow.value = null
            assertNull(awaitItem())
            authUserFlow.value = user(2L, UserRoleModel.DISPATCHER)
            val mapped = awaitItem()
            assertEquals("2", mapped?.id)
            assertEquals(Role.DISPATCHER, mapped?.role)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun user(id: Long, role: UserRoleModel) = User(
        id = id,
        name = "User $id",
        phone = "",
        role = role,
        rating = 5.0,
        birthDate = null,
        avatarInitials = "U$id",
        createdAt = 0L
    )
}
