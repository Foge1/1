package com.loaderapp.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.SessionState
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthEvent {
    data class Login(val name: String, val role: UserRoleModel) : AuthEvent()
    data object Logout : AuthEvent()
    data object RestoreSession : AuthEvent()
    data object ConsumeError : AuthEvent()
}

data class AuthUiState(
    val sessionState: SessionState = SessionState.Authenticating,
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        onEvent(AuthEvent.RestoreSession)
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> login(event.name, event.role)
            AuthEvent.Logout -> logout()
            AuthEvent.RestoreSession -> restoreSession()
            AuthEvent.ConsumeError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.observeSession().collectLatest { sessionState ->
                _uiState.update {
                    it.copy(
                        sessionState = sessionState,
                        isLoading = sessionState is SessionState.Authenticating,
                        user = (sessionState as? SessionState.Authenticated)?.user,
                        error = (sessionState as? SessionState.Error)?.error.toHumanMessage()
                    )
                }
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            if (authRepository.restoreSession() is AppResult.Failure) {
                // observeSession() already exposes failure as SessionState.Error.
            }
        }
    }

    private fun login(name: String, role: UserRoleModel) {
        viewModelScope.launch {
            when (val result = authRepository.login(name, role)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _uiState.update { it.copy(error = result.error.toHumanMessage(), isLoading = false) }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _uiState.update { it.copy(error = result.error.toHumanMessage()) }
                }
            }
        }
    }
}

private fun AppError?.toHumanMessage(): String? = when (this) {
    null -> null
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
