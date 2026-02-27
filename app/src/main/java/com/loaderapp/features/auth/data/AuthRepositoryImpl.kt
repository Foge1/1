package com.loaderapp.features.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.core.common.toAppResult
import com.loaderapp.core.dispatcher.IoDispatcher
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.features.auth.domain.api.AuthSessionApi
import com.loaderapp.features.auth.domain.model.SessionState
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.auth.domain.repository.AuthRepository
import com.loaderapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStore: DataStore<Preferences>,
    private val appLogger: AppLogger,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : AuthRepository, AuthSessionApi {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val sessionState = MutableStateFlow<SessionState>(SessionState.Authenticating)

    init {
        repositoryScope.launch {
            dataStore.data
                .map { preferences -> preferences[CURRENT_USER_ID] }
                .catch {
                    appLogger.breadcrumb("session", "restore_session_failed", mapOf("reason" to "datastore_read"))
                    appLogger.e(LOG_TAG, "Session restore failed due to DataStore read error", it)
                    sessionState.value = SessionState.Error(AppError.Storage.Serialization(it))
                    emit(null)
                }
                .collect { userId ->
                    sessionState.value = if (userId == null) {
                        SessionState.Unauthenticated
                    } else {
                        resolveAuthenticatedState(userId)
                    }
                }
        }
    }

    override suspend fun restoreSession(): AppResult<Unit> = withContext(repositoryScope.coroutineContext) {
        appLogger.breadcrumb("session", "restore_session_requested")
        when (val state = sessionState.value) {
            is SessionState.Error -> {
                appLogger.breadcrumb("session", "restore_session_failed", mapOf("reason" to "state_error"))
                AppResult.Failure(state.error)
            }

            else -> {
                appLogger.breadcrumb("session", "restore_session_succeeded")
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun login(name: String, role: UserRoleModel): AppResult<User> =
        withContext(repositoryScope.coroutineContext) {
            sessionState.value = SessionState.Authenticating

            val normalizedName = name.trim()
            appLogger.breadcrumb("auth", "login_requested", mapOf("role" to role.name))
            if (normalizedName.isBlank()) {
                val error = AppError.Validation(message = "Введите имя")
                sessionState.value = SessionState.Error(error)
                appLogger.breadcrumb("auth", "login_failed", mapOf("reason" to "validation"))
                return@withContext AppResult.Failure(error)
            }

            when (val resolvedUserResult = resolveOrCreateUser(normalizedName, role)) {
                is AppResult.Success -> {
                    persistUserId(resolvedUserResult.data.id)
                    appLogger.breadcrumb("auth", "login_succeeded", mapOf("role" to role.name))
                    AppResult.Success(resolvedUserResult.data)
                }

                is AppResult.Failure -> {
                    appLogger.breadcrumb("auth", "login_failed", mapOf("reason" to resolvedUserResult.error::class.simpleName.orEmpty()))
                    sessionState.value = SessionState.Error(resolvedUserResult.error)
                    resolvedUserResult
                }
            }
        }

    override suspend fun logout(): AppResult<Unit> = withContext(repositoryScope.coroutineContext) {
        appLogger.breadcrumb("auth", "logout_requested")
        dataStore.edit { it.remove(CURRENT_USER_ID) }
        sessionState.value = SessionState.Unauthenticated
        appLogger.breadcrumb("auth", "logout_succeeded")
        AppResult.Success(Unit)
    }

    override fun observeSession(): Flow<SessionState> = sessionState.asStateFlow()

    override fun observeCurrentUser(): Flow<User?> = sessionState.map { state ->
        when (state) {
            is SessionState.Authenticated -> state.user
            else -> null
        }
    }

    override suspend fun getCurrentUserOrNull(): User? = when (val state = sessionState.value) {
        is SessionState.Authenticated -> state.user
        else -> null
    }

    private suspend fun resolveAuthenticatedState(userId: Long): SessionState {
        return when (val result = userRepository.getUserById(userId).toAppResult()) {
            is AppResult.Success -> SessionState.Authenticated(result.data.toAuthUser())
            is AppResult.Failure -> {
                clearSessionSafely()
                SessionState.Error(result.error)
            }
        }
    }

    private suspend fun resolveOrCreateUser(name: String, role: UserRoleModel): AppResult<User> {
        return when (val existingUserResult = userRepository.getUserByNameAndRole(name, role).toAppResult()) {
            is AppResult.Success -> {
                val existingUser = existingUserResult.data
                if (existingUser != null) {
                    AppResult.Success(existingUser.toAuthUser())
                } else {
                    createUser(name, role)
                }
            }

            is AppResult.Failure -> existingUserResult
        }
    }

    private suspend fun createUser(name: String, role: UserRoleModel): AppResult<User> {
        val newUser = UserModel(
            id = 0,
            name = name,
            phone = "",
            role = role,
            rating = 5.0,
            birthDate = null,
            avatarInitials = "",
            createdAt = System.currentTimeMillis()
        )

        return when (val createResult = userRepository.createUser(newUser).toAppResult()) {
            is AppResult.Success -> {
                when (val getUserResult = userRepository.getUserById(createResult.data).toAppResult()) {
                    is AppResult.Success -> AppResult.Success(getUserResult.data.toAuthUser())
                    is AppResult.Failure -> getUserResult
                }
            }

            is AppResult.Failure -> createResult
        }
    }

    private suspend fun persistUserId(userId: Long) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
        }
    }

    private suspend fun clearSessionSafely() {
        dataStore.edit { preferences -> preferences.remove(CURRENT_USER_ID) }
    }

    private fun UserModel.toAuthUser(): User = User(
        id = id,
        name = name,
        role = role,
        phone = phone,
        rating = rating,
        birthDate = birthDate,
        avatarInitials = avatarInitials,
        createdAt = createdAt
    )

    companion object {
        private const val LOG_TAG = "AuthRepository"

        private val CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }
}
