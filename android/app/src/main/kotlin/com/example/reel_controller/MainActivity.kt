package com.example.reel_controller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "reel_controller/accessibility"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->

            when (call.method) {

                "swipeUp" -> performSwipe(result, GestureAccessibilityService::swipeUp)

                "swipeDown" -> performSwipe(result, GestureAccessibilityService::swipeDown)

                "startTracking" -> startTracking(result)

                "stopTracking" -> {
                    stopService(Intent(this, HandTrackingService::class.java))
                    result.success(true)
                }

                "trackingStatus" -> result.success(HandTrackingService.status)

                else -> result.notImplemented()
            }
        }
    }

    private var trackingResult: MethodChannel.Result? = null

    private fun startTracking(result: MethodChannel.Result) {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraGranted) {
            HandTrackingService.status = "Allow the camera"
            trackingResult = result
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraRequest)
            return
        }

        startHandTracking()
        result.success(true)
    }

    private fun startHandTracking() {
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, HandTrackingService::class.java)
            )
        } catch (error: Exception) {
            HandTrackingService.status = "Could not start the camera"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != cameraRequest) {
            return
        }

        val result = trackingResult
        trackingResult = null

        val cameraGranted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (cameraGranted) {
            startHandTracking()
            result?.success(true)
        } else {
            HandTrackingService.status =
                "Open Settings and allow the camera"
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
            result?.error(
                "PERMISSION",
                "Open Settings and allow the camera",
                null
            )
        }
    }

    private fun performSwipe(
        result: MethodChannel.Result,
        swipe: GestureAccessibilityService.( (Boolean) -> Unit ) -> Unit
    ) {

        val service = GestureAccessibilityService.instance

        if (service == null) {
            result.error(
                "SERVICE_DISABLED",
                "Turn on Reel Controller in Settings, then Accessibility",
                null
            )
            return
        }

        service.swipe { completed ->
            if (completed) {
                result.success(true)
            } else {
                result.error(
                    "SWIPE_CANCELLED",
                    "Android cancelled the swipe",
                    null
                )
            }
        }
    }

    private companion object {
        const val cameraRequest = 4101
    }
}