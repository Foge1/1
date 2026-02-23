package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class GetUserByNameAndRoleParams(
    val name: String,
    val role: UserRoleModel
)

class GetUserByNameAndRoleUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<GetUserByNameAndRoleParams, UserModel?>() {

    override suspend fun execute(params: GetUserByNameAndRoleParams): Result<UserModel?> {
        return userRepository.getUserByNameAndRole(
            name = params.name,
            role = params.role
        )
    }
}
