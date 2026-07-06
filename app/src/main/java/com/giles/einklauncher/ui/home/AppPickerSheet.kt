package com.giles.einklauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.giles.einklauncher.data.apps.AppInfo
import com.giles.einklauncher.ui.components.EinkBottomSheet
import com.giles.einklauncher.ui.components.noRippleClickable
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.einkColors

/**
 * Bottom sheet listing every installed app not already pinned. Tapping one assigns it to
 * the slot. Includes the "i" info affordance explaining the drawer-less model.
 */
@Composable
fun AppPickerSheet(
    apps: List<AppInfo>,
    onPick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = einkColors
    var showTip by remember { mutableStateOf(false) }

    EinkBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Choose an app for this slot",
                        style = EinkType.Control.copy(fontWeight = FontWeight.Medium),
                        color = colors.content,
                    )
                    Box(Modifier.padding(start = 8.dp)) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, colors.content, RoundedCornerShape(8.dp))
                                .noRippleClickable { showTip = !showTip },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("i", style = EinkType.WidgetSecondary, color = colors.content)
                        }
                        if (showTip) {
                            Popup(onDismissRequest = { showTip = false }) {
                                Box(
                                    Modifier
                                        .padding(top = 22.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.content)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        "No app drawer — hold an app to remove it, tap an empty slot to add one.",
                                        style = EinkType.WidgetSecondary,
                                        color = colors.background,
                                    )
                                }
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = colors.content,
                    modifier = Modifier.size(18.dp).noRippleClickable(onDismiss),
                )
            }

            if (apps.isEmpty()) {
                Text(
                    "No other apps available to add.",
                    style = EinkType.WidgetSecondary,
                    color = colors.content,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(apps, key = { it.packageName + "/" + it.activityName }) { app ->
                        PickerTile(app = app, onClick = { onPick(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerTile(app: AppInfo, onClick: () -> Unit) {
    val colors = einkColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.noRippleClickable(onClick),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.content, RoundedCornerShape(14.dp))
                .background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.monogram, style = EinkType.Monogram, color = colors.content)
        }
        Text(
            text = app.label,
            style = EinkType.AppLabel,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
