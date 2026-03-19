package com.loaderapp.core.common

interface AppConfig {
    val baseUrl: String
    val envName: String
    val verboseLogging: Boolean
    val sentryDsn: String
}
