package com.example.heartrate

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.camera.core.CameraControl

class HeartRateMonitor : AppCompatActivity() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        private const val TAG = "HeartRateMonitor"
        private val processing = AtomicBoolean(false)
        private lateinit var previewView: PreviewView

        private lateinit var text: TextView
        private lateinit var mainText: TextView
        private lateinit var subText: TextView
        private lateinit var heartLayout: LinearLayout
        private lateinit var progressLayout: FrameLayout
        private lateinit var hintImage: ImageView
        private lateinit var progressBar: ProgressBar
        private lateinit var progressText: TextView
        private val handler = Handler(Looper.getMainLooper())
        private var progressStatus = 0

        private var averageIndex = 0
        private const val AVERAGE_ARRAY_SIZE = 4
        private val averageArray = IntArray(AVERAGE_ARRAY_SIZE)

        enum class TYPE {
            GREEN, RED
        }

        private var currentType = TYPE.GREEN

        private var beatsIndex = 0
        private const val BEATS_ARRAY_SIZE = 3
        private val beatsArray = IntArray(BEATS_ARRAY_SIZE)
        private var beats = 0.0
        private var startTime: Long = 0

        private lateinit var cameraControl: CameraControl
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hrmonitor)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        previewView = findViewById(R.id.preview)
        text = findViewById(R.id.text)
        mainText = findViewById(R.id.mainText)
        subText = findViewById(R.id.subText)
        heartLayout = findViewById(R.id.heartLayout)
        hintImage = findViewById(R.id.hint_img)
        progressLayout = findViewById(R.id.progress_layout)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
    }

    override fun onResume() {
        super.onResume()

        startCamera()

        startTime = System.currentTimeMillis()

        progressBar.progress = 0
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

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                        analyzeImage(image)
                    }
                }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                cameraControl = camera.cameraControl
                cameraControl.enableTorch(true)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    private fun analyzeImage(image: ImageProxy) {
        if (image.format != ImageFormat.YUV_420_888) {
            image.close()
            return
        }

        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

        val imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width)
        if (imgAvg == 0 || imgAvg == 255) {
            processing.set(false)
            image.close()
            return
        }

        if (imgAvg !in 57..105) {
            runOnUiThread {
                text.text = "--"
                mainText.text = "Палець не виявлено"
                subText.text = "Щільно прикладіть палець до камери $imgAvg"
                progressLayout.visibility = View.GONE
                hintImage.visibility = View.VISIBLE
                progressBar.progress = 0
                progressStatus = 0
            }
            processing.set(false)
            image.close()
            return
        }

        var averageArrayAvg = 0
        var averageArrayCnt = 0
        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }

        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
        var newType = currentType
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != currentType) {
                beats++
                runOnUiThread {
                    animateHeartBeat()
                }
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }

        if (averageIndex == AVERAGE_ARRAY_SIZE) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        if (newType != currentType) {
            currentType = newType
        }

        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        if (totalTimeInSecs >= 10) {
            val bps = beats / totalTimeInSecs
            val dpm = (bps * 60.0).toInt()
            if (dpm < 30 || dpm > 180) {
                startTime = System.currentTimeMillis()
                beats = 0.0
                processing.set(false)
                image.close()
                return
            }

            if (beatsIndex == BEATS_ARRAY_SIZE) beatsIndex = 0
            beatsArray[beatsIndex] = dpm
            beatsIndex++

            var beatsArrayAvg = 0
            var beatsArrayCnt = 0
            for (i in beatsArray.indices) {
                if (beatsArray[i] > 0) {
                    beatsArrayAvg += beatsArray[i]
                    beatsArrayCnt++
                }
            }
            val beatsAvg = beatsArrayAvg / beatsArrayCnt
            runOnUiThread {
                text.text = beatsAvg.toString()
            }
            startTime = System.currentTimeMillis()
            beats = 0.0
        }
        processing.set(false)
        image.close()
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
        heartLayout.startAnimation(scaleAnimation)
    }

    private val progressRunnable = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (progressStatus <= 100) {
                mainText.text = "Йде вимірювання"
                subText.text = "Визначаємо ваш пульс. Утримуйте!"
                progressLayout.visibility = View.VISIBLE
                hintImage.visibility = View.GONE
                progressBar.progress = progressStatus
                progressText.text = "$progressStatus%"
                progressStatus += 2
                handler.postDelayed(this, 750)
            } else {
                val intent = Intent(this@HeartRateMonitor, ResultActivity::class.java)
                intent.putExtra("bpm", text.text.toString())
                startActivity(intent)
                handler.removeCallbacks(this)
            }
        }
    }
}
