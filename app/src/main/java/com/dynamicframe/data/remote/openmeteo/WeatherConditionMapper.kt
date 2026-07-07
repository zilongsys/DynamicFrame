package com.dynamicframe.data.remote.openmeteo

internal data class WeatherConditionPresentation(
    val label: String,
    val emoji: String,
)

/** Etiquetas WMO en inglés (mockup Paradise). */
internal fun weatherConditionForCode(code: Int): WeatherConditionPresentation {
    return when (code) {
        0 -> WeatherConditionPresentation("Clear sky", "☀️")
        1 -> WeatherConditionPresentation("Mainly clear", "🌤️")
        2 -> WeatherConditionPresentation("Partly cloudy", "⛅")
        3 -> WeatherConditionPresentation("Overcast", "☁️")
        45, 48 -> WeatherConditionPresentation("Foggy", "🌫️")
        51, 53, 55 -> WeatherConditionPresentation("Drizzle", "🌦️")
        56, 57 -> WeatherConditionPresentation("Freezing drizzle", "🌧️")
        61, 63, 65 -> WeatherConditionPresentation("Rain", "🌧️")
        66, 67 -> WeatherConditionPresentation("Freezing rain", "🌧️")
        71, 73, 75 -> WeatherConditionPresentation("Snow", "❄️")
        77 -> WeatherConditionPresentation("Snow grains", "❄️")
        80, 81, 82 -> WeatherConditionPresentation("Showers", "🌦️")
        85, 86 -> WeatherConditionPresentation("Snow showers", "🌨️")
        95 -> WeatherConditionPresentation("Thunderstorm", "⛈️")
        96, 99 -> WeatherConditionPresentation("Thunderstorm with hail", "⛈️")
        else -> WeatherConditionPresentation("Cloudy", "☁️")
    }
}
