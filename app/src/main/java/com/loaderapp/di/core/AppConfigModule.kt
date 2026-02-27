package com.loaderapp.di.core

import com.loaderapp.appinfo.AppConfigImpl
import com.loaderapp.core.common.AppConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppConfigModule {

    @Binds
    @Singleton
    abstract fun bindAppConfig(impl: AppConfigImpl): AppConfig
}
