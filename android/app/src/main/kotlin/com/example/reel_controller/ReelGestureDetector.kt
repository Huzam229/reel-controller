package com.example.reel_controller

/**
 * Point the index finger for the next reel.
 * Open the thumb out of a fist for the previous reel.
 * Each pose fires once, then the finger has to return.
 */
class ReelGestureDetector {

    private var armed = true
    private var lockedUntil = 0L
    private var missingSince: Long? = null
    private var indexWasDown = false
    private var indexUpSince: Long? = null
    private var fistSince: Long? = null
    private var thumbReady = false
    private var closedThumb = 1f
    private var smoothThumb = 1f
    private var waitingFor = Wait.NONE

    fun onHandMissing(nowMs: Long) {
        if (missingSince == null) {
            missingSince = nowMs
        }
        if (nowMs - (missingSince ?: nowMs) < missingGraceMs) {
            return
        }
        if (armed) {
            indexWasDown = false
            indexUpSince = null
            fistSince = null
            thumbReady = false
            waitingFor = Wait.NONE
        }
    }

    fun update(pose: FingerPose, nowMs: Long): String? {
        missingSince = null
        smoothThumb = smoothThumb * 0.55f + pose.thumbFold * 0.45f

        if (!armed) {
            if (nowMs < lockedUntil) {
                return null
            }
            val released = when (waitingFor) {
                Wait.INDEX_DOWN -> !pose.indexExtended
                Wait.THUMB_CLOSED -> pose.fist && smoothThumb < closedThumb + 0.12f
                Wait.NONE -> true
            }
            if (released) {
                armed = true
                waitingFor = Wait.NONE
                indexUpSince = null
                if (!pose.indexExtended) {
                    indexWasDown = true
                }
            }
            return null
        }

        if (thumbOpened(pose, nowMs)) {
            lock(nowMs, Wait.THUMB_CLOSED)
            return "PREVIOUS REEL"
        }

        if (indexPointed(pose, nowMs)) {
            lock(nowMs, Wait.INDEX_DOWN)
            return "NEXT REEL"
        }
        return null
    }

    private fun thumbOpened(pose: FingerPose, nowMs: Long): Boolean {
        if (!pose.fist) {
            fistSince = null
            return false
        }
        if (fistSince == null) {
            fistSince = nowMs
            closedThumb = smoothThumb
        } else if (!thumbReady) {
            closedThumb = closedThumb * 0.8f + smoothThumb * 0.2f
        }
        if (nowMs - (fistSince ?: nowMs) >= fistHoldMs) {
            thumbReady = true
        }
        return thumbReady && smoothThumb > closedThumb + thumbOpenTravel
    }

    private fun indexPointed(pose: FingerPose, nowMs: Long): Boolean {
        if (!pose.indexExtended) {
            indexWasDown = true
            indexUpSince = null
            return false
        }
        if (!indexWasDown) {
            return false
        }
        if (indexUpSince == null) {
            indexUpSince = nowMs
        }
        return nowMs - (indexUpSince ?: nowMs) >= pointHoldMs
    }

    private fun lock(nowMs: Long, until: Wait) {
        armed = false
        lockedUntil = nowMs + lockMs
        waitingFor = until
        indexWasDown = false
        indexUpSince = null
        thumbReady = false
        fistSince = null
    }

    private enum class Wait {
        NONE,
        INDEX_DOWN,
        THUMB_CLOSED,
    }

    private companion object {
        const val thumbOpenTravel = 0.28f
        const val fistHoldMs = 80L
        const val pointHoldMs = 90L
        const val lockMs = 280L
        const val missingGraceMs = 250L
    }
}

class FingerPose(
    val indexExtended: Boolean,
    val thumbFold: Float,
    val fist: Boolean,
)
