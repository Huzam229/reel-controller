package com.example.reel_controller

/**
 * A short, steady finger travel is one reel.
 * Up (smaller y) is the next reel. Down is the previous reel.
 * The return stroke is ignored until the hand is still again.
 */
class ReelGestureDetector {

    private val samples = ArrayDeque<Sample>()
    private var smoothY: Float? = null
    private var lockedUntil = 0L
    private var stillSince: Long? = null
    private var armed = true
    private var missingSince: Long? = null

    fun onHandMissing(nowMs: Long) {
        if (missingSince == null) {
            missingSince = nowMs
        }
        if (nowMs - (missingSince ?: nowMs) < missingGraceMs) {
            return
        }
        samples.clear()
        smoothY = null
        stillSince = null
        if (armed) {
            lockedUntil = 0L
        }
    }

    fun update(y: Float, nowMs: Long): String? {
        missingSince = null
        val last = smoothY
        if (last != null && kotlin.math.abs(y - last) > jumpLimit) {
            return null
        }
        val current = smooth(y)

        samples.addLast(Sample(nowMs, current))
        while (samples.size > 1 && nowMs - samples.first().timeMs > windowMs) {
            samples.removeFirst()
        }

        if (!armed) {
            if (nowMs < lockedUntil) {
                return null
            }
            val speed = if (last == null) 0f else kotlin.math.abs(current - last)
            if (speed <= stillSpeed) {
                if (stillSince == null) {
                    stillSince = nowMs
                }
                if (nowMs - (stillSince ?: nowMs) >= stillMs) {
                    armed = true
                    stillSince = null
                    samples.clear()
                    samples.addLast(Sample(nowMs, current))
                }
            } else {
                stillSince = null
            }
            return null
        }

        if (samples.size < 4) {
            return null
        }
        val oldest = samples.first()
        val elapsed = nowMs - oldest.timeMs
        if (elapsed < 70L) {
            return null
        }

        val travel = current - oldest.y
        if (kotlin.math.abs(travel) < minTravel) {
            return null
        }
        if (!directionIsSteady(travel)) {
            return null
        }

        armed = false
        lockedUntil = nowMs + lockMs
        stillSince = null
        samples.clear()
        return if (travel < 0f) "NEXT REEL" else "PREVIOUS REEL"
    }

    private fun directionIsSteady(travel: Float): Boolean {
        val sign = if (travel < 0f) -1 else 1
        var steps = 0
        var agree = 0
        var previous = samples.first().y
        for (index in 1 until samples.size) {
            val delta = samples[index].y - previous
            previous = samples[index].y
            if (kotlin.math.abs(delta) <= noise) {
                continue
            }
            steps++
            val stepSign = if (delta < 0f) -1 else 1
            if (stepSign == sign) {
                agree++
            }
        }
        return steps >= 3 && agree * 4 >= steps * 3
    }

    private fun smooth(y: Float): Float {
        val last = smoothY
        val value = if (last == null) y else last * 0.5f + y * 0.5f
        smoothY = value
        return value
    }

    private data class Sample(val timeMs: Long, val y: Float)

    private companion object {
        const val windowMs = 150L
        const val minTravel = 0.055f
        const val noise = 0.0025f
        const val jumpLimit = 0.2f
        const val lockMs = 480L
        const val stillMs = 80L
        const val stillSpeed = 0.0045f
        const val missingGraceMs = 250L
    }
}
