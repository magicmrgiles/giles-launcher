package com.giles.einklauncher.data.widget.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Open-Meteo geocoding DTOs (https://geocoding-api.open-meteo.com) ----

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
)

@Serializable
data class GeocodingResult(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val admin1: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
)

// ---- Open-Meteo forecast DTOs (https://api.open-meteo.com) ----

@Serializable
data class ForecastResponse(
    val current: CurrentDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentDto(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
)

@Serializable
data class DailyDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
)

// ---- Domain models used by the UI ----

enum class TemperatureUnit(val apiValue: String, val symbol: String) {
    FAHRENHEIT("fahrenheit", "°"),
    CELSIUS("celsius", "°"),
}

/** A minimal weather-condition taxonomy the UI maps to a single outline glyph. */
enum class WeatherIcon { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

data class DayForecast(
    val label: String,   // "Today", "Mon", ...
    val icon: WeatherIcon,
    val high: Int,
    val low: Int,
)

data class Weather(
    val locationLabel: String,
    val currentTemp: Int,
    val currentIcon: WeatherIcon,
    val currentDescription: String,
    val daily: List<DayForecast>,
)

/** Maps WMO weather-interpretation codes to an [WeatherIcon] + short description. */
object WeatherCodes {
    fun icon(code: Int): WeatherIcon = when (code) {
        0 -> WeatherIcon.CLEAR
        1, 2 -> WeatherIcon.PARTLY_CLOUDY
        3 -> WeatherIcon.CLOUDY
        45, 48 -> WeatherIcon.FOG
        51, 53, 55, 56, 57 -> WeatherIcon.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherIcon.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherIcon.SNOW
        95, 96, 99 -> WeatherIcon.THUNDER
        else -> WeatherIcon.CLOUDY
    }

    fun description(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm, hail"
        else -> "—"
    }
}
