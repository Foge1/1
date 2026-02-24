package com.loaderapp.ui.dispatcher

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.ui.OrderResponsesUiModel
import com.loaderapp.features.orders.ui.ResponseRowUiModel
import com.loaderapp.features.orders.ui.ResponsesViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.main.LocalBottomNavHeight

@Composable
fun ResponsesScreen(viewModel: ResponsesViewModel) {
    val state by viewModel.uiState.collectAsState()
    val bottomNavHeight = LocalBottomNavHeight.current
    val topBarHeightPx = LocalTopBarHeightPx.current
    val topBarHeight = with(LocalDensity.current) { topBarHeightPx.toDp() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    AppScaffold(title = "Отклики") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight + 8.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            when {
                state.loading -> LoadingView()
                state.items.isEmpty() -> EmptyStateView(
                    icon = Icons.Outlined.Person,
                    title = "Нет откликов",
                    message = "Когда грузчики откликнутся — они появятся здесь"
                )

                else -> {
                    val expandedMap = rememberSaveable(
                        saver = listSaver(
                            save = { state ->
                                state.entries.flatMap { entry ->
                                    listOf(entry.key, if (entry.value) 1L else 0L)
                                }
                            },
                            restore = { restored ->
                                mutableStateMapOf<Long, Boolean>().apply {
                                    restored.chunked(2).forEach { (orderId, expandedFlag) ->
                                        put(orderId, expandedFlag == 1L)
                                    }
                                }
                            }
                        )
                    ) { mutableStateMapOf<Long, Boolean>() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 0.dp,
                            bottom = bottomNavHeight + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items, key = { it.orderId }) { item ->
                            ResponseOrderCard(
                                item = item,
                                expanded = expandedMap[item.orderId] ?: false,
                                onExpandedChange = { expandedMap[item.orderId] = it },
                                pending = state.pendingActions.contains(item.orderId),
                                onToggle = { loaderId, isSelected ->
                                    viewModel.onToggleApplicant(item.orderId, loaderId, isSelected)
                                },
                                onStart = { viewModel.onStartClicked(item.orderId) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomNavHeight + 8.dp)
        )
    }
}

@Composable
private fun ResponseOrderCard(
    item: OrderResponsesUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    pending: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.address, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    item.cargoText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Выбрано: ${item.selectedCount}/${item.requiredCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!expanded) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Отклики: ${item.responsesCount}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }

                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    item.responses.forEach { response ->
                        ResponseRow(
                            item = response,
                            actionsBlocked = pending,
                            onToggle = { onToggle(response.loaderId, response.isSelected) }
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onStart,
                    enabled = item.canStart && !pending,
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Запустить")
                }
                if (!item.canStart) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.startDisabledReason ?: "Нужно выбрать грузчиков",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseRow(
    item: ResponseRowUiModel,
    actionsBlocked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.loaderName, maxLines = 1)
            if (item.isBusy) {
                Text(
                    text = "В работе",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        val enabled = item.canToggle && !actionsBlocked
        IconButton(onClick = onToggle, enabled = enabled) {
            if (item.isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Снять выбор")
            } else {
                Text("+")
            }
        }
    }
}
