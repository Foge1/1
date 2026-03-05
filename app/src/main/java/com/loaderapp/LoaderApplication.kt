package com.loaderapp

import android.app.Application
import com.loaderapp.core.common.AppBuildInfo
import com.loaderapp.core.common.AppConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import io.sentry.android.core.SentryAndroid

/**
 * Application класс.
 */
@HiltAndroidApp
class LoaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val configEntryPoint =
            EntryPointAccessors.fromApplication(this, AppConfigEntryPoint::class.java)
        val appConfig = configEntryPoint.appConfig()

        SentryAndroid.init(this) { options ->
            if (BuildConfig.DEBUG) {
                options.isEnabled = false
                return@init
            }

            val dsn = appConfig.sentryDsn
            if (dsn.isBlank()) {
                options.isEnabled = false
                return@init
            }

            options.dsn = dsn
            options.environment = appConfig.envName
            options.isDebug = appConfig.verboseLogging
            options.release = buildReleaseName(configEntryPoint.appBuildInfo())
            options.tracesSampleRate = 0.0
        }
    }

    private fun buildReleaseName(buildInfo: AppBuildInfo): String =
        "${buildInfo.applicationId}@${buildInfo.versionName}+${buildInfo.versionCode}"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppConfigEntryPoint {
    fun appConfig(): AppConfig

    fun appBuildInfo(): AppBuildInfo
}
