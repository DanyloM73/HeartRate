package com.example.heartrate

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: ImageButton = findViewById(R.id.start_button)

        startButton.setOnClickListener{
            val intent = Intent(this@MainActivity, HeartRateMonitor::class.java)
            startActivity(intent)
        }

    }
}