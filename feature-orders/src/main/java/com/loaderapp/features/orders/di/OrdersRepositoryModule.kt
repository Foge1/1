package com.loaderapp.features.orders.di

import com.loaderapp.features.orders.data.OrdersRepositoryImpl
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OrdersRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(impl: OrdersRepositoryImpl): OrdersRepository
}
