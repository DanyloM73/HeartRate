package com.example.heartrate

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*


class ResultActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val progressBar = findViewById<SeekBar>(R.id.progress_bar)
        val resultTextView = findViewById<TextView>(R.id.level_text)
        val timeTextView = findViewById<TextView>(R.id.time_text)
        val dateTextView = findViewById<TextView>(R.id.date_text)
        val homeButton = findViewById<Button>(R.id.home_btn)

        val slowedDiapasonText = findViewById<TextView>(R.id.slowed_diapason_text)
        val normalDiapasonText = findViewById<TextView>(R.id.normal_diapason_text)
        val speedDiapasonText = findViewById<TextView>(R.id.speed_diapason_text)

        val number = intent.getStringExtra("bpm")?.toInt()

        if (number != null) {
            progressBar.progress = number

            progressBar.setOnTouchListener { _, _ -> true }
        }

        if (number != null) {
            resultTextView.text = when {
                number < 60 -> "Уповільнений"
                number in 60..100 -> "Звичайно"
                else -> "Прискорений"
            }

            resultTextView.setTextColor(when {
                number < 60 -> {
                    Color.parseColor("#21D7E2")
                }
                number in 60..100 -> {
                    Color.parseColor("#1FF19B")
                }
                else -> {
                    Color.parseColor("#FF4C4C")
                }
            })

            when {
                number < 60 -> {
                    slowedDiapasonText.setTextColor(Color.parseColor("#000000"))
                }
                number in 60..100 -> {
                    normalDiapasonText.setTextColor(Color.parseColor("#000000"))
                }
                number > 100 -> {
                    speedDiapasonText.setTextColor(Color.parseColor("#000000"))
                }
            }
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        timeTextView.text = currentTime
        dateTextView.text = currentDate

        homeButton.setOnClickListener{
            val intent = Intent(this@ResultActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
