package com.loaderapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Модель одной вкладки для [SwipeableTabs].
 *
 * @param label      Текст вкладки
 * @param badgeCount Количество для бейджа (0 = не показывать)
 */
data class TabItem(
    val label: String,
    val badgeCount: Int = 0
)

/**
 * Переиспользуемые pill-табы со свайпом между страницами.
 *
 * Внешний вид: скруглённый контейнер-трек, активная вкладка —
 * белая/surface капсула с тенью. Соответствует скриншоту из задачи.
 *
 * Свайп реализован через [HorizontalPager] из `androidx.compose.foundation`,
 * без сторонних библиотек. Нажатие на вкладку и свайп синхронизированы
 * через общий [PagerState].
 *
 * @param tabs       Список вкладок
 * @param modifier   Modifier для контейнера
 * @param content    Контент каждой страницы, получает индекс
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SwipeableTabs(
    tabs: List<TabItem>,
    modifier: Modifier = Modifier,
    content: @Composable (pageIndex: Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope      = rememberCoroutineScope()

    Column(modifier = modifier) {
        PillTabRow(
            tabs          = tabs,
            pagerState    = pagerState,
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        )

        Spacer(Modifier.height(4.dp))

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            content(page)
        }
    }
}

/**
 * Визуальная полоса pill-табов.
 *
 * Стиль: фон-трек с низкой прозрачностью primary, активная вкладка —
 * surface-капсула. Анимация цвета при переключении — 200 мс.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PillTabRow(
    tabs: List<TabItem>,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(primary.copy(alpha = 0.10f))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                PillTab(
                    label      = tab.label,
                    badgeCount = tab.badgeCount,
                    isSelected = pagerState.currentPage == index,
                    onClick    = { onTabSelected(index) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Одна pill-вкладка.
 *
 * Активная — surface фон + bold текст primary цвета.
 * Неактивная — прозрачный фон + onSurfaceVariant.
 */
@Composable
private fun PillTab(
    label: String,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(200),
        label         = "tab_bg"
    )
    val textColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label         = "tab_text"
    )

    Surface(
        onClick   = onClick,
        modifier  = modifier,
        shape     = RoundedCornerShape(50),
        color     = bgColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = textColor,
                maxLines   = 1
            )

            if (badgeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = if (badgeCount > 99) "99+" else "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
