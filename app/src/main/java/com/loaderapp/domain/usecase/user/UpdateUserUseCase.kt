package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * UseCase: Обновить данные пользователя.
 * Используется в ProfileViewModel при сохранении профиля.
 */
class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<UserModel, Unit>() {

    override suspend fun execute(params: UserModel): Result<Unit> =
        userRepository.updateUser(params)
}
