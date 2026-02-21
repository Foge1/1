package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalTopBarHeightPx = compositionLocalOf { 0 }

@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    GradientBackground(modifier = modifier) {
        SubcomposeLayout { constraints ->
            val topBarPlaceables = subcompose("topBar") {
                AppTopBar(title = title, actions = actions)
            }.map { it.measure(constraints.copy(minHeight = 0)) }
            val topBarHeightPx = topBarPlaceables.maxOfOrNull { it.height } ?: 0

            val contentPlaceables = subcompose("content") {
                CompositionLocalProvider(LocalTopBarHeightPx provides topBarHeightPx) {
                    Box(modifier = Modifier.fillMaxSize(), content = content)
                }
            }.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                contentPlaceables.forEach { it.placeRelative(0, 0) }
                topBarPlaceables.forEach { it.placeRelative(0, 0) }
            }
        }
    }
}

@Composable
private fun AppTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
