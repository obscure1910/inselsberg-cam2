package de.obscure.webcam.repositories

import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.obscure.webcam.DateTimeExtensions.forGermany
import de.obscure.webcam.database.WeatherStatisticDao
import de.obscure.webcam.entity.WeatherStatistic
import de.obscure.webcam.network.InselsbergApiService
import de.obscure.webcam.viewmodels.NetworkConnectionType
import de.obscure.webcam.viewmodels.SharedViewmodel
import kotlinx.coroutines.yield
import org.joda.time.DateTime

import org.joda.time.Duration
import timber.log.Timber

class WeatherStatisticRepository(
    private val viewmodel: SharedViewmodel,
    private val dao: WeatherStatisticDao,
    private val network: InselsbergApiService
) {

    suspend fun refreshWeatherStatistics(): Result {
        return refresh(this::isNullOrToOld)
    }

    private suspend fun refresh(condition: (WeatherStatistic?) -> Boolean): Result {
        // server expects millis of zone europe/berlin
        val since = (DateTime.now().forGermany().millis - FIVE_DAYS_IN_MILLIS) / 1000
        val tmp = dao.getLatest()
        val networkConnectionType = viewmodel.requestNetworkConnectionType()
        return if (condition.invoke(tmp) &&
            networkConnectionType == NetworkConnectionType.Fast ||
            networkConnectionType == NetworkConnectionType.Slow) {
            FirebaseCrashlytics.getInstance().log("WeatherStatisticRepository - fulfilled condition")
            Timber.i("Load weather statistics from the server")
            try {
                val response = network.loadStatistic(since)
                val weatherstatistics = response.body()?.statistics?.sortedBy { it.timestamp }
                if (response.isSuccessful && !weatherstatistics.isNullOrEmpty()) {
                    val last = weatherstatistics.last()
                    if (tmp != last) {
                        FirebaseCrashlytics.getInstance().log("WeatherStatisticRepository - update db")
                        Timber.i("Update DB with new received data")
                        yield() // chance to cancel the coroutine job
                        dao.updateAll(weatherstatistics)
                    }
                    Result.Success
                } else {
                    Result.Failure
                }
            } catch (e: Exception) {
                Timber.e(e)
                FirebaseCrashlytics.getInstance().log("WeatherStatisticRepository - ${e.message}")
                Result.Failure
            }
        } else {
            Result.Success
        }
    }

    private fun isNullOrToOld(weatherStatistic: WeatherStatistic?): Boolean {
        return if(weatherStatistic == null) {
            true
        } else {
            DateTime(weatherStatistic.milliseconds()).plus(Duration.standardMinutes(10)).isBeforeNow
        }

    }

    companion object {
        val FIVE_DAYS_IN_MILLIS = Duration.standardDays(5).millis
    }

    sealed class Result {
        object Success : Result()
        object Failure : Result()
    }


}