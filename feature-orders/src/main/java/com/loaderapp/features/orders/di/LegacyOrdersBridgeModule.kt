package com.loaderapp.features.orders.di

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.features.orders.data.LegacyOrderRepositoryAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LegacyOrdersBridgeModule {
    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: LegacyOrderRepositoryAdapter): OrderRepository
}
