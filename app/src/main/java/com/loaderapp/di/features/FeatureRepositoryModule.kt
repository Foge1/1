package com.loaderapp.di.features

import com.loaderapp.features.auth.data.AuthRepositoryImpl
import com.loaderapp.features.auth.domain.api.AuthSessionApi
import com.loaderapp.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthSessionApi(impl: AuthRepositoryImpl): AuthSessionApi

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
