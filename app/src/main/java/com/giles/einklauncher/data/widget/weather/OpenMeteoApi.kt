package com.giles.einklauncher.data.widget.weather

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo endpoints. No API key required, HTTPS only. Two hosts are involved
 * (geocoding + forecast) so this interface uses absolute-path @Url-free calls with a base
 * URL per Retrofit instance; see [WeatherApi.Factory].
 */
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
    ): ForecastResponse
}
