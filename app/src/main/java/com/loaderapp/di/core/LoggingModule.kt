package com.loaderapp.di.core

import com.loaderapp.core.common.AppConfig
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.core.logging.LogcatAppLogger
import com.loaderapp.core.logging.NoOpAppLogger
import com.loaderapp.core.logging.SentryAppLogger
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
    fun provideAppLogger(appConfig: AppConfig): AppLogger {
        return when {
            appConfig.verboseLogging -> LogcatAppLogger()
            appConfig.sentryDsn.isNotBlank() -> SentryAppLogger()
            else -> NoOpAppLogger()
        }
    }
}
