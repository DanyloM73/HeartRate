package com.example.heartrate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.heartrate.data.AppDatabase
import com.example.heartrate.data.MeasurementEntity
import com.example.heartrate.databinding.ActivityResultBinding
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        val number = intent.getIntExtra("bpm", 0)

        binding.progressBar.progress = number
        binding.progressBar.setOnTouchListener { _, _ -> true }

        binding.levelText.text = when {
            number < 60 -> getString(R.string.slowed_level)
            number in 60..100 -> getString(R.string.normal_level)
            else -> getString(R.string.speed_level)
        }

        binding.levelText.setTextColor(when {
            number < 60 -> {
                "#21D7E2".toColorInt()
            }
            number in 60..100 -> {
                "#1FF19B".toColorInt()
            }
            else -> {
                "#FF4C4C".toColorInt()
            }
        })

        when {
            number < 60 -> {
                binding.slowedDiapasonText.setTextColor("#000000".toColorInt())
            }
            number in 60..100 -> {
                binding.normalDiapasonText.setTextColor("#000000".toColorInt())
            }
            else -> {
                binding.speedDiapasonText.setTextColor("#000000".toColorInt())
            }
        }

        val currentTime = SimpleDateFormat(
            "HH:mm", Locale.getDefault()
        ).format(Date())
        val currentDate = SimpleDateFormat(
            "dd/MM/yyyy", Locale.getDefault()
        ).format(Date())
        binding.timeText.text = currentTime
        binding.dateText.text = currentDate

        lifecycleScope.launch {
            database.measurementDao().insertMeasurement(
                MeasurementEntity(
                    bpm = number,
                    time = currentTime,
                    date = currentDate
                )
            )
        }

        binding.homeBtn.setOnClickListener{
            startActivity(Intent(this@ResultActivity, MainActivity::class.java))
        }

        binding.historyBtn.setOnClickListener {
            startActivity(Intent(this@ResultActivity, HistoryActivity::class.java))
        }
    }
}
