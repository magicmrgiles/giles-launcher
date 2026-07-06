package com.giles.einklauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.giles.einklauncher.data.apps.PinnedAppRef
import com.giles.einklauncher.ui.PinnedSlot
import com.giles.einklauncher.ui.components.noRippleClickable
import com.giles.einklauncher.ui.components.noRippleCombinedClickable
import com.giles.einklauncher.ui.icons.AppIconImage
import com.giles.einklauncher.ui.theme.einkColors

/**
 * The bottom-aligned pinned-app grid. 4 columns, 1–2 rows. Long-pressing a filled icon
 * enters edit mode (every icon shows an X badge); tapping an X clears the slot; tapping an
 * empty slot opens the picker. Long-pressing empty grid space opens Settings.
 */
@Composable
fun AppGrid(
    slots: List<PinnedSlot>,
    editMode: Boolean,
    onLaunch: (PinnedAppRef) -> Unit,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onRemove: (Int) -> Unit,
    onEmptyTap: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = slots.chunked(4)
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(editMode) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onLongPress = { onOpenSettings() },
                    onTap = { if (editMode) onExitEdit() },
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        rows.forEach { rowSlots ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                rowSlots.forEach { slot ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        GridTile(
                            slot = slot,
                            editMode = editMode,
                            onLaunch = onLaunch,
                            onEnterEdit = onEnterEdit,
                            onRemove = onRemove,
                            onEmptyTap = onEmptyTap,
                        )
                    }
                }
                // Pad short final rows so tiles stay left/column-aligned.
                repeat(4 - rowSlots.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GridTile(
    slot: PinnedSlot,
    editMode: Boolean,
    onLaunch: (PinnedAppRef) -> Unit,
    onEnterEdit: () -> Unit,
    onRemove: (Int) -> Unit,
    onEmptyTap: (Int) -> Unit,
) {
    val colors = einkColors
    // Label-less grid, matching the prototype — real (monochrome) icons carry recognition.
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.content, RoundedCornerShape(16.dp))
                .background(colors.background)
                .noRippleCombinedClickable(
                    onClick = {
                        when (slot) {
                            is PinnedSlot.Empty -> onEmptyTap(slot.index)
                            is PinnedSlot.Filled -> if (!editMode) onLaunch(slot.ref)
                        }
                    },
                    onLongClick = {
                        if (slot is PinnedSlot.Filled) onEnterEdit()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (slot) {
                is PinnedSlot.Empty -> Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add app",
                    tint = colors.content,
                    modifier = Modifier.size(20.dp),
                )

                is PinnedSlot.Filled -> AppIconImage(
                    ref = slot.ref,
                    fallback = slot.monogram(),
                    modifier = Modifier
                        .size(52.dp)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }

        if (editMode && slot is PinnedSlot.Filled) {
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.content)
                    .noRippleClickable { onRemove(slot.index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove app",
                    tint = colors.background,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

private fun PinnedSlot.Filled.monogram(): String =
    label.trim().firstOrNull()?.uppercase() ?: "?"
