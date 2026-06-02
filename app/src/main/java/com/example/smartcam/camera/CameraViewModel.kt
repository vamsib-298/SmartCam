package com.example.smartcam.camera

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.smartcam.pose.CompositionScorer
import com.example.smartcam.pose.PoseCategory
import com.example.smartcam.pose.PoseMatcher
import com.example.smartcam.pose.PosePoint
import com.example.smartcam.pose.PoseTemplate
import com.example.smartcam.pose.PoseTemplates

/** Holds camera + pose UI state and drives the auto-capture decision. */
class CameraViewModel : ViewModel() {

    val templates: List<PoseTemplate> = PoseTemplates.all

    /** Pose categories that actually have templates, for the filter chips. */
    val categories: List<PoseCategory> = PoseCategory.entries.filter { cat ->
        templates.any { it.category == cat }
    }

    var selectedCategory by mutableStateOf<PoseCategory?>(null)
        private set

    /** Templates shown in the strip after applying the category filter. */
    val visibleTemplates: List<PoseTemplate>
        get() = selectedCategory?.let { c -> templates.filter { it.category == c } } ?: templates

    var selectedTemplate by mutableStateOf(templates.first())
        private set

    var liveLandmarks by mutableStateOf<Map<Int, PosePoint>?>(null)
        private set

    var matchScore by mutableStateOf(0)
        private set

    var compositionScore by mutableStateOf(0)
        private set

    var compositionHint by mutableStateOf("")
        private set

    /** Blended pose + composition quality, the headline "photo score". */
    var overallScore by mutableStateOf(0)
        private set

    var isFrontCamera by mutableStateOf(true)
        private set

    var autoCaptureEnabled by mutableStateOf(true)
        private set

    var showGrid by mutableStateOf(true)
        private set

    var flashOn by mutableStateOf(false)
        private set

    /** Self-timer in seconds; 0 = off. Cycles 0 -> 3 -> 5. */
    var timerSeconds by mutableStateOf(0)
        private set

    var lastPhotoUri by mutableStateOf<Uri?>(null)
        private set

    var statusMessage by mutableStateOf("Pick a pose and match the outline")
        private set

    /** Increments each time the app should auto-capture. UI observes this. */
    var captureRequestId by mutableStateOf(0)
        private set

    private var goodFrames = 0
    private var captureCooldownUntil = 0L

    /** Called from the pose result listener (background thread). */
    fun onPoseResult(landmarks: Map<Int, PosePoint>?) {
        liveLandmarks = landmarks

        val comp = CompositionScorer.score(landmarks)
        compositionScore = comp.score
        compositionHint = comp.hint

        if (landmarks == null) {
            matchScore = 0
            overallScore = 0
            goodFrames = 0
            statusMessage = "Step into frame"
            return
        }
        val score = PoseMatcher.match(landmarks, selectedTemplate.points)
        matchScore = score
        overallScore = ((score * 0.7f) + (compositionScore * 0.3f)).toInt()

        statusMessage = when {
            score >= MATCH_THRESHOLD -> "Perfect — hold still"
            score >= 70 -> "Almost there"
            else -> comp.hint
        }

        if (!autoCaptureEnabled) return
        val now = System.currentTimeMillis()
        if (now < captureCooldownUntil) return

        if (score >= MATCH_THRESHOLD) goodFrames++ else goodFrames = 0

        if (goodFrames >= STABLE_FRAMES) {
            goodFrames = 0
            captureCooldownUntil = now + CAPTURE_COOLDOWN_MS
            captureRequestId++
        }
    }

    fun selectTemplate(template: PoseTemplate) {
        selectedTemplate = template
        goodFrames = 0
    }

    fun selectCategory(category: PoseCategory?) {
        selectedCategory = if (selectedCategory == category) null else category
    }

    fun toggleCamera() {
        isFrontCamera = !isFrontCamera
        liveLandmarks = null
        matchScore = 0
        overallScore = 0
        goodFrames = 0
    }

    fun toggleAutoCapture() {
        autoCaptureEnabled = !autoCaptureEnabled
        goodFrames = 0
    }

    fun toggleGrid() {
        showGrid = !showGrid
    }

    fun toggleFlash() {
        flashOn = !flashOn
    }

    fun cycleTimer() {
        timerSeconds = when (timerSeconds) {
            0 -> 3
            3 -> 5
            else -> 0
        }
    }

    fun onPhotoSaved(uri: Uri?) {
        lastPhotoUri = uri
    }

    companion object {
        const val MATCH_THRESHOLD = 85
        private const val STABLE_FRAMES = 8
        private const val CAPTURE_COOLDOWN_MS = 3000L
    }
}
