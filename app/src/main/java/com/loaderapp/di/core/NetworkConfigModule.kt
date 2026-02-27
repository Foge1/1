package com.loaderapp.di.core

import com.loaderapp.core.common.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkConfigModule {

    @Provides
    @Named("base_url")
    fun provideBaseUrl(appConfig: AppConfig): String = appConfig.baseUrl
}
