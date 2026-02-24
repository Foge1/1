package com.loaderapp.core.common

import com.loaderapp.BuildConfig
import javax.inject.Inject

class AppBuildInfoImpl @Inject constructor() : AppBuildInfo {
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val applicationId: String = BuildConfig.APPLICATION_ID
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Long = BuildConfig.VERSION_CODE.toLong()
}
