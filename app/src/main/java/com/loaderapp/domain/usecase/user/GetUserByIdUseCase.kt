package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для получения пользователя
 */
data class GetUserByIdParams(val userId: Long)

/**
 * UseCase: Получить пользователя по ID
 */
class GetUserByIdUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<GetUserByIdParams, UserModel>() {
    
    override suspend fun execute(params: GetUserByIdParams): Result<UserModel> {
        return userRepository.getUserById(params.userId)
    }
}
