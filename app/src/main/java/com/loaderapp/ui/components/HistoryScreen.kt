package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.presentation.DispatcherHistoryUiState
import com.loaderapp.features.orders.presentation.mapper.toLegacyOrderModel

@Composable
fun HistoryScreen(
    state: DispatcherHistoryUiState,
    onQueryChange: (String) -> Unit,
    onOrderClick: (Long) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("Поиск по истории") }
        )

        if (state.sections.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "История пуста",
                message = "Нет заказов, подходящих под фильтр"
            )
            return
        }

        FadingEdgeLazyColumn(
            modifier = Modifier.fillMaxSize(),
            topFadeHeight = 0.dp,
            bottomFadeHeight = 36.dp,
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomPadding)
        ) {
            itemsIndexed(state.sections, key = { _, section -> section.key }) { index, section ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(section.count.toString(), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                section.items.forEach { item ->
                    OrderCard(
                        order = item.order.toLegacyOrderModel(),
                        onClick = { onOrderClick(item.order.order.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (index < state.sections.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
