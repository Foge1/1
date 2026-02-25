package com.loaderapp.features.orders.data.session

import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.orders.domain.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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

        val emission = provider.observeCurrentUser().take(1).toList().single()

        assertNull(emission)
    }

    @Test
    fun `observeCurrentUser maps auth user to orders current user`() = runTest {
        val authUserFlow = MutableStateFlow<User?>(null)
        val provider = CurrentUserProviderImpl.createForTests(
            observeCurrentUser = authUserFlow,
            getCurrentUserOrNull = { authUserFlow.value }
        )

        val emissionsDeferred = async { provider.observeCurrentUser().take(2).toList() }
        authUserFlow.value = user(id = 1L, role = UserRoleModel.LOADER)

        val emissions = emissionsDeferred.await()
        assertNull(emissions[0])
        assertEquals("1", emissions[1]?.id)
        assertEquals(Role.LOADER, emissions[1]?.role)
    }

    @Test
    fun `observeCurrentUser emits consistent sequence when session user changes`() = runTest {
        val authUserFlow = MutableStateFlow<User?>(user(1L, UserRoleModel.LOADER))
        val provider = CurrentUserProviderImpl.createForTests(
            observeCurrentUser = authUserFlow,
            getCurrentUserOrNull = { authUserFlow.value }
        )

        val emissionsDeferred = async { provider.observeCurrentUser().take(3).toList() }
        authUserFlow.value = null
        authUserFlow.value = user(2L, UserRoleModel.DISPATCHER)

        val emissions = emissionsDeferred.await()
        assertEquals("1", emissions[0]?.id)
        assertNull(emissions[1])
        assertEquals("2", emissions[2]?.id)
        assertEquals(Role.DISPATCHER, emissions[2]?.role)
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
