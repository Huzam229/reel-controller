package com.example.reel_controller

/**
 * Point the index finger for the next reel.
 * Point the index and middle fingers together for the previous reel.
 * A fist does not scroll. It only readies the next pose.
 */
class ReelGestureDetector {

    private var armed = true
    private var lockedUntil = 0L
    private var missingSince: Long? = null
    private var indexWasDown = false
    private var indexUpSince: Long? = null
    private var twoWereDown = false
    private var twoUpSince: Long? = null
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
            twoWereDown = false
            twoUpSince = null
            waitingFor = Wait.NONE
        }
    }

    fun update(pose: FingerPose, nowMs: Long): String? {
        missingSince = null
        if (!armed) {
            if (nowMs < lockedUntil) {
                return null
            }
            val released = when (waitingFor) {
                Wait.INDEX_DOWN -> !pose.indexOnly
                Wait.TWO_DOWN -> !pose.twoFingers
                Wait.NONE -> true
            }
            if (released) {
                armed = true
                waitingFor = Wait.NONE
                indexUpSince = null
                twoUpSince = null
                if (!pose.indexOnly) {
                    indexWasDown = true
                }
                if (!pose.twoFingers) {
                    twoWereDown = true
                }
            }
            return null
        }

        if (twoFingersUp(pose, nowMs)) {
            lock(nowMs, Wait.TWO_DOWN)
            return "PREVIOUS REEL"
        }

        if (indexPointed(pose, nowMs)) {
            lock(nowMs, Wait.INDEX_DOWN)
            return "NEXT REEL"
        }
        return null
    }

    private fun twoFingersUp(pose: FingerPose, nowMs: Long): Boolean {
        if (!pose.twoFingers) {
            twoWereDown = true
            twoUpSince = null
            return false
        }
        indexUpSince = null
        if (!twoWereDown) {
            return false
        }
        if (twoUpSince == null) {
            twoUpSince = nowMs
        }
        return nowMs - (twoUpSince ?: nowMs) >= pointHoldMs
    }

    private fun indexPointed(pose: FingerPose, nowMs: Long): Boolean {
        if (!pose.indexOnly) {
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
        twoWereDown = false
        twoUpSince = null
    }

    private enum class Wait {
        NONE,
        INDEX_DOWN,
        TWO_DOWN,
    }

    private companion object {
        const val pointHoldMs = 90L
        const val lockMs = 280L
        const val missingGraceMs = 250L
    }
}

class FingerPose(
    val indexOnly: Boolean,
    val twoFingers: Boolean,
    val fist: Boolean,
)
