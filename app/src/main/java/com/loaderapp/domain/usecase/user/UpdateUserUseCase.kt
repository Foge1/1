package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class UpdateUserParams(val user: UserModel)

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<UpdateUserParams, Unit>() {
    override suspend fun execute(params: UpdateUserParams): Result<Unit> =
        userRepository.updateUser(params.user)
}
