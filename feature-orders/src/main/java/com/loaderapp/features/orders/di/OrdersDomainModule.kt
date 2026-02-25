package com.loaderapp.features.orders.di

import com.loaderapp.features.orders.domain.OrdersLimits
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OrdersDomainModule {

    @Provides
    @Singleton
    fun provideOrdersLimits(): OrdersLimits = OrdersLimits(maxActiveApplications = 3)
}
