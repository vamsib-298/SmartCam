package com.example.smartcam.pose

import com.example.smartcam.pose.PoseLandmarks.LEFT_ANKLE
import com.example.smartcam.pose.PoseLandmarks.LEFT_ELBOW
import com.example.smartcam.pose.PoseLandmarks.LEFT_HIP
import com.example.smartcam.pose.PoseLandmarks.LEFT_KNEE
import com.example.smartcam.pose.PoseLandmarks.LEFT_SHOULDER
import com.example.smartcam.pose.PoseLandmarks.LEFT_WRIST
import com.example.smartcam.pose.PoseLandmarks.NOSE
import com.example.smartcam.pose.PoseLandmarks.RIGHT_ANKLE
import com.example.smartcam.pose.PoseLandmarks.RIGHT_ELBOW
import com.example.smartcam.pose.PoseLandmarks.RIGHT_HIP
import com.example.smartcam.pose.PoseLandmarks.RIGHT_KNEE
import com.example.smartcam.pose.PoseLandmarks.RIGHT_SHOULDER
import com.example.smartcam.pose.PoseLandmarks.RIGHT_WRIST

/**
 * Built-in starter pose templates. In later phases these get replaced/augmented
 * by a downloadable, scene-mapped pose dataset (the real product moat).
 */
object PoseTemplates {

    private fun p(x: Float, y: Float) = PosePoint(x, y)

    // Shared lower body for upright standing poses.
    private val standingLowerBody = mapOf(
        LEFT_HIP to p(0.55f, 0.55f),
        RIGHT_HIP to p(0.45f, 0.55f),
        LEFT_KNEE to p(0.55f, 0.77f),
        RIGHT_KNEE to p(0.45f, 0.77f),
        LEFT_ANKLE to p(0.55f, 0.95f),
        RIGHT_ANKLE to p(0.45f, 0.95f)
    )

    private val handOnHip = PoseTemplate(
        id = "hand_on_hip",
        name = "Hand on Hip",
        emoji = "💃",
        category = PoseCategory.TRENDING,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.60f, 0.42f),
            LEFT_WRIST to p(0.60f, 0.55f),
            RIGHT_ELBOW to p(0.39f, 0.41f),
            RIGHT_WRIST to p(0.47f, 0.53f)
        )
    )

    private val waveHello = PoseTemplate(
        id = "wave_hello",
        name = "Wave Hello",
        emoji = "👋",
        category = PoseCategory.FUN,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.62f, 0.42f),
            LEFT_WRIST to p(0.63f, 0.55f),
            RIGHT_ELBOW to p(0.40f, 0.16f),
            RIGHT_WRIST to p(0.42f, 0.04f)
        )
    )

    private val handsUp = PoseTemplate(
        id = "hands_up",
        name = "Hands Up",
        emoji = "🙌",
        category = PoseCategory.TRAVEL,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.64f, 0.16f),
            LEFT_WRIST to p(0.66f, 0.04f),
            RIGHT_ELBOW to p(0.36f, 0.16f),
            RIGHT_WRIST to p(0.34f, 0.04f)
        )
    )

    private val armsCrossed = PoseTemplate(
        id = "arms_crossed",
        name = "Arms Crossed",
        emoji = "😎",
        category = PoseCategory.FASHION,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.62f, 0.44f),
            LEFT_WRIST to p(0.44f, 0.40f),
            RIGHT_ELBOW to p(0.38f, 0.44f),
            RIGHT_WRIST to p(0.56f, 0.40f)
        )
    )

    private val peaceSign = PoseTemplate(
        id = "peace_sign",
        name = "Peace Sign",
        emoji = "✌️",
        category = PoseCategory.PORTRAIT,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.60f, 0.42f),
            LEFT_WRIST to p(0.60f, 0.55f),
            RIGHT_ELBOW to p(0.40f, 0.34f),
            RIGHT_WRIST to p(0.45f, 0.17f)
        )
    )

    private val bothHandsOnHips = PoseTemplate(
        id = "both_hands_hips",
        name = "Power Pose",
        emoji = "🦸",
        category = PoseCategory.TRENDING,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.64f, 0.42f),
            LEFT_WRIST to p(0.56f, 0.55f),
            RIGHT_ELBOW to p(0.36f, 0.42f),
            RIGHT_WRIST to p(0.44f, 0.55f)
        )
    )

    private val pointAway = PoseTemplate(
        id = "point_away",
        name = "Point Away",
        emoji = "🧭",
        category = PoseCategory.TRAVEL,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.66f, 0.30f),
            LEFT_WRIST to p(0.78f, 0.26f),
            RIGHT_ELBOW to p(0.40f, 0.42f),
            RIGHT_WRIST to p(0.42f, 0.54f)
        )
    )

    private val handInHair = PoseTemplate(
        id = "hand_in_hair",
        name = "Hair Flip",
        emoji = "💇",
        category = PoseCategory.FASHION,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.60f, 0.20f),
            LEFT_WRIST to p(0.54f, 0.08f),
            RIGHT_ELBOW to p(0.40f, 0.42f),
            RIGHT_WRIST to p(0.42f, 0.54f)
        )
    )

    private val chinRest = PoseTemplate(
        id = "chin_rest",
        name = "Thinker",
        emoji = "🤔",
        category = PoseCategory.PORTRAIT,
        points = standingLowerBody + mapOf(
            NOSE to p(0.50f, 0.13f),
            LEFT_SHOULDER to p(0.58f, 0.28f),
            RIGHT_SHOULDER to p(0.42f, 0.28f),
            LEFT_ELBOW to p(0.58f, 0.40f),
            LEFT_WRIST to p(0.52f, 0.20f),
            RIGHT_ELBOW to p(0.40f, 0.42f),
            RIGHT_WRIST to p(0.42f, 0.54f)
        )
    )

    val all: List<PoseTemplate> = listOf(
        handOnHip, bothHandsOnHips, waveHello, handsUp, pointAway,
        armsCrossed, handInHair, peaceSign, chinRest
    )
}
