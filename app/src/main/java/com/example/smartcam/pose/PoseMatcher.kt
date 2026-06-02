package com.example.smartcam.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Scores how closely a live skeleton matches a template by comparing joint
 * angles. Angles are invariant to where the person stands and how big they are
 * in frame, so matching works at any distance.
 */
object PoseMatcher {

    /** (endpointA, jointVertex, endpointB) for each angle we compare. */
    private val joints: List<Triple<Int, Int, Int>> = listOf(
        Triple(PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_ELBOW, PoseLandmarks.LEFT_WRIST),
        Triple(PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_ELBOW, PoseLandmarks.RIGHT_WRIST),
        Triple(PoseLandmarks.LEFT_ELBOW, PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_HIP),
        Triple(PoseLandmarks.RIGHT_ELBOW, PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_HIP),
        Triple(PoseLandmarks.LEFT_SHOULDER, PoseLandmarks.LEFT_HIP, PoseLandmarks.LEFT_KNEE),
        Triple(PoseLandmarks.RIGHT_SHOULDER, PoseLandmarks.RIGHT_HIP, PoseLandmarks.RIGHT_KNEE),
        Triple(PoseLandmarks.LEFT_HIP, PoseLandmarks.LEFT_KNEE, PoseLandmarks.LEFT_ANKLE),
        Triple(PoseLandmarks.RIGHT_HIP, PoseLandmarks.RIGHT_KNEE, PoseLandmarks.RIGHT_ANKLE)
    )

    /** Angle differences above this (degrees) count as a total miss for that joint. */
    private const val MAX_JOINT_DIFF = 55f

    /** Returns 0..100 match score. */
    fun match(live: Map<Int, PosePoint>, template: Map<Int, PosePoint>): Int {
        var total = 0f
        var count = 0
        for ((a, b, c) in joints) {
            val liveAngle = angle(live[a], live[b], live[c]) ?: continue
            val tmplAngle = angle(template[a], template[b], template[c]) ?: continue
            val diff = abs(liveAngle - tmplAngle).coerceAtMost(MAX_JOINT_DIFF)
            total += 1f - (diff / MAX_JOINT_DIFF)
            count++
        }
        if (count == 0) return 0
        return ((total / count) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun angle(a: PosePoint?, b: PosePoint?, c: PosePoint?): Float? {
        if (a == null || b == null || c == null) return null
        val abx = a.x - b.x
        val aby = a.y - b.y
        val cbx = c.x - b.x
        val cby = c.y - b.y
        val magAb = hypot(abx, aby)
        val magCb = hypot(cbx, cby)
        if (magAb == 0f || magCb == 0f) return null
        val cos = ((abx * cbx + aby * cby) / (magAb * magCb)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }
}
