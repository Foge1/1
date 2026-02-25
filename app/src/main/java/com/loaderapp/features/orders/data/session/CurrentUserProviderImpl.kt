package com.loaderapp.features.orders.data.session

import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.api.AuthSessionApi
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val authSessionApi: AuthSessionApi
) : CurrentUserProvider {

    override fun observeCurrentUser(): Flow<CurrentUser?> {
        return authSessionApi.observeCurrentUser().map { user ->
            user?.let {
                CurrentUser(
                    id = it.id.toString(),
                    role = it.role.toFeatureRole()
                )
            }
        }
    }

    override suspend fun getCurrentUserOrNull(): CurrentUser? =
        authSessionApi.getCurrentUserOrNull()?.toCurrentUser()

    override suspend fun requireCurrentUserOnce(): CurrentUser {
        return getCurrentUserOrNull() ?: error("Current user is not selected")
    }

    companion object {
        internal fun createForTests(
            observeCurrentUser: Flow<User?>,
            getCurrentUserOrNull: suspend () -> User?
        ): CurrentUserProviderImpl = CurrentUserProviderImpl(
            authSessionApi = object : AuthSessionApi {
                override fun observeCurrentUser() = observeCurrentUser

                override suspend fun getCurrentUserOrNull() = getCurrentUserOrNull()
            }
        )
    }
}

private fun User.toCurrentUser(): CurrentUser = CurrentUser(
    id = id.toString(),
    role = role.toFeatureRole()
)

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
