package com.loaderapp.core.ui.components.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    sheetState: SheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
        ),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = AppBottomSheetDefaults.sheetMaxWidth,
        shape = AppBottomSheetDefaults.shape,
        containerColor = AppBottomSheetDefaults.containerColor(),
        contentColor = AppBottomSheetDefaults.contentColor(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppBottomSheetDefaults.sheetPadding()),
            verticalArrangement = Arrangement.spacedBy(AppBottomSheetDefaults.contentSpacing),
            content = content,
        )
    }
}
