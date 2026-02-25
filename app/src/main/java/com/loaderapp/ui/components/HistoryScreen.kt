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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.ui.HistoryOrderUiModel
import com.loaderapp.features.orders.data.mappers.toLegacyOrderModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HistorySection(
    val key: String,
    val title: String,
    val items: List<HistoryOrderUiModel>
)

@Composable
fun HistoryScreen(
    items: List<HistoryOrderUiModel>,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onOrderClick: (Long) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val sections by remember(items, query.text) {
        derivedStateOf {
            val normalizedQuery = query.text.trim().lowercase()
            val filtered = if (normalizedQuery.isBlank()) {
                items
            } else {
                items.filter { historyItem ->
                    historyItem.address.contains(normalizedQuery, ignoreCase = true) ||
                        historyItem.searchableDetails.contains(normalizedQuery, ignoreCase = true)
                }
            }

            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val yesterday = today.minusDays(1)
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

            filtered
                .groupBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDate() }
                .toList()
                .sortedByDescending { (date, _) -> date }
                .map { (date, sectionItems) ->
                    val title = when (date) {
                        today -> "Сегодня"
                        yesterday -> "Вчера"
                        else -> date.format(formatter)
                    }
                    HistorySection(
                        key = date.toString(),
                        title = title,
                        items = sectionItems
                    )
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("Поиск по истории") }
        )

        if (sections.isEmpty()) {
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
            itemsIndexed(sections, key = { _, section -> section.key }) { index, section ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(section.items.size.toString(), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                section.items.forEach { item ->
                    OrderCard(
                        order = item.order.toLegacyOrderModel(),
                        onClick = { onOrderClick(item.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (index < sections.lastIndex) {
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
