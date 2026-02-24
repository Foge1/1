package com.loaderapp.appinfo

import com.loaderapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppBuildInfoImplTest {

    @Test
    fun `provides values from generated BuildConfig`() {
        val info = AppBuildInfoImpl()

        assertEquals(BuildConfig.DEBUG, info.isDebug)
        assertEquals(BuildConfig.APPLICATION_ID, info.applicationId)
        assertEquals(BuildConfig.VERSION_NAME, info.versionName)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), info.versionCode)
        assertNotNull(info.applicationId)
    }
}
