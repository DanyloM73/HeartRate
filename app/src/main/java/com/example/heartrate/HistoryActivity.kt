package com.example.heartrate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.heartrate.adapters.MeasurementAdapter
import com.example.heartrate.data.AppDatabase
import com.example.heartrate.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: MeasurementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        adapter = MeasurementAdapter(this, mutableListOf())
        binding.measurementsRv.layoutManager = LinearLayoutManager(this)
        binding.measurementsRv.adapter = adapter

        loadMeasurements()

        binding.clearBtn.setOnClickListener {
            lifecycleScope.launch {
                database.measurementDao().deleteAllMeasurements()
                adapter.clearMeasurements()
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadMeasurements() {
        lifecycleScope.launch {
            val measurements = database.measurementDao().getAllMeasurements()
            adapter = MeasurementAdapter(this@HistoryActivity, measurements.toMutableList())
            binding.measurementsRv.adapter = adapter
        }
    }
}
