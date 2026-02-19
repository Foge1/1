package com.loaderapp.ui.rating

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран рейтинга.
 * Заглушка — готова к наполнению функционалом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Рейтинг") }) }
    ) { padding ->
        PlaceholderContent(
            icon     = Icons.Default.Star,
            title    = "Рейтинг",
            subtitle = "Здесь будет отображаться рейтинг грузчиков",
            modifier = Modifier.padding(padding)
        )
    }
}
