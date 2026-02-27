package com.loaderapp.appinfo

import com.loaderapp.BuildConfig
import com.loaderapp.core.common.AppConfig
import javax.inject.Inject

class AppConfigImpl @Inject constructor() : AppConfig {
    override val baseUrl: String = BuildConfig.BASE_URL
    override val envName: String = BuildConfig.ENV_NAME
    override val verboseLogging: Boolean = BuildConfig.ENABLE_VERBOSE_LOGGING
    override val sentryDsn: String = BuildConfig.SENTRY_DSN
}
