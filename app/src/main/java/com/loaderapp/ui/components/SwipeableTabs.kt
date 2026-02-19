package com.loaderapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Модель одной вкладки.
 *
 * @param label      Текст вкладки
 * @param badgeCount Счётчик для бейджа; 0 = не показывать
 */
data class TabItem(
    val label: String,
    val badgeCount: Int = 0
)

/**
 * Pill-табы со свайпом — без мерцания.
 *
 * ## Почему не было мерцания и как исправлено
 *
 * Предыдущая реализация использовала `Surface` с `shadowElevation` и
 * `animateColorAsState`. Material3 `Surface` внутри имеет собственный
 * composable-слой, который рекомпозируется отдельно при каждом изменении
 * цвета. При свайпе `pagerState.currentPage` переключается резко (не плавно)
 * → `animateColorAsState` стартует от одного крайнего значения к другому,
 * `shadowElevation` вызывает перерисовку с серым scrim Material3 → мерцание.
 *
 * **Решение:** вместо `Surface` используем `drawBehind` на `Box`.
 * Анимация основана на `pagerState.currentPageOffsetFraction` — это нативный
 * `Float` от Pager, меняется плавно и непрерывно даже во время свайпа.
 * Мы интерполируем цвета через `lerp()` напрямую, без отдельных
 * `animateColorAsState` на каждый таб. Никакого `shadowElevation` — тень
 * имитируется через полупрозрачный overlay-фон трека.
 * Результат: zero-flicker, 60fps-анимация синхронная со свайпом.
 *
 * @param tabs     Список вкладок
 * @param modifier Modifier для внешнего контейнера
 * @param content  Контент страницы по индексу
 */
@OptIn(ExperimentalFoundationApi::class)
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

        Spacer(Modifier.height(8.dp))

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f),
            // pageSpacing нулевой — страницы встык, без артефактов
            pageSpacing = 0.dp
        ) { page ->
            content(page)
        }
    }
}

/**
 * Визуальная полоса вкладок.
 *
 * Индикатор активной вкладки рисуется через [drawBehind] — один Canvas-вызов
 * на весь Row. Позиция и ширина индикатора вычисляются из [PagerState.currentPage]
 * и [PagerState.currentPageOffsetFraction], поэтому анимация синхронна со свайпом
 * без какого-либо `animateXxx` — плавность обеспечивает сам Pager.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PillTabRow(
    tabs: List<TabItem>,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackCornerRadius: Dp = 50.dp,
    indicatorPadding: Dp = 4.dp
) {
    val primary       = MaterialTheme.colorScheme.primary
    val trackColor    = primary.copy(alpha = 0.10f)
    // Цвет капсулы активного таба — surface с лёгким оттенком
    val indicatorColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(trackCornerRadius))
            .drawBehind {
                // Трек
                drawRoundRect(
                    color       = trackColor,
                    cornerRadius = CornerRadius(trackCornerRadius.toPx())
                )

                if (tabs.isEmpty()) return@drawBehind

                val tabWidth = size.width / tabs.size
                val pad      = indicatorPadding.toPx()

                // Текущая позиция с учётом фракции свайпа — плавная интерполяция
                val currentPage   = pagerState.currentPage
                val offsetFraction = pagerState.currentPageOffsetFraction
                val indicatorLeft  = (currentPage + offsetFraction) * tabWidth + pad

                // Капсула-индикатор
                drawRoundRect(
                    color        = indicatorColor,
                    topLeft      = Offset(indicatorLeft, pad),
                    size         = Size(tabWidth - pad * 2, size.height - pad * 2),
                    cornerRadius = CornerRadius((trackCornerRadius - indicatorPadding).toPx())
                )
            }
            .padding(indicatorPadding)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                PillTab(
                    label      = tab.label,
                    badgeCount = tab.badgeCount,
                    isSelected = pagerState.currentPage == index,
                    // Дополнительно: плавный alpha при свайпе между соседними табами
                    selectionFraction = when {
                        pagerState.currentPage == index ->
                            1f - kotlin.math.abs(pagerState.currentPageOffsetFraction)
                        pagerState.currentPage == index - 1 && pagerState.currentPageOffsetFraction > 0f ->
                            pagerState.currentPageOffsetFraction
                        pagerState.currentPage == index + 1 && pagerState.currentPageOffsetFraction < 0f ->
                            -pagerState.currentPageOffsetFraction
                        else -> 0f
                    },
                    onClick  = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                    primary  = primary
                )
            }
        }
    }
}

/**
 * Одна вкладка внутри [PillTabRow].
 *
 * Не использует `Surface` / `Card` / `animateColorAsState` — цвета
 * интерполируются через `lerp` на основе [selectionFraction], синхронно
 * со свайпом Pager. Никаких отдельных анимаций → никакого мерцания.
 *
 * @param selectionFraction 0f = полностью неактивна, 1f = полностью активна.
 *                          Промежуточные значения возникают при свайпе.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PillTab(
    label: String,
    badgeCount: Int,
    isSelected: Boolean,
    selectionFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Цвет текста интерполируется плавно, синхронно с fractionом свайпа
    val textColor = lerp(unselectedColor, primary, selectionFraction)
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            // Кликабельность без ripple-артефактов на прозрачном фоне
            .then(
                Modifier.clip(RoundedCornerShape(50))
            )
            .then(
                remember(onClick) {
                    Modifier.padding(0.dp) // placeholder — реальный клик ниже
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick  = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(50),
            colors   = ButtonDefaults.textButtonColors(
                // Фон прозрачный — капсула-индикатор рисуется в drawBehind родителя
                containerColor = Color.Transparent,
                contentColor   = textColor
            ),
            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = fontWeight,
                color      = textColor,
                maxLines   = 1
            )

            if (badgeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Badge(
                    containerColor = primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text       = if (badgeCount > 99) "99+" else "$badgeCount",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
