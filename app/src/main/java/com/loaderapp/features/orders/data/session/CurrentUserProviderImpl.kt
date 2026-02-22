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
import kotlinx.coroutines.flow.filterNotNull
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
            .filterNotNull()
            .flatMapLatest { userId ->
                userRepository.getUserByIdFlow(userId)
                    .map { user ->
                        user?.let {
                            CurrentUser(
                                id = it.id.toString(),
                                role = it.role.toFeatureRole()
                            )
                        }
                    }
            }
            .filterNotNull()
            .distinctUntilChanged()
    }

    override suspend fun getCurrentUser(): CurrentUser = observeCurrentUser().first()
}

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
