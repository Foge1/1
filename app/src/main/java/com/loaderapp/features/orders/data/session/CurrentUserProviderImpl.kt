package com.loaderapp.features.orders.data.session

import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) : CurrentUserProvider {

    override fun observeCurrentUser(): Flow<CurrentUser> {
        return userPreferences.currentUserId
            .distinctUntilChanged()
            .map { userId ->
                userId ?: error("Current user is not selected")
            }
            .flatMapLatest { userId ->
                userRepository.getUserByIdFlow(userId)
                    .map { user ->
                        val resolvedUser = user ?: error("Current user not found: $userId")
                        CurrentUser(
                            id = resolvedUser.id.toString(),
                            role = resolvedUser.role.toFeatureRole()
                        )
                    }
            }
            .distinctUntilChanged()
    }

    override suspend fun getCurrentUser(): CurrentUser = observeCurrentUser().first()
}

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
