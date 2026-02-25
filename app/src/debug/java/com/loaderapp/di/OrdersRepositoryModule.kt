package com.loaderapp.di

import com.loaderapp.features.orders.data.FakeOrdersRepository
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrdersRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        impl: FakeOrdersRepository
    ): OrdersRepository
}
