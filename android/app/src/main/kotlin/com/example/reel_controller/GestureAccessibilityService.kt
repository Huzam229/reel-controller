package com.example.reel_controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GestureAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Nothing here yet
    }

    override fun onInterrupt() {
        // Required
    }

    override fun onDestroy() {
        instance = null

        super.onDestroy()
    }

    fun swipeUp(onDone: (Boolean) -> Unit) {
        // Finger moves up: next reel.
        swipe(0.75f, 0.30f, onDone)
    }

    fun swipeDown(onDone: (Boolean) -> Unit) {
        // Finger moves down: previous reel.
        swipe(0.30f, 0.75f, onDone)
    }

    private fun swipe(
        startFraction: Float,
        endFraction: Float,
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
            250
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
                    finish(false)
                }
            },
            null
        )

        if (!dispatched) {
            finish(false)
        }
    }
}