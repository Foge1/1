package com.loaderapp.core.logging

import javax.inject.Inject

class NoOpAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) = Unit
}
