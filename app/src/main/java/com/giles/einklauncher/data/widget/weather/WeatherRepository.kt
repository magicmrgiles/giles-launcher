package com.giles.einklauncher.data.widget.weather

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

sealed interface WeatherResult {
    data class Success(val weather: Weather) : WeatherResult
    data object NoLocation : WeatherResult   // no manual location and no location permission/fix
    data object Offline : WeatherResult      // no network
    data object Error : WeatherResult        // request/parse failure
}

/**
 * Fetches weather from Open-Meteo. Location priority: a non-blank manual location (geocoded
 * via Open-Meteo) wins; otherwise the device's coarse location is used. All work is off the
 * main thread and every failure path maps to a [WeatherResult] the widget can render
 * gracefully.
 */
@OptIn(ExperimentalSerializationApi::class)
class WeatherRepository(
    private val context: Context,
    private val locationProvider: LocationProvider = LocationProvider(context),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder().build()

    private val geocodingApi: GeocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(GeocodingApi::class.java)

    private val forecastApi: ForecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(ForecastApi::class.java)

    private val unit: TemperatureUnit = defaultUnitForLocale()

    suspend fun load(manualLocation: String): WeatherResult = withContext(Dispatchers.IO) {
        if (!NetworkStatus.isOnline(context)) return@withContext WeatherResult.Offline

        val point: GeoPoint = when {
            manualLocation.isNotBlank() ->
                geocode(manualLocation) ?: return@withContext WeatherResult.Error
            else -> locationProvider.current()?.let { GeoPoint(it.latitude, it.longitude, it.label) }
                ?: return@withContext WeatherResult.NoLocation
        }

        runCatching {
            val response = forecastApi.forecast(
                latitude = point.lat,
                longitude = point.lon,
                temperatureUnit = unit.apiValue,
            )
            toWeather(point.label, response)
        }.fold(
            onSuccess = { it?.let(WeatherResult::Success) ?: WeatherResult.Error },
            onFailure = { WeatherResult.Error },
        )
    }

    private data class GeoPoint(val lat: Double, val lon: Double, val label: String)

    private suspend fun geocode(query: String): GeoPoint? = runCatching {
        geocodingApi.search(query).results?.firstOrNull()?.let { r ->
            val label = listOfNotNull(r.name, r.admin1).firstOrNull() ?: r.name
            GeoPoint(r.latitude, r.longitude, label)
        }
    }.getOrNull()

    private fun toWeather(label: String, r: ForecastResponse): Weather? {
        val current = r.current ?: return null
        val daily = r.daily ?: return null
        val temp = current.temperature ?: return null
        val code = current.weatherCode ?: 0

        val days = daily.time.indices.mapNotNull { i ->
            val hi = daily.tempMax.getOrNull(i) ?: return@mapNotNull null
            val lo = daily.tempMin.getOrNull(i) ?: return@mapNotNull null
            val wc = daily.weatherCode.getOrNull(i) ?: 0
            DayForecast(
                label = dayLabel(daily.time[i], i),
                icon = WeatherCodes.icon(wc),
                high = hi.roundToInt(),
                low = lo.roundToInt(),
            )
        }

        return Weather(
            locationLabel = label,
            currentTemp = temp.roundToInt(),
            currentIcon = WeatherCodes.icon(code),
            currentDescription = WeatherCodes.description(code),
            daily = days,
        )
    }

    private fun dayLabel(isoDate: String, index: Int): String {
        if (index == 0) return "Today"
        return runCatching {
            LocalDate.parse(isoDate).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }.getOrDefault(isoDate)
    }

    private fun defaultUnitForLocale(): TemperatureUnit {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        // The three countries that use Fahrenheit day-to-day.
        return if (country in setOf("US", "LR", "MM")) TemperatureUnit.FAHRENHEIT
        else TemperatureUnit.CELSIUS
    }
}
