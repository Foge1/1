package com.loaderapp.features.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.features.auth.domain.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AuthRepositoryImplTest {

    @Test
    fun `restoreSession emits authenticated when user id exists in datastore`() = runTest {
        val dataStore = testDataStore("restore")
        dataStore.edit { it[CURRENT_USER_ID] = 7L }
        val userRepository = FakeUserRepository().apply {
            users[7L] = testUser(7L, "Alex", UserRoleModel.LOADER)
        }

        val repository = AuthRepositoryImpl(
            userRepository = userRepository,
            dataStore = dataStore,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.restoreSession()
        val state = repository.observeSession().first { it !is SessionState.Authenticating }

        assertTrue(state is SessionState.Authenticated)
        val user = (state as SessionState.Authenticated).user
        assertEquals(7L, user.id)
        assertEquals("Alex", user.name)
    }

    @Test
    fun `login saves session and publishes authenticated`() = runTest {
        val dataStore = testDataStore("login")
        val userRepository = FakeUserRepository()
        val repository = AuthRepositoryImpl(
            userRepository = userRepository,
            dataStore = dataStore,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val loginResult = repository.login("Den", UserRoleModel.DISPATCHER)
        val state = repository.observeSession().first { it is SessionState.Authenticated }

        assertTrue(loginResult is AppResult.Success)
        assertTrue(state is SessionState.Authenticated)
        assertTrue(dataStore.data.first()[CURRENT_USER_ID] != null)
    }

    @Test
    fun `logout clears session and publishes unauthenticated`() = runTest {
        val dataStore = testDataStore("logout")
        dataStore.edit { it[CURRENT_USER_ID] = 1L }
        val userRepository = FakeUserRepository().apply {
            users[1L] = testUser(1L, "User", UserRoleModel.LOADER)
        }
        val repository = AuthRepositoryImpl(
            userRepository = userRepository,
            dataStore = dataStore,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.logout()
        val state = repository.observeSession().first { it is SessionState.Unauthenticated }

        assertTrue(state is SessionState.Unauthenticated)
        assertTrue(dataStore.data.first()[CURRENT_USER_ID] == null)
    }

    private fun testDataStore(name: String): DataStore<Preferences> {
        val file = File("build/tmp/test-datastore-$name.preferences_pb")
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        return PreferenceDataStoreFactory.create(
            produceFile = { file }
        )
    }

    private fun testUser(id: Long, name: String, role: UserRoleModel) = UserModel(
        id = id,
        name = name,
        phone = "",
        role = role,
        rating = 5.0,
        birthDate = null,
        avatarInitials = "${name.first()}",
        createdAt = 0L
    )

    private class FakeUserRepository : UserRepository {
        val users = LinkedHashMap<Long, UserModel>()
        private var idCounter = 100L

        override fun getAllUsers(): Flow<List<UserModel>> = flowOf(users.values.toList())
        override fun getLoaders(): Flow<List<UserModel>> = flowOf(users.values.filter { it.role == UserRoleModel.LOADER })
        override fun getDispatchers(): Flow<List<UserModel>> = flowOf(users.values.filter { it.role == UserRoleModel.DISPATCHER })

        override suspend fun getUserById(userId: Long): Result<UserModel> =
            users[userId]?.let { Result.Success(it) } ?: Result.Error(AppError.NotFound)

        override fun getUserByIdFlow(userId: Long): Flow<UserModel?> = MutableStateFlow(users[userId])

        override suspend fun getUserByNameAndRole(name: String, role: UserRoleModel): Result<UserModel?> =
            Result.Success(users.values.firstOrNull { it.name == name && it.role == role })

        override suspend fun createUser(user: UserModel): Result<Long> {
            val id = idCounter++
            users[id] = user.copy(id = id)
            return Result.Success(id)
        }

        override suspend fun updateUser(user: UserModel): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteUser(user: UserModel): Result<Unit> = Result.Success(Unit)
        override suspend fun updateUserRating(userId: Long, rating: Double): Result<Unit> = Result.Success(Unit)
    }

    companion object {
        private val CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }
}
