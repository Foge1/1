package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------- навигационное намерение ----------
sealed class SessionDestination {
    object Loading    : SessionDestination()
    object Auth       : SessionDestination()
    object Main       : SessionDestination()   // единый Main для всех ролей

    val isResolved: Boolean get() = this !is Loading
}

// ---------- публичное состояние сессии ----------
data class SessionState(
    val user: UserModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SessionDestination>(SessionDestination.Loading)
    val destination: StateFlow<SessionDestination> = _destination.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /** Удобный shortcut — текущий пользователь */
    val currentUser: StateFlow<UserModel?> = sessionState
        .map { it.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        resolveSession()
    }

    // ── Восстановление сессии при старте ─────────────────────────────────────
    private fun resolveSession() {
        viewModelScope.launch {
            val userId = userPreferences.getCurrentUserId()
            if (userId == null) {
                _destination.value = SessionDestination.Auth
                return@launch
            }
            when (val result = userRepository.getUserById(userId)) {
                is Result.Success -> {
                    _sessionState.value = SessionState(user = result.data)
                    _destination.value  = SessionDestination.Main
                }
                else -> {
                    userPreferences.clearCurrentUser()
                    _destination.value = SessionDestination.Auth
                }
            }
        }
    }

    // ── Вход: создаём пользователя и сохраняем сессию ────────────────────────
    fun login(name: String, role: UserRoleModel) {
        viewModelScope.launch {
            _sessionState.value = SessionState(isLoading = true)

            val domainUser = UserModel(
                id             = 0,
                name           = name.trim(),
                phone          = "",
                role           = role,
                rating         = 5.0,
                birthDate      = null,
                avatarInitials = buildInitials(name),
                createdAt      = System.currentTimeMillis()
            )

            when (val result = userRepository.createUser(domainUser)) {
                is Result.Success -> {
                    val userId  = result.data
                    userPreferences.setCurrentUserId(userId)

                    // Перечитываем свежего пользователя из репозитория
                    when (val userResult = userRepository.getUserById(userId)) {
                        is Result.Success -> {
                            _sessionState.value = SessionState(user = userResult.data)
                            _destination.value  = SessionDestination.Main
                        }
                        else -> {
                            _sessionState.value = SessionState(error = "Ошибка загрузки профиля")
                        }
                    }
                }
                else -> {
                    _sessionState.value = SessionState(error = "Ошибка создания пользователя")
                }
            }
        }
    }

    // ── Выход / смена роли ───────────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            userPreferences.clearCurrentUser()
            _sessionState.value = SessionState()
            _destination.value  = SessionDestination.Auth
        }
    }

    // ── Тема ─────────────────────────────────────────────────────────────────
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun buildInitials(name: String): String =
        name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
}
