package com.loaderapp

import android.app.Application
import io.sentry.android.core.SentryAndroid
import dagger.hilt.android.HiltAndroidApp

/**
 * Application класс.
 */
@HiltAndroidApp
class LoaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.isEnabled = BuildConfig.SENTRY_ENABLED
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            options.isDebug = BuildConfig.DEBUG
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.tracesSampleRate = 0.0
        }
    }
}
