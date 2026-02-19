package com.loaderapp.presentation.auth

import com.loaderapp.data.mapper.UserMapper
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.user.CreateUserParams
import com.loaderapp.domain.usecase.user.CreateUserUseCase
import com.loaderapp.core.common.Result
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * ViewModel для экрана выбора роли.
 * Создаёт пользователя через UseCase и сохраняет ID в DataStore.
 * Вся эта логика раньше была прямо в NavGraph.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val userPreferences: UserPreferences
) : BaseViewModel() {

    fun createUser(dataUser: User, onSuccess: (userId: Long, isDispatcher: Boolean) -> Unit) {
        viewModelScope.launch {
            // Маппим data.User → domain.UserModel
            val domainUser = UserMapper.toDomain(dataUser)
            val result = createUserUseCase(CreateUserParams(domainUser))
            when (result) {
                is Result.Success -> {
                    val userId = result.data
                    userPreferences.setCurrentUserId(userId)
                    val isDispatcher = dataUser.role == UserRole.DISPATCHER
                    onSuccess(userId, isDispatcher)
                }
                is Result.Error -> showSnackbar(result.message)
                else -> {}
            }
        }
    }
}
