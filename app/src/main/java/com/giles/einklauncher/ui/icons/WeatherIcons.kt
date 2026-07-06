package com.giles.einklauncher.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.giles.einklauncher.data.widget.weather.WeatherIcon

/** Maps the condition taxonomy to a single outline glyph. */
fun WeatherIcon.vector(): ImageVector = when (this) {
    WeatherIcon.CLEAR -> Icons.Outlined.WbSunny
    WeatherIcon.PARTLY_CLOUDY -> Icons.Outlined.CloudQueue
    WeatherIcon.CLOUDY -> Icons.Outlined.Cloud
    WeatherIcon.FOG -> Icons.Outlined.BlurOn
    WeatherIcon.DRIZZLE -> Icons.Outlined.Grain
    WeatherIcon.RAIN -> Icons.Outlined.WaterDrop
    WeatherIcon.SNOW -> Icons.Outlined.AcUnit
    WeatherIcon.THUNDER -> Icons.Outlined.Bolt
}
