package com.example.heartrate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bpm: Int,
    val time: String,
    val date: String
)