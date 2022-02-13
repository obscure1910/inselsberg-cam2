package de.obscure.webcam.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.obscure.webcam.database.WeatherStatisticDao

class SharedViewmodelFactory(
    private val statisticDao: WeatherStatisticDao,
    val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewmodel::class.java)) {
            return SharedViewmodel(statisticDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}