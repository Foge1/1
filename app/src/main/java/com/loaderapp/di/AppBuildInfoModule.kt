package com.loaderapp.di

import com.loaderapp.core.common.AppBuildInfo
import com.loaderapp.core.common.AppBuildInfoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppBuildInfoModule {

    @Provides
    @Singleton
    fun provideAppBuildInfo(): AppBuildInfo = AppBuildInfoImpl()
}
