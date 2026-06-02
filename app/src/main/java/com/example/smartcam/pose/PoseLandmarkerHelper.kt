package com.example.smartcam.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Wraps the MediaPipe Pose Landmarker for live-stream camera frames.
 *
 * Requires the model file at app/src/main/assets/pose_landmarker_lite.task
 */
class PoseLandmarkerHelper(
    context: Context,
    private val onPose: (landmarks: Map<Int, PosePoint>?) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var landmarker: PoseLandmarker? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ -> onPose(toPointMap(result)) }
                .setErrorListener { e -> onError(e.message ?: "Pose detection error") }
                .build()
            landmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            onError("Could not load pose model. Add pose_landmarker_lite.task to assets. (${e.message})")
        }
    }

    /** Feeds one camera frame. Always closes the [imageProxy]. */
    fun detect(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val detector = landmarker
        if (detector == null) {
            imageProxy.close()
            return
        }
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val source = imageProxy.toBitmap()
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                if (isFrontCamera) postScale(-1f, 1f)
            }
            val upright = Bitmap.createBitmap(
                source, 0, 0, source.width, source.height, matrix, true
            )
            val mpImage = BitmapImageBuilder(upright).build()
            detector.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            onError(e.message ?: "Frame processing error")
        }
    }

    fun close() {
        landmarker?.close()
        landmarker = null
    }

    private fun toPointMap(result: PoseLandmarkerResult): Map<Int, PosePoint>? {
        val poses = result.landmarks()
        if (poses.isEmpty()) return null
        val first = poses[0]
        val map = HashMap<Int, PosePoint>(first.size)
        first.forEachIndexed { index, lm -> map[index] = PosePoint(lm.x(), lm.y()) }
        return map
    }

    companion object {
        const val MODEL_PATH = "pose_landmarker_lite.task"
    }
}
