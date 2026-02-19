package com.loaderapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.loaderapp.navigation.AppNavGraph
import com.loaderapp.navigation.Route
import com.loaderapp.presentation.settings.SettingsViewModel
import com.loaderapp.ui.theme.LoaderAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — единственная Activity.
 * Тёмная тема читается из SettingsViewModel (Hilt), никакого прямого
 * обращения к Application или DataStore.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // viewModels() — Hilt-aware делегат, создаёт VM в скоупе Activity
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат не требует обработки */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            LoaderAppTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        navController = rememberNavController(),
                        startDestination = Route.Splash.route,
                        onRequestNotificationPermission = ::requestNotificationPermission
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
