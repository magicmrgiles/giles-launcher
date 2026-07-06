package com.giles.einklauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giles.einklauncher.data.widget.CalendarEvent
import com.giles.einklauncher.ui.AlarmUiState
import com.giles.einklauncher.ui.CalendarUiState
import com.giles.einklauncher.ui.WeatherUiState
import com.giles.einklauncher.ui.components.Hairline
import com.giles.einklauncher.ui.components.noRippleClickable
import com.giles.einklauncher.ui.components.noRippleCombinedClickable
import com.giles.einklauncher.ui.icons.vector
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.einkColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WidgetExpansion { NONE, EVENT, WEATHER }

@Composable
fun Widget(
    is24Hour: Boolean,
    alarm: AlarmUiState,
    calendar: CalendarUiState,
    weather: WeatherUiState,
    expansion: WidgetExpansion,
    onToggleEvent: () -> Unit,
    onToggleWeather: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestLocation: () -> Unit,
    onRetryWeather: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .noRippleCombinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 20.dp),
    ) {
        ClockAndDate(is24Hour = is24Hour, alarm = alarm)

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventSummary(
                calendar = calendar,
                is24Hour = is24Hour,
                onClick = {
                    if (calendar is CalendarUiState.PermissionRequired) onRequestCalendarPermission()
                    else onToggleEvent()
                },
            )
            WeatherSummary(
                weather = weather,
                onClick = {
                    when (weather) {
                        is WeatherUiState.NeedsLocationPermission -> onRequestLocation()
                        is WeatherUiState.Error, is WeatherUiState.Offline -> onRetryWeather()
                        else -> onToggleWeather()
                    }
                },
            )
        }

        AnimatedVisibility(visible = expansion == WidgetExpansion.EVENT) {
            EventSchedule(calendar = calendar, is24Hour = is24Hour)
        }
        AnimatedVisibility(visible = expansion == WidgetExpansion.WEATHER) {
            WeatherForecast(weather = weather)
        }

        Hairline(Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun ClockAndDate(is24Hour: Boolean, alarm: AlarmUiState) {
    val colors = einkColors
    val context = LocalContext.current

    // Re-render on the minute boundary; SYSTEM time updates are cheap and battery-friendly.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            val ms = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000 - (ms % 60_000))
        }
    }

    val numericFmt = remember24(is24Hour)
    val date = Date(now)
    val timeNumeric = numericFmt.format(date)
    val period = if (is24Hour) null else SimpleDateFormat("a", Locale.getDefault()).format(date)
    val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = timeNumeric, style = EinkType.Clock, color = colors.content)
        if (period != null) {
            Text(
                text = period,
                style = EinkType.ClockPeriod,
                color = colors.content,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
            )
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = dateStr, style = EinkType.DateLine, color = colors.content)
        alarm.triggerTimeMillis?.let { millis ->
            Text(text = "  ·  ", style = EinkType.DateLine, color = colors.content)
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = "Next alarm",
                tint = colors.content,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = " " + remember24(is24Hour).format(Date(millis)),
                style = EinkType.WidgetSecondary,
                color = colors.content,
            )
        }
    }
}

@Composable
private fun EventSummary(
    calendar: CalendarUiState,
    is24Hour: Boolean,
    onClick: () -> Unit,
) {
    val colors = einkColors
    Row(
        Modifier.noRippleClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(15.dp),
        )
        Column {
            when (calendar) {
                CalendarUiState.PermissionRequired ->
                    Text("Show events", style = EinkType.WidgetPrimary, color = colors.content)

                CalendarUiState.Loading ->
                    Text("…", style = EinkType.WidgetPrimary, color = colors.content)

                is CalendarUiState.Ready -> {
                    val next = calendar.next
                    if (next == null) {
                        Text("No upcoming events", style = EinkType.WidgetPrimary, color = colors.content)
                    } else {
                        Text(
                            text = next.title,
                            style = EinkType.WidgetPrimary,
                            color = colors.content,
                        )
                        Text(
                            text = eventTimeLine(next, is24Hour),
                            style = EinkType.WidgetSecondary,
                            color = colors.content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSummary(
    weather: WeatherUiState,
    onClick: () -> Unit,
) {
    val colors = einkColors
    Row(
        Modifier.noRippleClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            when (weather) {
                WeatherUiState.Loading ->
                    Text("…", style = EinkType.WidgetPrimary, color = colors.content)

                is WeatherUiState.Ready -> {
                    Text("${weather.weather.currentTemp}°", style = EinkType.WidgetPrimary, color = colors.content)
                    Text(weather.weather.currentDescription, style = EinkType.WidgetSecondary, color = colors.content)
                }

                WeatherUiState.NeedsLocationPermission ->
                    Text("Set location", style = EinkType.WidgetPrimary, color = colors.content)

                WeatherUiState.Offline ->
                    Text("Offline", style = EinkType.WidgetPrimary, color = colors.content)

                WeatherUiState.Error ->
                    Text("Unavailable", style = EinkType.WidgetPrimary, color = colors.content)
            }
        }
        Icon(
            imageVector = when (weather) {
                is WeatherUiState.Ready -> weather.weather.currentIcon.vector()
                else -> Icons.Outlined.Cloud
            },
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun EventSchedule(calendar: CalendarUiState, is24Hour: Boolean) {
    val colors = einkColors
    val events = (calendar as? CalendarUiState.Ready)?.restOfDay ?: emptyList()
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (events.isEmpty()) {
            Text("Nothing else today", style = EinkType.WidgetSecondary, color = colors.content)
        } else {
            events.forEach { ev ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (ev.allDay) "All day" else formatTime(ev.begin, is24Hour),
                            style = EinkType.WidgetSecondary,
                            color = colors.content,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(text = ev.title, style = EinkType.WidgetPrimary, color = colors.content)
                    }
                    Text(
                        text = durationLabel(ev),
                        style = EinkType.WidgetSecondary,
                        color = colors.content,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherForecast(weather: WeatherUiState) {
    val colors = einkColors
    val data = (weather as? WeatherUiState.Ready)?.weather ?: return
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = data.locationLabel,
            style = EinkType.WidgetSecondary,
            color = colors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.daily.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(day.label, style = EinkType.WidgetSecondary, color = colors.content)
                    Icon(
                        imageVector = day.icon.vector(),
                        contentDescription = null,
                        tint = colors.content,
                        modifier = Modifier.size(15.dp),
                    )
                    Text("${day.high}°", style = EinkType.WidgetSecondary.copy(fontWeight = FontWeight.Medium), color = colors.content)
                    Text("${day.low}°", style = EinkType.WidgetSecondary, color = colors.content)
                }
            }
        }
    }
}

// ---- formatting helpers ----

@Composable
private fun remember24(is24Hour: Boolean): SimpleDateFormat =
    SimpleDateFormat(if (is24Hour) "H:mm" else "h:mm", Locale.getDefault())

private fun formatTime(millis: Long, is24Hour: Boolean): String =
    SimpleDateFormat(if (is24Hour) "H:mm" else "h:mm a", Locale.getDefault()).format(Date(millis))

private fun eventTimeLine(ev: CalendarEvent, is24Hour: Boolean): String =
    if (ev.allDay) "All day" else "${formatTime(ev.begin, is24Hour)} · ${durationLabel(ev)}"

private fun durationLabel(ev: CalendarEvent): String {
    if (ev.allDay) return "All day"
    val minutes = ((ev.end - ev.begin) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} hr"
        else -> String.format(Locale.getDefault(), "%.1f hr", minutes / 60.0)
    }
}
