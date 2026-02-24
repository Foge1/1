package com.loaderapp.features.orders.data.session

import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
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
    fun `observeCurrentUser emits null when current user id is not selected`() = runTest {
        val currentUserIdFlow = MutableStateFlow<Long?>(null)
        val usersById = mutableMapOf<Long, MutableStateFlow<UserModel?>>()
        val provider = CurrentUserProviderImpl.createForTests(currentUserIdFlow) { userId ->
            usersById.getOrPut(userId) { MutableStateFlow(null) }
        }

        val emission = provider.observeCurrentUser().take(1).toList().single()

        assertNull(emission)
    }

    @Test
    fun `observeCurrentUser emits selected user when it appears`() = runTest {
        val currentUserIdFlow = MutableStateFlow<Long?>(1L)
        val userFlow = MutableStateFlow<UserModel?>(null)
        val provider = CurrentUserProviderImpl.createForTests(currentUserIdFlow) { userFlow }

        val emissionsDeferred = async { provider.observeCurrentUser().take(2).toList() }
        userFlow.value = user(id = 1L, role = UserRoleModel.LOADER)

        val emissions = emissionsDeferred.await()
        assertNull(emissions[0])
        assertEquals("1", emissions[1]?.id)
        assertEquals(Role.LOADER, emissions[1]?.role)
    }

    @Test
    fun `observeCurrentUser emits consistent sequence when current user changes`() = runTest {
        val currentUserIdFlow = MutableStateFlow<Long?>(1L)
        val usersById = mutableMapOf(
            1L to MutableStateFlow<UserModel?>(user(1L, UserRoleModel.LOADER)),
            2L to MutableStateFlow<UserModel?>(user(2L, UserRoleModel.DISPATCHER))
        )
        val provider = CurrentUserProviderImpl.createForTests(currentUserIdFlow) { userId ->
            usersById.getValue(userId)
        }

        val emissionsDeferred = async { provider.observeCurrentUser().take(3).toList() }
        currentUserIdFlow.value = null
        currentUserIdFlow.value = 2L

        val emissions = emissionsDeferred.await()
        assertEquals("1", emissions[0]?.id)
        assertNull(emissions[1])
        assertEquals("2", emissions[2]?.id)
        assertEquals(Role.DISPATCHER, emissions[2]?.role)
    }

    private fun user(id: Long, role: UserRoleModel) = UserModel(
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
