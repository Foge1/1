package com.loaderapp.core.logging

import android.util.Log
import javax.inject.Inject

class LogcatAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}
