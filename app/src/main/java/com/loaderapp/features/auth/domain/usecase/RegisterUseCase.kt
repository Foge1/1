package com.loaderapp.features.auth.domain.usecase

import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

data class RegisterParams(
    val name: String,
    val phone: String,
    val pin: String,
    val role: UserRoleModel
)

/**
 * UseCase для регистрации нового пользователя.
 * TODO: Реализовать когда будет готова серверная авторизация.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterParams): Result<UserModel> {
        return authRepository.register(params.name, params.phone, params.pin, params.role)
    }
}
