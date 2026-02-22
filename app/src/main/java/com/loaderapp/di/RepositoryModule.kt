package com.loaderapp.di

import com.loaderapp.data.repository.ChatRepositoryImpl
import com.loaderapp.data.repository.OrderRepositoryImpl
import com.loaderapp.data.repository.UserRepositoryImpl
import com.loaderapp.domain.repository.ChatRepository
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.features.orders.data.OrdersRepositoryImpl
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для связывания Repository интерфейсов с их реализациями
 * 
 * @Binds используется для связывания интерфейсов с конкретными реализациями
 * Это более эффективно чем @Provides для простых биндингов
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Связать OrderRepository -> OrderRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository
    
    /**
     * Связать UserRepository -> UserRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    /**
     * Связать ChatRepository -> ChatRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        ordersRepositoryImpl: OrdersRepositoryImpl
    ): OrdersRepository
}
