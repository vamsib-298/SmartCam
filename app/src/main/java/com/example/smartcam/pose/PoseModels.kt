package com.example.smartcam.pose

/**
 * MediaPipe Pose landmark indices we use for matching and drawing.
 * Full model returns 33 landmarks; we work with the body skeleton subset.
 */
object PoseLandmarks {
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28

    /** Bone connections drawn as the skeleton/silhouette. */
    val BONES: List<Pair<Int, Int>> = listOf(
        LEFT_SHOULDER to RIGHT_SHOULDER,
        LEFT_SHOULDER to LEFT_ELBOW,
        LEFT_ELBOW to LEFT_WRIST,
        RIGHT_SHOULDER to RIGHT_ELBOW,
        RIGHT_ELBOW to RIGHT_WRIST,
        LEFT_SHOULDER to LEFT_HIP,
        RIGHT_SHOULDER to RIGHT_HIP,
        LEFT_HIP to RIGHT_HIP,
        LEFT_HIP to LEFT_KNEE,
        LEFT_KNEE to LEFT_ANKLE,
        RIGHT_HIP to RIGHT_KNEE,
        RIGHT_KNEE to RIGHT_ANKLE
    )
}

/** A normalized 2D point (x, y in 0..1, y pointing down like image coordinates). */
data class PosePoint(val x: Float, val y: Float)

/**
 * A pose template the user tries to imitate. Coordinates are normalized so the
 * silhouette scales to any body size or camera distance.
 */
data class PoseTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val category: PoseCategory,
    val points: Map<Int, PosePoint>
)

enum class PoseCategory(val label: String) {
    TRENDING("🔥 Trending"),
    PORTRAIT("📷 Portrait"),
    TRAVEL("✈️ Travel"),
    FASHION("👗 Fashion"),
    FUN("😎 Fun")
}
