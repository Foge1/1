package com.loaderapp.di

import com.loaderapp.features.orders.data.FakeOrdersRepository
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OrdersRepositoryBindingModule {

    @Provides
    @Singleton
    fun provideOrdersRepository(): OrdersRepository = FakeOrdersRepository()
}
