package com.loaderapp.features.orders.data.session

import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.session.OrdersUser
import com.loaderapp.features.orders.domain.session.OrdersUserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class CurrentUserProviderImpl @Inject constructor(
    private val ordersUserSession: OrdersUserSession
) : CurrentUserProvider {

    override fun observeCurrentUser(): Flow<CurrentUser?> {
        return ordersUserSession.observeCurrentUser().map { user -> user?.toCurrentUser() }
    }

    override suspend fun getCurrentUserOrNull(): CurrentUser? =
        ordersUserSession.getCurrentUserOrNull()?.toCurrentUser()

    override suspend fun requireCurrentUserOnce(): CurrentUser {
        return getCurrentUserOrNull() ?: error("Current user is not selected")
    }

    companion object {
        internal fun createForTests(
            observeCurrentUser: Flow<OrdersUser?>,
            getCurrentUserOrNull: suspend () -> OrdersUser?
        ): CurrentUserProviderImpl = CurrentUserProviderImpl(
            ordersUserSession = object : OrdersUserSession {
                override fun observeCurrentUser() = observeCurrentUser

                override suspend fun getCurrentUserOrNull() = getCurrentUserOrNull()
            }
        )
    }
}

private fun OrdersUser.toCurrentUser(): CurrentUser = CurrentUser(
    id = id,
    role = role
)
