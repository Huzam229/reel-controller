package com.example.reel_controller

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

                else -> result.notImplemented()
            }
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
}