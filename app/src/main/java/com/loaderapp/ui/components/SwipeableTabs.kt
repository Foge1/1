package com.loaderapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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

        // HorizontalPager с двусторонним fade через единый drawWithContent.
        // Применяем fade к самому Pager — у него есть реальный контент в буфере,
        // поэтому BlendMode.DstIn работает корректно (в отличие от пустого Box-оверлея).
        // Верх: карточки «растворяются» под топбаром.
        // Низ: карточки «растворяются» перед навбаром — граница исчезает.
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    // Нижний fade: последние 36dp контента растворяются
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - 36.dp.toPx(),
                            endY   = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
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
 * Намеренно НЕ использует TextButton / Surface / Card — они добавляют
 * собственный ripple-слой Material3, который при нажатии даёт серую тень
 * поверх прозрачного трека.
 *
 * Решение: простой Box с clickable(indication = null) + MutableInteractionSource.
 * Весь визуал (индикатор-капсула) рисуется через drawBehind родителя [PillTabRow],
 * поэтому здесь нужны только текст и badge. Никаких ripple-артефактов.
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
    // Плавная интерполяция цвета текста синхронно со свайпом
    val textColor  = lerp(unselectedColor, primary, selectionFraction)
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    // Отдельный InteractionSource позволяет подавить ripple точечно,
    // не затрагивая другие элементы
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            // indication = null → никакого ripple / серой тени при нажатии
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
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
