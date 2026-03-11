package com.loaderapp.features.orders.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object OrdersDispatchersModule {
    @Provides
    @Named("historyDispatcher")
    fun provideHistoryDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
