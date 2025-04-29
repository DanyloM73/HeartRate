package com.example.heartrate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY id DESC")
    suspend fun getAllMeasurements(): List<MeasurementEntity>

    @Query("DELETE FROM measurements")
    suspend fun deleteAllMeasurements()
}