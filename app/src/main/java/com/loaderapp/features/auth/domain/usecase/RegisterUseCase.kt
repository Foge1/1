package com.loaderapp.features.auth.domain.usecase

import com.loaderapp.core.common.AppResult
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

data class RegisterParams(
    val name: String,
    val role: UserRoleModel
)

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(params: RegisterParams): AppResult<User> {
        return authRepository.login(params.name, params.role)
    }
}
