package com.loaderapp.features.orders.data.session

import com.loaderapp.core.common.Result
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) : CurrentUserProvider {

    override suspend fun getCurrentUser(): CurrentUser {
        val userId = userPreferences.getCurrentUserId()
            ?: error("Current user is not selected")
        val user = when (val result = userRepository.getUserById(userId)) {
            is Result.Success -> result.data
            is Result.Error -> error(result.message)
        }

        return CurrentUser(
            id = user.id.toString(),
            role = user.role.toFeatureRole()
        )
    }
}

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
