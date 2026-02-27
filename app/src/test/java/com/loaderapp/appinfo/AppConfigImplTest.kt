package com.loaderapp.appinfo

import com.loaderapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigImplTest {

    @Test
    fun `provides values from generated BuildConfig`() {
        val config = AppConfigImpl()

        assertEquals(BuildConfig.BASE_URL, config.baseUrl)
        assertEquals(BuildConfig.ENV_NAME, config.envName)
        assertEquals(BuildConfig.ENABLE_VERBOSE_LOGGING, config.verboseLogging)
        assertEquals(BuildConfig.SENTRY_DSN, config.sentryDsn)
    }
}
