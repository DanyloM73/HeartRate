package com.example.heartrate

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.example.heartrate.databinding.ActivityLoadBinding

class LoadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        val totalTime = 4000L
        val interval = 40L
        val steps = (totalTime / interval).toInt()
        var progress = 0

        binding.progressBar.max = steps

        object : CountDownTimer(totalTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                progress++
                binding.progressBar.progress = progress
                val percent = (progress * 100 / steps)
                binding.progressText.text = getString(R.string.progress, percent)
            }

            override fun onFinish() {
                binding.progressBar.progress = steps
                startActivity(Intent(this@LoadActivity, MainActivity::class.java))
                finish()
            }
        }.start()
    }
}
