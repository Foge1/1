package com.loaderapp.di.logging

import com.loaderapp.BuildConfig
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.core.logging.LogcatAppLogger
import com.loaderapp.core.logging.NoOpAppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger {
        return if (BuildConfig.DEBUG) LogcatAppLogger() else NoOpAppLogger()
    }
}
