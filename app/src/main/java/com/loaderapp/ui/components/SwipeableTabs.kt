package com.loaderapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.ui.theme.LoaderAppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class TabItem(
    val label: String,
    val badgeCount: Int = 0,
)

private object SwipeableTabsDefaults {
    val TRACK_SHAPE = AppShapes.small
    val TAB_SHAPE = RoundedCornerShape(AppSpacing.sm + AppSpacing.xxs)
    val TRACK_INNER_PADDING = AppSpacing.xs
    val TAB_VERTICAL_PADDING = AppSpacing.sm
    val TAB_HORIZONTAL_PADDING = AppSpacing.md
    const val BADGE_ALPHA_ACTIVE = 0.2f
    val BADGE_SPACING = AppSpacing.xs
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTabs(
    tabs: List<TabItem>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    tabsToPagerSpacing: Dp = AppSpacing.sm,
    tabVerticalPadding: Dp = SwipeableTabsDefaults.TAB_VERTICAL_PADDING,
    tabHorizontalPadding: Dp = SwipeableTabsDefaults.TAB_HORIZONTAL_PADDING,
    tabRowHorizontalPadding: Dp = AppSpacing.lg,
    content: @Composable (pageIndex: Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Column(modifier = modifier) {
        SegmentedTabRow(
            tabs = tabs,
            pagerState = pagerState,
            tabVerticalPadding = tabVerticalPadding,
            tabHorizontalPadding = tabHorizontalPadding,
            tabRowHorizontalPadding = tabRowHorizontalPadding,
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
        )

        Spacer(modifier = Modifier.height(tabsToPagerSpacing))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            content(page)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SegmentedTabRow(
    tabs: List<TabItem>,
    pagerState: PagerState,
    tabVerticalPadding: Dp,
    tabHorizontalPadding: Dp,
    tabRowHorizontalPadding: Dp,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorProgressTarget = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val indicatorProgress by
        animateFloatAsState(
            targetValue = indicatorProgressTarget,
            animationSpec = AppMotion.tweenLong(),
            label = "segmented_tabs_indicator",
        )

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = tabRowHorizontalPadding)
                .background(
                    color = AppColors.Muted,
                    shape = SwipeableTabsDefaults.TRACK_SHAPE,
                ).padding(SwipeableTabsDefaults.TRACK_INNER_PADDING),
    ) {
        if (tabs.isNotEmpty()) {
            val tabWidth = maxWidth / tabs.size
            val density = LocalDensity.current
            val indicatorOffsetPx = with(density) { (tabWidth * indicatorProgress).toPx() }

            Box(
                modifier =
                    Modifier
                        .offset { IntOffset(x = indicatorOffsetPx.roundToInt(), y = 0) }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .background(
                            color = AppColors.Primary,
                            shape = SwipeableTabsDefaults.TAB_SHAPE,
                        ),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                SegmentedTab(
                    label = tab.label,
                    badgeCount = tab.badgeCount,
                    isSelected = pagerState.currentPage == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                    verticalPadding = tabVerticalPadding,
                    horizontalPadding = tabHorizontalPadding,
                )
            }
        }
    }
}

@Composable
private fun SegmentedTab(
    label: String,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp,
    horizontalPadding: Dp,
) {
    val textColor = if (isSelected) AppColors.OnPrimary else AppColors.MutedForeground
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(SwipeableTabsDefaults.BADGE_SPACING))

            Badge(
                containerColor =
                    if (isSelected) {
                        AppColors.OnPrimary.copy(alpha = SwipeableTabsDefaults.BADGE_ALPHA_ACTIVE)
                    } else {
                        AppColors.Border
                    },
                contentColor = if (isSelected) AppColors.OnPrimary else AppColors.MutedForeground,
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwipeableTabsPreview() {
    LoaderAppTheme {
        SwipeableTabs(
            tabs =
                listOf(
                    TabItem(label = "Available", badgeCount = 12),
                    TabItem(label = "In Progress", badgeCount = 3),
                    TabItem(label = "History", badgeCount = 9),
                ),
            modifier = Modifier.height(AppSpacing.xxxl * 5),
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
