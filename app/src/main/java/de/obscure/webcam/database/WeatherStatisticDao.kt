package de.obscure.webcam.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import de.obscure.webcam.entity.WeatherStatistic

@Dao
interface WeatherStatisticDao {

    @Transaction
    fun updateAll(weatherStatistic: List<WeatherStatistic>) {
        deleteAll()
        insertAll(weatherStatistic)
    }

    @Insert(onConflict = REPLACE)
    fun insertAll(weatherStatistics: List<WeatherStatistic>)

    @Query("DELETE FROM WeatherStatistic")
    fun deleteAll()

    @Query("SELECT * FROM WeatherStatistic ORDER BY timestamp ASC")
    fun getAll(): LiveData<List<WeatherStatistic>>

    @Query("SELECT * FROM WeatherStatistic WHERE timestamp IN (SELECT MAX(timestamp) FROM WeatherStatistic)")
    fun getLatest(): WeatherStatistic?

}