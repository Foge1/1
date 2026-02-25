package com.loaderapp.di.core

import com.loaderapp.appinfo.AppBuildInfoImpl
import com.loaderapp.core.common.AppBuildInfo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBuildInfoModule {

    @Binds
    @Singleton
    abstract fun bindAppBuildInfo(impl: AppBuildInfoImpl): AppBuildInfo
}
