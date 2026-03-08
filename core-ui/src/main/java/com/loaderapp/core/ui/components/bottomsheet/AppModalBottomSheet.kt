package com.loaderapp.core.ui.components.bottomsheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loaderapp.core.ui.theme.AppTypography

@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = AppBottomSheetDefaults.headerSpacing),
                style = AppTypography.titleLarge,
            )
        }

        content()
    }
}
