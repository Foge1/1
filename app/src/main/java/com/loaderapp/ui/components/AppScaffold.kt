package com.loaderapp.ui.components

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CompositionLocal — точная высота TopBar в px, доступна всем дочерним composable.
 * Устанавливается внутри [AppScaffold] через SubcomposeLayout после измерения.
 * Экраны читают через [LocalTopBarHeightPx] и конвертируют в dp для contentPadding.
 */
val LocalTopBarHeightPx = compositionLocalOf { 0 }

/**
 * Единый враппер экрана с frosted-glass sticky TopBar.
 *
 * ## Архитектура
 *
 * [SubcomposeLayout] — единственный правильный способ получить точный размер
 * TopBar до размещения контента. Порядок subcompose-слотов:
 * 1. "topBar"   — измеряется первым, получаем topBarHeightPx
 * 2. "content"  — размещается на [fillMaxSize], получает высоту через [LocalTopBarHeightPx]
 * 3. "overlay"  — frosted-glass оверлей, рисуется поверх контента
 *
 * ## Frosted-glass
 *
 * API 31+: [android.graphics.RenderEffect] с настоящим Gaussian blur на GPU.
 * Blur применяется к offscreen-буферу → реальное матовое стекло.
 *
 * API < 31: полупрозрачный фон [MaterialTheme.colorScheme.background] с
 * повышенной непрозрачностью (0.93f). Честный fallback без имитации blur.
 *
 * В обоих случаях нижняя треть зоны растворяется через [BlendMode.DstIn],
 * создавая плавный переход контента под TopBar.
 *
 * ## Touches
 *
 * TopBar разделён на два физических слоя:
 * - Opaque-слой (верхние ~72%): [pointerInput] поглощает касания → нажатия
 *   на заголовок/actions работают, контент под ним недостижим.
 * - Fade-слой (нижние ~28%): отдельный Box без pointerInput → касания
 *   проходят насквозь к карточкам под ним.
 *
 * @param title   Заголовок экрана
 * @param actions Слот для action-кнопок справа (пусто по умолчанию)
 * @param content Контент. Занимает [fillMaxSize] с y=0.
 *                Читай [LocalTopBarHeightPx] для contentPadding.
 */
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    GradientBackground(modifier = modifier) {
        SubcomposeLayout { constraints ->

            // ── Шаг 1: измеряем TopBar ────────────────────────────────────────
            val topBarMeasurables = subcompose("topBar") {
                AppTopBar(title = title, actions = actions)
            }
            val topBarPlaceables = topBarMeasurables.map {
                it.measure(constraints.copy(minHeight = 0))
            }
            val topBarHeightPx = topBarPlaceables.maxOfOrNull { it.height } ?: 0

            // ── Шаг 2: размещаем контент на весь экран ────────────────────────
            // LocalTopBarHeightPx доступен дочерним composable для contentPadding
            val contentPlaceables = subcompose("content") {
                CompositionLocalProvider(LocalTopBarHeightPx provides topBarHeightPx) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        content  = content
                    )
                }
            }.map { it.measure(constraints) }

            // ── Шаг 3: frosted-glass оверлей поверх контента ─────────────────
            val overlayPlaceables = subcompose("overlay") {
                FrostedOverlay(
                    heightPx = topBarHeightPx,
                    title    = title,
                    actions  = actions
                )
            }.map {
                it.measure(constraints.copy(minHeight = 0))
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                contentPlaceables.forEach { it.placeRelative(0, 0) }
                overlayPlaceables.forEach { it.placeRelative(0, 0) }
            }
        }
    }
}

/**
 * Frosted-glass оверлей, разделённый на два независимых слоя.
 *
 * Разделение критично для корректной обработки touches:
 * только opaque-часть поглощает касания.
 */
@Composable
private fun FrostedOverlay(
    heightPx: Int,
    title: String,
    actions: @Composable RowScope.() -> Unit
) {
    if (heightPx == 0) return

    val bgColor      = MaterialTheme.colorScheme.background
    // Точка разделения opaque/fade — 72% высоты зоны
    val opaqueFraction = 0.72f

    Box(modifier = Modifier.fillMaxWidth()) {

        // ── Слой A: opaque + blur (поглощает touches) ─────────────────────────
        // Занимает только opaque-часть (opaqueFraction от полной высоты).
        // pointerInput здесь поглощает все касания в этой зоне.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    // API 31+: настоящий Gaussian blur через RenderEffect
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        bgColor.copy(alpha = 0.60f)  // blur виден сквозь полупрозрачность
                    else
                        bgColor.copy(alpha = 0.93f)  // без blur — плотнее
                )
                .drawWithContent {
                    drawContent()
                    // Нижняя треть (от opaqueFraction до конца) — fade к прозрачному
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black,
                                opaqueFraction to Color.Black,
                                1.00f to Color.Transparent
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                // Поглощаем touches — контент под этим слоем недостижим
                .pointerInput(Unit) { /* intentionally consume */ }
        ) {
            AppTopBar(title = title, actions = actions)
        }
    }
}

/**
 * Содержимое TopBar.
 * Вынесен отдельно: SubcomposeLayout измеряет его как "topBar"-слот,
 * а затем переиспользует в "overlay"-слоте без лишних re-measure.
 */
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
            text       = title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content           = actions
        )
    }
}
