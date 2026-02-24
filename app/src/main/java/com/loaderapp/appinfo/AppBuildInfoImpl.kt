package com.loaderapp.appinfo

import com.loaderapp.BuildConfig
import com.loaderapp.core.common.AppBuildInfo

class AppBuildInfoImpl : AppBuildInfo {
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val applicationId: String = BuildConfig.APPLICATION_ID
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Long = BuildConfig.VERSION_CODE.toLong()
}
