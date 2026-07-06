package com.giles.einklauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.giles.einklauncher.data.settings.ClockStyle
import com.giles.einklauncher.data.settings.LauncherSettings
import com.giles.einklauncher.ui.components.EinkBottomSheet
import com.giles.einklauncher.ui.components.SegmentedControl
import com.giles.einklauncher.ui.components.noRippleClickable
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.ThemeMode
import com.giles.einklauncher.ui.theme.einkColors

@Composable
fun SettingsSheet(
    settings: LauncherSettings,
    deviceIs24Hour: Boolean,
    isDefaultLauncher: Boolean,
    notificationAccessGranted: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onClockStyle: (ClockStyle) -> Unit,
    onWeatherLocation: (String) -> Unit,
    onAppCount: (Int) -> Unit,
    onNotifFilter: (Boolean) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = einkColors

    EinkBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Launcher settings", style = EinkType.Control, color = colors.content)
                Icon(
                    Icons.Outlined.Close, "Close",
                    tint = colors.content,
                    modifier = Modifier.size(18.dp).noRippleClickable(onDismiss),
                )
            }

            if (!isDefaultLauncher) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.content)
                        .noRippleClickable(onRequestDefaultLauncher)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Set as default launcher", style = EinkType.Control, color = colors.background)
                }
                Box(Modifier.height(22.dp))
            }

            SectionHeader("Appearance")
            SegmentedControl(
                options = listOf(
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.SYSTEM to "System",
                ),
                selected = settings.themeMode,
                onSelect = onThemeMode,
            )
            Gap()

            SectionHeader("Clock style")
            val selectedClock = settings.clockStyle
                ?: if (deviceIs24Hour) ClockStyle.TWENTY_FOUR_HOUR else ClockStyle.TWELVE_HOUR
            SegmentedControl(
                options = listOf(
                    ClockStyle.TWELVE_HOUR to "12-hour",
                    ClockStyle.TWENTY_FOUR_HOUR to "24-hour",
                ),
                selected = selectedClock,
                onSelect = onClockStyle,
            )
            Gap()

            SectionHeader("Weather location")
            WeatherLocationField(
                initial = settings.weatherLocation,
                onCommit = onWeatherLocation,
            )
            Gap()

            SectionHeader("Number of apps")
            SegmentedControl(
                options = listOf(4 to "4", 8 to "8"),
                selected = settings.appCount,
                onSelect = onAppCount,
            )
            Gap()

            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notification filter", style = EinkType.Control, color = colors.content)
                EinkToggle(
                    checked = settings.notificationFilterEnabled,
                    onCheckedChange = { enabled ->
                        onNotifFilter(enabled)
                        if (enabled && !notificationAccessGranted) onOpenNotificationAccess()
                    },
                )
            }
            Text(
                "Hide all notifications except those from pinned apps.",
                style = EinkType.WidgetSecondary,
                color = colors.content,
            )
            if (settings.notificationFilterEnabled && !notificationAccessGranted) {
                Box(Modifier.height(8.dp))
                Text(
                    "Notification access not granted — tap to grant.",
                    style = EinkType.WidgetSecondary,
                    color = colors.content,
                    modifier = Modifier.noRippleClickable(onOpenNotificationAccess),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = EinkType.SectionHeader,
        color = einkColors.content,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun Gap() {
    Box(Modifier.height(22.dp))
}

@Composable
private fun WeatherLocationField(initial: String, onCommit: (String) -> Unit) {
    val colors = einkColors
    var text by remember(initial) { mutableStateOf(initial) }
    val latest = rememberUpdatedState(text)

    // Commit on Done, and also when the sheet closes, so an edit isn't lost on dismiss.
    DisposableEffect(Unit) {
        onDispose { if (latest.value != initial) onCommit(latest.value) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.content, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = EinkType.Control.copy(color = colors.content),
            cursorBrush = SolidColor(colors.content),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit(text) }),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text("e.g. Seattle, WA", style = EinkType.Control, color = colors.content)
                }
                inner()
            },
        )
    }
}

/** Custom monochrome toggle: filled track when on, outline knob positions swap. */
@Composable
private fun EinkToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = einkColors
    Row(
        Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, colors.content, RoundedCornerShape(11.dp))
            .background(if (checked) colors.content else colors.background)
            .noRippleClickable { onCheckedChange(!checked) }
            .padding(3.dp),
        horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (checked) colors.background else colors.content)
        )
    }
}
