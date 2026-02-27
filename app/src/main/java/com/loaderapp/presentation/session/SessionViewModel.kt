package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.SessionState as AuthSessionState
import com.loaderapp.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionDestination {
    object Loading : SessionDestination()
    object Auth : SessionDestination()
    object Main : SessionDestination()

    val isResolved: Boolean get() = this !is Loading
}

data class SessionState(
    val user: UserModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _destination = MutableStateFlow<SessionDestination>(SessionDestination.Loading)
    val destination: StateFlow<SessionDestination> = _destination.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState(isLoading = true))
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    val currentUser: StateFlow<UserModel?> = sessionState
        .map { it.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        observeSession()
        resolveSession()
    }

    private fun resolveSession() {
        viewModelScope.launch {
            if (authRepository.restoreSession() is AppResult.Failure) {
                appLogger.breadcrumb("session", "restore_session_failed", mapOf("source" to "session_vm"))
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.observeSession().collectLatest { state ->
                when (state) {
                    AuthSessionState.Authenticating -> {
                        _sessionState.value = SessionState(isLoading = true)
                        _destination.value = SessionDestination.Loading
                    }

                    AuthSessionState.Unauthenticated -> {
                        _sessionState.value = SessionState()
                        _destination.value = SessionDestination.Auth
                    }

                    is AuthSessionState.Authenticated -> {
                        val user = UserModel(
                            id = state.user.id,
                            name = state.user.name,
                            phone = state.user.phone,
                            role = state.user.role,
                            rating = state.user.rating,
                            birthDate = state.user.birthDate,
                            avatarInitials = state.user.avatarInitials,
                            createdAt = state.user.createdAt
                        )
                        _sessionState.value = SessionState(user = user)
                        _destination.value = SessionDestination.Main
                    }

                    is AuthSessionState.Error -> {
                        _sessionState.value = SessionState(error = state.error.toHumanMessage())
                        _destination.value = SessionDestination.Auth
                    }
                }
            }
        }
    }

    fun login(name: String, role: UserRoleModel) {
        viewModelScope.launch {
            _sessionState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(name, role)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    appLogger.breadcrumb("auth", "login_failed", mapOf("source" to "session_vm"))
                    result.error.logError(appLogger, "login")
                    _sessionState.update { it.copy(isLoading = false, error = result.error.toHumanMessage()) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    appLogger.breadcrumb("auth", "logout_failed", mapOf("source" to "session_vm"))
                    result.error.logError(appLogger, "logout")
                    _sessionState.update { it.copy(error = result.error.toHumanMessage()) }
                }
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }
}

private fun AppError.toHumanMessage(): String = when (this) {
    is AppError.Validation -> message ?: "Ошибка валидации"
    AppError.Auth.Unauthorized -> "Требуется авторизация"
    AppError.Auth.Forbidden -> "Доступ запрещён"
    AppError.Auth.SessionExpired -> "Сессия истекла"
    AppError.Network.NoInternet -> "Нет интернета"
    AppError.Network.Timeout -> "Превышено время ожидания"
    AppError.Network.Dns,
    AppError.Network.UnknownHost -> "Ошибка сети"
    is AppError.Storage.Db -> "Ошибка базы данных"
    is AppError.Storage.Serialization -> "Ошибка чтения сессии"
    AppError.NotFound -> "Пользователь не найден"
    is AppError.Backend -> serverMessage ?: "Ошибка сервера"
    is AppError.Unknown -> cause?.message ?: "Неизвестная ошибка"
}


private fun AppError.logError(appLogger: AppLogger, action: String) {
    when (this) {
        AppError.Network.NoInternet,
        AppError.Network.Timeout,
        AppError.Network.Dns,
        AppError.Network.UnknownHost -> {
            appLogger.breadcrumb("network", "network_error", mapOf("action" to action))
            appLogger.w("SessionViewModel", "Network error during $action")
        }

        is AppError.Storage.Db,
        is AppError.Storage.Serialization -> {
            appLogger.breadcrumb("storage", "database_error", mapOf("action" to action))
            appLogger.w("SessionViewModel", "Storage error during $action")
        }

        else -> Unit
    }
}
