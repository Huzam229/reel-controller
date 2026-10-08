package com.example.reel_controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        var instance: GestureAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun swipeUp(onDone: (Boolean) -> Unit) {
        swipe(0.82f, 0.16f, 0, onDone)
    }

    fun swipeDown(onDone: (Boolean) -> Unit) {
        swipe(0.18f, 0.82f, 0, onDone)
    }

    private fun swipe(
        startFraction: Float,
        endFraction: Float,
        attempt: Int,
        onDone: (Boolean) -> Unit
    ) {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * startFraction
        val endY = metrics.heightPixels * endFraction

        val path = Path()
        path.moveTo(x, startY)
        path.lineTo(x, endY)

        val stroke = GestureDescription.StrokeDescription(
            path,
            0,
            180
        )

        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        var finished = false

        fun finish(completed: Boolean) {
            if (finished) {
                return
            }
            finished = true
            onDone(completed)
        }

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    finish(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (attempt == 0 && !finished) {
                        finished = true
                        handler.post {
                            swipe(startFraction, endFraction, 1, onDone)
                        }
                    } else {
                        finish(false)
                    }
                }
            },
            null
        )

        if (!dispatched) {
            finish(false)
        }
    }
}
