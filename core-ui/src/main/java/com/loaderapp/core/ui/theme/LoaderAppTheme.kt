package com.loaderapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LoaderAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}
