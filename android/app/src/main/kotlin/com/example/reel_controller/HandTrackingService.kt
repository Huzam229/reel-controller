package com.example.reel_controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HandTrackingService : LifecycleService() {

    private val detector = ReelGestureDetector()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)
    private val swipeInFlight = AtomicBoolean(false)
    private var landmarker: HandLandmarker? = null
    private var pendingImage: MPImage? = null
    private var statusHoldUntil = 0L
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastTimestamp = 0L
    private var nativeReady = false
    private var modelBuffer: ByteBuffer? = null

    override fun onCreate() {
        super.onCreate()
        // Show the notification before loading the hand model. On a phone,
        // that load can take longer than Android allows, and the app is
        // then closed and restarted.
        if (!startAsForeground()) {
            nativeReady = false
            return
        }
        try {
            landmarker = createLandmarker()
            nativeReady = true
        } catch (error: Throwable) {
            Log.e(tag, "Hand tracking failed to start", error)
            status = failureText(error)
            nativeReady = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!nativeReady) {
            return Service.START_NOT_STICKY
        }
        startCamera()
        status = "Watching your hand"
        return Service.START_STICKY
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        landmarker?.close()
        analysisExecutor.shutdown()
        if (nativeReady) {
            status = "Stopped"
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun failureText(error: Throwable): String {
        val parts = ArrayList<String>()
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 4) {
            val message = current.message?.lineSequence()?.firstOrNull()?.trim().orEmpty()
            val piece = if (message.isEmpty()) current.javaClass.simpleName else message
            if (parts.lastOrNull() != piece) {
                parts.add(piece)
            }
            current = current.cause
            depth++
        }
        return parts.joinToString(" / ").take(140).ifEmpty { "Hand tracking failed to start" }
    }

    private fun createLandmarker(): HandLandmarker {
        val bytes = assets.open("hand_landmarker.task").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        modelBuffer = buffer

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetBuffer(buffer)
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setResultListener { result, image ->
                val now = SystemClock.uptimeMillis()
                val hand = result.landmarks().firstOrNull()
                if (hand == null || hand.size <= pinkyTip) {
                    detector.onHandMissing(now)
                    if (now >= statusHoldUntil) {
                        status = "Show your hand"
                    }
                } else {
                    val pose = fingerPose(hand)
                    val action = detector.update(pose, now)
                    if (action != null) {
                        performReel(action)
                    } else if (now >= statusHoldUntil) {
                        status = when {
                            pose.fist -> "Fist"
                            pose.indexExtended -> "Index"
                            else -> "Hand seen"
                        }
                    }
                }
                image.close()
                pendingImage = null
                busy.set(false)
            }
            .setErrorListener { error ->
                Log.e(tag, "Hand landmarker error", error)
                pendingImage?.close()
                pendingImage = null
                busy.set(false)
            }
            .build()

        return HandLandmarker.createFromOptions(this, options)
    }

    private fun fingerPose(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): FingerPose {
        fun distance(first: Int, second: Int): Float {
            val dx = hand[first].x() - hand[second].x()
            val dy = hand[first].y() - hand[second].y()
            return kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }

        val palm = distance(wrist, middleMcp).coerceAtLeast(0.05f)
        val indexExtended = hand[indexMcp].y() - hand[indexTip].y() > palm * 0.28f
        val fingersCurled = hand[middleTip].y() > hand[middlePip].y() &&
            hand[ringTip].y() > hand[ringPip].y() &&
            hand[pinkyTip].y() > hand[pinkyPip].y()
        return FingerPose(
            indexExtended = indexExtended,
            thumbFold = distance(thumbTip, indexMcp) / palm,
            fist = fingersCurled && !indexExtended,
        )
    }

    @Suppress("DEPRECATION")
    private fun startCamera() {
        val cameraSelector = preferredCameraSelector()
        if (cameraSelector == null) {
            status = "No camera found"
            return
        }

        try {
            ProcessCameraProvider.configureInstance(
                CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                    .setAvailableCamerasLimiter(cameraSelector)
                    .build()
            )
        } catch (error: IllegalStateException) {
            Log.i(tag, "CameraX was already configured")
        }

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(android.util.Size(640, 480))
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                analyze(imageProxy)
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    analysis
                )
            } catch (error: Exception) {
                Log.e(tag, "Camera failed to start", error)
                status = "Camera failed to start"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun preferredCameraSelector(): CameraSelector? {
        val manager = getSystemService(CameraManager::class.java) ?: return null
        val facings = manager.cameraIdList.mapNotNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING)
        }
        return when {
            facings.contains(CameraCharacteristics.LENS_FACING_FRONT) ->
                CameraSelector.DEFAULT_FRONT_CAMERA
            facings.contains(CameraCharacteristics.LENS_FACING_BACK) ->
                CameraSelector.DEFAULT_BACK_CAMERA
            else -> null
        }
    }

    private fun analyze(imageProxy: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = uprightBitmap(imageProxy)
            val mpImage = BitmapImageBuilder(bitmap).build()
            pendingImage = mpImage
            var timestamp = SystemClock.uptimeMillis()
            if (timestamp <= lastTimestamp) {
                timestamp = lastTimestamp + 1
            }
            lastTimestamp = timestamp
            landmarker?.detectAsync(mpImage, timestamp)
        } catch (error: Exception) {
            Log.e(tag, "Could not read camera frame", error)
            pendingImage?.close()
            pendingImage = null
            busy.set(false)
        } finally {
            imageProxy.close()
        }
    }

    private fun uprightBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes[0]
        val width = imageProxy.width
        val height = imageProxy.height
        val pixelStride = plane.pixelStride.coerceAtLeast(1)
        val rowStride = plane.rowStride
        val buffer = plane.buffer
        buffer.rewind()

        val rowWidth = rowStride / pixelStride
        val padded = Bitmap.createBitmap(rowWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = if (rowWidth == width) {
            padded
        } else {
            Bitmap.createBitmap(padded, 0, 0, width, height)
        }

        val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
        if (rotation == 0f) {
            return cropped
        }

        val matrix = Matrix()
        matrix.postRotate(rotation)
        return Bitmap.createBitmap(
            cropped,
            0,
            0,
            cropped.width,
            cropped.height,
            matrix,
            true
        )
    }

    private fun performReel(action: String) {
        if (!swipeInFlight.compareAndSet(false, true)) {
            return
        }
        status = if (action == "NEXT REEL") "Next reel" else "Previous reel"
        statusHoldUntil = SystemClock.uptimeMillis() + 1500
        Log.i(tag, action)
        mainHandler.post {
            val service = GestureAccessibilityService.instance
            if (service == null) {
                swipeInFlight.set(false)
                status = "Turn on Reel Controller in Accessibility"
                return@post
            }
            val done: (Boolean) -> Unit = { swipeInFlight.set(false) }
            if (action == "NEXT REEL") {
                service.swipeUp(done)
            } else {
                service.swipeDown(done)
            }
        }
    }

    private fun startAsForeground(): Boolean {
        val channelId = "hand_tracking"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hand tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Reel Controller")
            .setContentText("Watching your index finger")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(notificationId, notification)
            }
            true
        } catch (error: Exception) {
            Log.e(tag, "Could not keep hand tracking running", error)
            status = "Allow the camera, then reopen the app"
            stopSelf()
            false
        }
    }

    companion object {
        const val tag = "HandTrackingService"
        const val notificationId = 42
        const val indexTip = 8
        const val indexPip = 6
        const val indexMcp = 5
        const val thumbTip = 4
        const val wrist = 0
        const val middleMcp = 9
        const val middlePip = 10
        const val middleTip = 12
        const val ringPip = 14
        const val ringTip = 16
        const val pinkyPip = 18
        const val pinkyTip = 20

        @Volatile
        var status: String = "Starting"
    }
}
