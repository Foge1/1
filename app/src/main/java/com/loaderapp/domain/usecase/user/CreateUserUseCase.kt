package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class CreateUserParams(val user: UserModel)

/**
 * UseCase: Create a new user.
 *
 * Validates name length. Phone is optional in this app's current flow.
 * Ensures avatarInitials are set before persisting.
 */
class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<CreateUserParams, Long>() {

    override suspend fun execute(params: CreateUserParams): Result<Long> {
        val user = params.user

        if (user.name.isBlank() || user.name.length < 2) {
            return Result.Error("Имя должно содержать минимум 2 символа")
        }

        val userWithInitials = if (user.avatarInitials.isBlank()) {
            user.copy(avatarInitials = generateInitials(user.name))
        } else {
            user
        }

        return userRepository.createUser(userWithInitials)
    }

    private fun generateInitials(name: String): String =
        name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
}
