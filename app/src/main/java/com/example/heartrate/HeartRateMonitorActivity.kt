package com.example.heartrate

import android.content.Intent
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.camera.core.CameraControl
import com.example.heartrate.databinding.ActivityHrmonitorBinding
import com.example.heartrate.utils.ImageProcessing

class HeartRateMonitorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHrmonitorBinding
    private lateinit var cameraControl: CameraControl

    private val handler = Handler(Looper.getMainLooper())
    private var progressStatus = 0
    private var averageIndex = 0
    private val averageArray = IntArray(4)
    var currentPhase = PulsePhase.NO_BEAT
    private var beatsIndex = 0
    private val beatsArray = IntArray(3)
    private var beats = 0.0
    private var startTime: Long = 0

    enum class PulsePhase {
        BEAT_DETECTED,
        NO_BEAT
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHrmonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        startCamera()
        startTime = System.currentTimeMillis()
        binding.progressBar.progress = 0
        progressStatus = 0
        handler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(progressRunnable)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        try {
            val preview = buildPreview()
            val imageAnalyzer = buildImageAnalyzer()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            cameraControl = camera.cameraControl
            cameraControl.enableTorch(true)
        } catch (e: Exception) {
            Log.e("HeartRateMonitor", "Camera binding failed", e)
        }
    }

    private fun buildPreview(): Preview {
        return Preview.Builder()
            .build()
            .also { it.surfaceProvider = binding.preview.surfaceProvider }
    }

    private fun buildImageAnalyzer(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                    analyzeImage(image)
                }
            }
    }

    private fun analyzeImage(image: ImageProxy) {
        if (!isValidImage(image)) {
            image.close()
            return
        }

        val imgAvg = getImageAverage(image)
        if (!isValidPulse(imgAvg)) {
            resetUi()
            image.close()
            return
        }

        updateAverageArray(imgAvg)
        detectHeartbeat(imgAvg)
        calculateBpm()

        image.close()
    }

    private fun isValidImage(image: ImageProxy): Boolean {
        return image.format == ImageFormat.YUV_420_888
    }

    private fun getImageAverage(image: ImageProxy): Int {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return ImageProcessing.decodeYUV420SPtoRedAvg(
            data.clone(), image.height, image.width
        )
    }

    private fun isValidPulse(imgAvg: Int): Boolean {
        return imgAvg in 57..105
    }

    private fun resetUi() {
        runOnUiThread {
            binding.text.text = getString(R.string.bpm_value_default)
            binding.mainText.text = getString(R.string.main_text_default)
            binding.subText.text = getString(R.string.sub_text_default)
            binding.progressLayout.visibility = View.GONE
            binding.hintImg.visibility = View.VISIBLE
            binding.progressBar.progress = 0
            progressStatus = 0
        }
    }

    private fun updateAverageArray(imgAvg: Int) {
        averageArray[averageIndex % averageArray.size] = imgAvg
        averageIndex++
    }

    private fun detectHeartbeat(imgAvg: Int) {
        val rollingAverage = averageArray.filter { it > 0 }.average().toInt()
        val newPhase =
            if (imgAvg < rollingAverage) PulsePhase.BEAT_DETECTED
            else PulsePhase.NO_BEAT

        if (newPhase == PulsePhase.BEAT_DETECTED && newPhase != currentPhase) {
            beats++
            runOnUiThread { animateHeartBeat() }
        }
        currentPhase = newPhase
    }

    private fun calculateBpm() {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        if (elapsedTime < 10) return

        val bps = beats / elapsedTime
        val bpm = (bps * 60).toInt()

        if (bpm in 30..180) {
            beatsArray[beatsIndex % beatsArray.size] = bpm
            beatsIndex++

            val avgBpm = beatsArray.filter { it > 0 }.average().toInt()
            runOnUiThread { binding.text.text = avgBpm.toString() }
        }

        startTime = System.currentTimeMillis()
        beats = 0.0
    }

    private fun animateHeartBeat() {
        val scaleAnimation = ScaleAnimation(
            1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 200
        scaleAnimation.repeatCount = 1
        scaleAnimation.repeatMode = Animation.REVERSE
        binding.heartLayout.startAnimation(scaleAnimation)
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (progressStatus <= 100) {
                updateProgressUI()
                progressStatus += 1
                handler.postDelayed(this, 500)
            } else {
                navigateToResultScreen()
                handler.removeCallbacks(this)
            }
        }
    }

    private fun updateProgressUI() {
        binding.mainText.text = getString(R.string.main_text_active)
        binding.subText.text = getString(R.string.sub_text_active)
        binding.progressLayout.visibility = View.VISIBLE
        binding.hintImg.visibility = View.GONE
        binding.progressBar.progress = progressStatus
        binding.progressText.text = getString(R.string.progress, progressStatus)
    }

    private fun navigateToResultScreen() {
        var bpm = binding.text.text.toString()
        if (bpm == "--") bpm = "0"
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("bpm", bpm.toInt())
        startActivity(intent)
        finish()
    }
}
