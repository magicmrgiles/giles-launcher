package com.giles.einklauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.einkColors

/**
 * Clickable with no ripple/indication — a Material ripple would render as a translucent grey,
 * which the strict black-and-white design system forbids. Use everywhere in place of
 * [clickable] for tap feedback that stays monochrome.
 */
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.noRippleCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    combinedClickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/** A 1dp hairline in the content colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(einkColors.content)
    )
}

/**
 * Segmented control. The selected segment inverts: content-coloured fill with
 * background-coloured text. No tint, no grey.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = einkColors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.content, RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.content else colors.background)
                    .noRippleClickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = EinkType.Control,
                    color = if (isSelected) colors.background else colors.content,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Bottom-anchored modal sheet with a scrim. Matches the mockup: content-coloured scrim,
 * background-coloured panel with top-rounded corners and a hairline top border. Tapping the
 * scrim dismisses.
 */
@Composable
fun EinkBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = einkColors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val appear by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label = "sheet",
    )
    Box(Modifier.fillMaxSize()) {
        // Scrim
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.85f * appear }
                .background(colors.content)
                .pointerInput(Unit) { detectTapClose(onDismiss) }
        )
        // Panel
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = (1f - appear) * 80.dp.toPx()
                    alpha = appear
                }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.background)
                .border(
                    1.dp,
                    colors.content,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
        ) {
            content()
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapClose(
    onTap: () -> Unit,
) {
    androidx.compose.foundation.gestures.detectTapGestures(onTap = { onTap() })
}

/** Cosmetic gesture-navigation pill drawn at the very bottom. Does not intercept gestures. */
@Composable
fun GestureNavPill(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(108.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(einkColors.content)
        )
    }
}

/** A small square outline icon button used throughout (toolbar, close, info). */
@Composable
fun OutlineIconButton(
    onClick: () -> Unit,
    selected: Boolean = false,
    size: Int = 30,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = einkColors
    Box(
        modifier = modifier
            .width(size.dp)
            .height(size.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, colors.content, RoundedCornerShape(9.dp))
            .background(if (selected) colors.content else colors.background)
            .noRippleClickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Spacer helper for consistent row spacing without importing Spacer everywhere. */
@Composable
fun RowScope.WeightSpacer() {
    Box(Modifier.weight(1f))
}

/**
 * Minimal black-and-white confirmation dialog, used for permission rationales. The confirm
 * button is inverted (filled), the dismiss button is outlined.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = einkColors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.background)
                .border(1.dp, colors.content, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text(title, style = EinkType.Control.copy(fontSize = 15.sp), color = colors.content)
            Box(Modifier.height(10.dp))
            Text(message, style = EinkType.Body, color = colors.content)
            Box(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.content, RoundedCornerShape(12.dp))
                        .noRippleClickable(onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(dismissLabel, style = EinkType.Control, color = colors.content)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.content)
                        .noRippleClickable(onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(confirmLabel, style = EinkType.Control, color = colors.background)
                }
            }
        }
    }
}
