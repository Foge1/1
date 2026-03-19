package com.loaderapp.core.common

interface AppBuildInfo {
    val isDebug: Boolean
    val applicationId: String
    val versionName: String
    val versionCode: Long
}
