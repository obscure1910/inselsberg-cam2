package de.obscure.webcam.viewmodels

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.*
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivities
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import de.obscure.webcam.DateTimeExtensions.forGermany
import de.obscure.webcam.DateTimeExtensions.printGermanDayMonthFormat
import de.obscure.webcam.DateTimeExtensions.printGermanLongFormat
import de.obscure.webcam.R
import de.obscure.webcam.database.WeatherStatisticDao
import de.obscure.webcam.entity.WeatherStatistic
import de.obscure.webcam.statistic.ChartDataEntries
import de.obscure.webcam.statistic.RainEntry
import de.obscure.webcam.statistic.TemperatureEntry
import kotlinx.coroutines.*
import org.joda.time.DateTime
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume


class SharedViewmodel(
    statisticDao: WeatherStatisticDao,
    val application: Application
) : ViewModel() {

    private var firstStart = true

    private var destinationId: Int? = null

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    var shareableBitmap: Bitmap? = null
        private set

    private val _selectedEntry = MutableLiveData<TemperatureEntry>()

    private val _statisticEntries: LiveData<List<WeatherStatistic>> = Transformations.map(statisticDao.getAll()) { it }

    private val _lastWeatherStatistic: LiveData<WeatherStatistic?> = Transformations.map(_statisticEntries) { it.lastOrNull() }

    private val _preferredPhoto = MutableLiveData<PreferredPhoto>()

    val preferredPhoto: LiveData<PreferredPhoto>
        get() = _preferredPhoto

    val visibleEntry = MediatorLiveData<WeatherStatistic>()

    val overviewIcon = MediatorLiveData<Drawable>()

    val displayDate = Transformations.map(visibleEntry) { DateTime(it.milliseconds()).forGermany().printGermanLongFormat() }
    val displayTemperature = Transformations.map(visibleEntry) { application.getString(R.string.display_temperature, it.temperature) }
    val displayBaro = Transformations.map(visibleEntry) { application.getString(R.string.display_baro, it.airPressure) }
    val displayHydro = Transformations.map(visibleEntry) { application.getString(R.string.display_hydro, it.humidity) }
    val displayRain = Transformations.map(visibleEntry) { application.getString(R.string.display_rain, it.rain) }
    val displayRain24 = Transformations.map(visibleEntry) { application.getString(R.string.display_rain, it.rainLastHours) }
    val displayWind = Transformations.map(visibleEntry) { application.getString(R.string.display_windgust, it.windGust) }
    val displaySkilift = Transformations.map(visibleEntry) {
        if (it.isLiftOpen == true) {
            "Skilift geöffnet"
        } else {
            "Skilift geschlossen"
        }
    }
    val displaySkiliftColor = Transformations.map(visibleEntry) {
        if (it.isLiftOpen == true) {
            Color.argb(255, 14, 157, 14)
        } else {
            Color.RED
        }
    }
    val displaySkiliftVisibility = Transformations.map(visibleEntry) {
        if (isWinter(it.milliseconds())) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    val displayStatisticTitle = Transformations.map(_statisticEntries) { statisticEntries ->
        if (!statisticEntries.isNullOrEmpty()) {
            val start = DateTime(statisticEntries.first().milliseconds()).forGermany().printGermanDayMonthFormat()
            val end = DateTime(statisticEntries.last().milliseconds()).forGermany().printGermanDayMonthFormat()
            "Statistik ($start - $end)"
        } else {
            "Statistik"
        }
    }

    val chartEntries: LiveData<ChartDataEntries> = Transformations.map(_statisticEntries) { mapToChartDataEntries(it) }

    init {
        _preferredPhoto.value = PreferredPhoto.byId(
            application.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE).getInt(PreferredPhoto.SHAREDPREFERENCES_KEY, PreferredPhoto.SKIHANG.id)
        )

        visibleEntry.addSource(_statisticEntries) {
            if (!it.isNullOrEmpty()) {
                visibleEntry.value = it.last()
            }
        }

        visibleEntry.addSource(_selectedEntry) { temperatureEntry ->
            _statisticEntries.value?.let { statisticEntries ->
                if (!statisticEntries.isNullOrEmpty() && temperatureEntry != null) {
                    visibleEntry.value = statisticEntries.find { weatherStatistic -> weatherStatistic.milliseconds() == temperatureEntry.milliseconds }
                }
            }
        }

        overviewIcon.addSource(_lastWeatherStatistic) {
            visibleEntry.value?.let { visibleEntry ->
                if (visibleEntry == it) {
                    overviewIcon.value = ContextCompat.getDrawable(application, R.drawable.image)
                } else {
                    overviewIcon.value = ContextCompat.getDrawable(application, R.drawable.autorenew)
                }
            }
        }

        overviewIcon.addSource(visibleEntry) {
            _lastWeatherStatistic.value?.let { visibleEntry ->
                if (visibleEntry == it) {
                    overviewIcon.value = ContextCompat.getDrawable(application, R.drawable.image)
                } else {
                    overviewIcon.value = ContextCompat.getDrawable(application, R.drawable.autorenew)
                }
            }
        }
    }

    private fun mapToChartDataEntries(statistics: List<WeatherStatistic>): ChartDataEntries {
        return statistics.fold(ChartDataEntries()) { chartDataEntries, statistic ->
            chartDataEntries.copy(
                dataTemperature = chartDataEntries.dataTemperature + TemperatureEntry(statistic.timestamp * 1000, statistic.temperature, statistic.rain),
                dataRain = chartDataEntries.dataRain + RainEntry(statistic.timestamp * 1000, statistic.temperature, statistic.rain)
            )
        }
    }

    suspend fun requestNetworkConnectionType(): NetworkConnectionType {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val result: NetworkConnectionType? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            withTimeoutOrNull(1000) {
                suspendCancellableCoroutine { cont ->
                    connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                            super.onCapabilitiesChanged(network, networkCapabilities)
                            connectivityManager.unregisterNetworkCallback(this)
                            when {
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                                    cont.resume(NetworkConnectionType.Fast)
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                                    cont.resume(NetworkConnectionType.Slow)
                                else -> {
                                    cont.resume(NetworkConnectionType.Unavailable)
                                }
                            }
                        }

                        override fun onUnavailable() {
                            super.onUnavailable()
                            connectivityManager.unregisterNetworkCallback(this)
                            cont.resume(NetworkConnectionType.Unavailable)
                        }
                    })
                }
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> NetworkConnectionType.Fast
                    ConnectivityManager.TYPE_MOBILE -> NetworkConnectionType.Slow
                    else -> NetworkConnectionType.Unavailable
                }
            } else {
                NetworkConnectionType.Unavailable
            }
        }
        if(result == null || result == NetworkConnectionType.Unavailable ) {
            uiScope.launch {
                Toast.makeText(application, "Keine Internetverbindung!", Toast.LENGTH_SHORT).show()
            }
        }
        return result ?: NetworkConnectionType.Unavailable
    }

    fun onValueSelected(temperatureEntry: TemperatureEntry) {
        _selectedEntry.value = temperatureEntry
    }

    fun onNothingSelected() {
        _selectedEntry.value = null
    }

    fun onNavigation(destinationId: Int) {
        //show last statistic entry if overview bottom menu item is pressed two times
        if (this.destinationId != null && this.destinationId == destinationId && !firstStart) {
            _selectedEntry.value = null
            _lastWeatherStatistic.value?.let { visibleEntry.value = it }
        } else {
            this.destinationId = destinationId
        }
        if (firstStart) {
            firstStart = false
        }
    }

    fun onActivityCreate() {
        firstStart = true
    }

    fun onPreferredPhoto(preferredPhoto: PreferredPhoto) {
        val sharedPref = application.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(PreferredPhoto.SHAREDPREFERENCES_KEY, preferredPhoto.id)
            apply()
        }
        _preferredPhoto.value = preferredPhoto
    }

    fun hideDialog() {
        val sharedPref = application.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(SHOW_DIALOG, false)
            apply()
        }
    }

    fun isDialogVisible(): Boolean {
        val sharedPref = application.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE) ?: return true
        return sharedPref.getBoolean(SHOW_DIALOG, true)
    }

    fun setCurrentVisibleBitmap(bitmap: Bitmap) {
        val bitmapCopy = bitmap.copy(bitmap.config, false)
        uiScope.launch {
            shareableBitmap = bitmapCopy
        }
    }

    fun shareBitmap(activity: Activity) {
        shareableBitmap?.let { bitmap ->
            val mimeType = "image/jpeg"
            val mimeTypeArray = arrayOf(mimeType)

            val cachePath = File(activity.externalCacheDir, "inselsberg/")
            cachePath.mkdirs()

            val file = File(cachePath, "inselsberg.jpg")
            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                fileOutputStream?.close()
            }

            val myShareIntent = Intent(Intent.ACTION_SEND)
            myShareIntent.type = mimeType
            val uri: Uri = FileProvider.getUriForFile(application, AUTHORITY, file)
            myShareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            myShareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            myShareIntent.clipData = ClipData("Wetter vom Inselsberg", mimeTypeArray, ClipData.Item(uri))

            startActivities(activity, arrayOf(Intent.createChooser(myShareIntent, "Bild teilen")))
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    companion object {

        const val SHAREDPREFERENCES_NAME = "application"
        const val SHOW_DIALOG = "de.obscure.show.dialog"
        const val AUTHORITY = "de.obscure.webcam.fileprovider"

        fun isWinter(millis: Long): Boolean {
            val winterMonth = setOf(12, 1, 2 , 3)
            val currentMonth = DateTime(millis).monthOfYear
            return winterMonth.contains(currentMonth)
        }
    }

}