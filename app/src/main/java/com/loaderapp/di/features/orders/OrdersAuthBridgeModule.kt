package com.loaderapp.di.features.orders

import com.loaderapp.features.orders.domain.session.OrdersUserSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrdersAuthBridgeModule {

    @Binds
    @Singleton
    abstract fun bindOrdersUserSession(impl: AuthOrdersUserSession): OrdersUserSession
}
