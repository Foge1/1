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

        SentryAndroid.init(this) { options ->
            options.dsn = configEntryPoint.appConfig().sentryDsn
            options.isEnabled = configEntryPoint.appConfig().sentryDsn.isNotBlank()
            options.environment = configEntryPoint.appConfig().envName
            options.isDebug = configEntryPoint.appConfig().verboseLogging
            options.release = buildReleaseName(configEntryPoint.appBuildInfo())
            options.tracesSampleRate = 0.0
        }
    }

    private fun buildReleaseName(buildInfo: AppBuildInfo): String {
        return "${buildInfo.applicationId}@${buildInfo.versionName}+${buildInfo.versionCode}"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppConfigEntryPoint {
    fun appConfig(): AppConfig
    fun appBuildInfo(): AppBuildInfo
}
