package com.loaderapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.loaderapp.navigation.AppNavGraph
import com.loaderapp.navigation.Route
import com.loaderapp.ui.theme.LoaderAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - главная активность приложения
 * Использует Navigation Compose для навигации
 * 
 * @AndroidEntryPoint - автоматическая инъекция Hilt зависимостей
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Главный экран с темой и навигацией
 */
@Composable
fun MainScreen(onRequestNotificationPermission: () -> Unit = {}) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    val isDarkTheme by app.userPreferences.isDarkTheme.collectAsState(initial = false)
    val navController = rememberNavController()

    LoaderAppTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavGraph(
                navController = navController,
                startDestination = Route.Splash.route,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}
