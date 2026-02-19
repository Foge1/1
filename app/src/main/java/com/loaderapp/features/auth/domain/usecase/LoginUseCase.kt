package com.loaderapp.features.auth.domain.usecase

import com.loaderapp.domain.model.UserModel
import com.loaderapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

data class LoginParams(val phone: String, val pin: String)

/**
 * UseCase для входа пользователя.
 * TODO: Реализовать когда будет готова серверная авторизация.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: LoginParams): Result<UserModel> {
        return authRepository.login(params.phone, params.pin)
    }
}
