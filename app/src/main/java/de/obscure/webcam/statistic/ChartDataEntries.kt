package de.obscure.webcam.statistic

import com.github.mikephil.charting.data.Entry

data class ChartDataEntries(
    val dataTemperature: List<TemperatureEntry> = emptyList(),
    val dataRain: List<RainEntry> = emptyList()
) {
    val showZeroLine: Boolean = dataTemperature.size >= 2
            && dataTemperature.any { it.temperature < 0.0f }
            && dataTemperature.any { it.temperature > 0.0f }
}

data class TemperatureEntry(
    val milliseconds: Long,
    val temperature: Float,
    val rain: Float
) : Entry(milliseconds.toFloat(), temperature)

data class RainEntry(
    val milliseconds: Long,
    val temperature: Float,
    val rain: Float
) : Entry(milliseconds.toFloat(), rain)