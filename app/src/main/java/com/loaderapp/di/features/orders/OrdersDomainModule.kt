package com.loaderapp.di.features.orders

import com.loaderapp.features.orders.domain.OrdersLimits
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OrdersDomainModule {

    @Provides
    @Singleton
    fun provideOrdersLimits(): OrdersLimits = OrdersLimits(maxActiveApplications = 3)
}
