package com.loaderapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application класс.
 * Все зависимости предоставляет Hilt — ручная инициализация не нужна.
 */
@HiltAndroidApp
class LoaderApplication : Application()
