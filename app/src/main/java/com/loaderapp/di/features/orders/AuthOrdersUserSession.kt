package com.loaderapp.di.features.orders

import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.api.AuthSessionApi
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.OrdersUser
import com.loaderapp.features.orders.domain.session.OrdersUserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AuthOrdersUserSession @Inject constructor(
    private val authSessionApi: AuthSessionApi
) : OrdersUserSession {

    override fun observeCurrentUser(): Flow<OrdersUser?> =
        authSessionApi.observeCurrentUser().map { user ->
            user?.let {
                OrdersUser(
                    id = it.id.toString(),
                    role = it.role.toFeatureRole()
                )
            }
        }

    override suspend fun getCurrentUserOrNull(): OrdersUser? =
        authSessionApi.getCurrentUserOrNull()?.let {
            OrdersUser(
                id = it.id.toString(),
                role = it.role.toFeatureRole()
            )
        }
}

private fun UserRoleModel.toFeatureRole(): Role = when (this) {
    UserRoleModel.DISPATCHER -> Role.DISPATCHER
    UserRoleModel.LOADER -> Role.LOADER
}
