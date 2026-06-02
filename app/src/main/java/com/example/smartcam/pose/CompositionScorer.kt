package com.example.smartcam.pose

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Live composition scoring derived purely from the pose skeleton (no extra
 * model needed). Rewards a well-centered subject with good head room and a
 * level body, and produces short, actionable coaching hints.
 */
object CompositionScorer {

    data class Result(val score: Int, val hint: String)

    fun score(landmarks: Map<Int, PosePoint>?): Result {
        if (landmarks == null) return Result(0, "Step into frame")

        val leftHip = landmarks[PoseLandmarks.LEFT_HIP]
        val rightHip = landmarks[PoseLandmarks.RIGHT_HIP]
        val leftShoulder = landmarks[PoseLandmarks.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmarks.RIGHT_SHOULDER]
        val nose = landmarks[PoseLandmarks.NOSE]

        if (leftHip == null || rightHip == null || leftShoulder == null ||
            rightShoulder == null || nose == null
        ) {
            return Result(40, "Show your full body")
        }

        val centerX = (leftHip.x + rightHip.x) / 2f
        // Best when subject sits on a third line (0.33 / 0.5 / 0.66).
        val nearestThird = listOf(0.33f, 0.5f, 0.66f).minBy { abs(it - centerX) }
        val centeringPenalty = abs(nearestThird - centerX) * 180f

        val headRoom = nose.y // 0 = top of frame
        val headRoomPenalty = when {
            headRoom < 0.08f -> 35f   // too high / cropped head
            headRoom > 0.35f -> 25f   // too much head room
            else -> 0f
        }

        // Level shoulders -> straight horizon.
        val tiltPenalty = abs(leftShoulder.y - rightShoulder.y) * 220f

        val raw = 100f - centeringPenalty - headRoomPenalty - tiltPenalty
        val score = raw.roundToInt().coerceIn(0, 100)

        val hint = when {
            centeringPenalty > 18f && centerX < nearestThird -> "Move slightly right"
            centeringPenalty > 18f -> "Move slightly left"
            headRoom < 0.08f -> "Lower the camera a little"
            headRoom > 0.35f -> "Raise the camera a little"
            tiltPenalty > 18f -> "Level the camera"
            score >= 85 -> "Great framing"
            else -> "Hold steady"
        }
        return Result(score, hint)
    }
}
