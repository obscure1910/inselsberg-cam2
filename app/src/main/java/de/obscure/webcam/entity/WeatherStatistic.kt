package de.obscure.webcam.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class WeatherStatistic(
    @PrimaryKey val id: Long,
    val timestamp: Long,
    val temperature: Float = 0.0f,
    val humidity: Float = 0.0f,
    val airPressure: Float = 0.0f,
    val airPressureTrend: String?,
    val windVelocity: Float = 0.0f,
    val windGust: Float = 0.0f,
    val rain: Float = 0.0f,
    val rainLastHours: Float = 0.0f,
    val isLiftOpen: Boolean?,
): Parcelable {

    fun milliseconds(): Long = timestamp * 1000

}

data class WeatherStatistics(
    val statistics: List<WeatherStatistic>
)