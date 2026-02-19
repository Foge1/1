package com.loaderapp.ui.rating

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.PlaceholderContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Экран рейтинга.
 * Заглушка — готова к наполнению функционалом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen() {
    Scaffold(
        topBar = { GradientTopBar(title = "Рейтинг") }
    ) { padding ->
        PlaceholderContent(
            icon     = Icons.Default.Star,
            title    = "Рейтинг",
            subtitle = "Здесь будет отображаться рейтинг грузчиков",
            modifier = Modifier.padding(padding)
        )
    }
}
