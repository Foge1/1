package com.loaderapp.features.orders.di

import com.loaderapp.features.orders.data.session.CurrentUserProviderImpl
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OrdersSessionModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(impl: CurrentUserProviderImpl): CurrentUserProvider
}
