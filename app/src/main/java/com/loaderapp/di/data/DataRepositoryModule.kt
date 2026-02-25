package com.loaderapp.di.data

import com.loaderapp.data.repository.ChatRepositoryImpl
import com.loaderapp.data.repository.OrderRepositoryImpl
import com.loaderapp.data.repository.UserRepositoryImpl
import com.loaderapp.domain.repository.ChatRepository
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
