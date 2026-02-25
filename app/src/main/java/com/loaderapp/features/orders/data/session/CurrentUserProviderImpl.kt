package com.loaderapp.features.orders.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.loaderapp.domain.model.UserModel
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class CurrentUserProviderImpl private constructor(
    private val currentUserIdFlow: Flow<Long?>,
    private val observeUserById: (Long) -> Flow<UserModel?>
) : CurrentUserProvider {

    @Inject
    constructor(
        dataStore: DataStore<Preferences>,
        userRepository: UserRepository
    ) : this(
        currentUserIdFlow = dataStore.data.map { it[CURRENT_USER_ID] },
        observeUserById = userRepository::getUserByIdFlow
    )

    override fun observeCurrentUser(): Flow<CurrentUser?> {
        return currentUserIdFlow
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    observeUserById(userId)
                        .map { user ->
                            user?.let {
                                CurrentUser(
                                    id = it.id.toString(),
                                    role = it.role.toFeatureRole()
                                )
                            }
                        }
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun getCurrentUserOrNull(): CurrentUser? = observeCurrentUser().first()

    override suspend fun requireCurrentUserOnce(): CurrentUser {
        return getCurrentUserOrNull() ?: error("Current user is not selected")
    }

    companion object {
        private val CURRENT_USER_ID = longPreferencesKey("current_user_id")

        internal fun createForTests(
            currentUserIdFlow: Flow<Long?>,
            observeUserById: (Long) -> Flow<UserModel?>
        ): CurrentUserProviderImpl = CurrentUserProviderImpl(currentUserIdFlow, observeUserById)
    }
}

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
