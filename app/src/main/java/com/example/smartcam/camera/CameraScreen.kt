package com.example.smartcam.camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartcam.pose.PoseCategory
import com.example.smartcam.pose.PoseLandmarkerHelper
import com.example.smartcam.pose.PoseTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var countdown by remember { mutableIntStateOf(0) }

    val poseHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            onPose = { landmarks -> viewModel.onPoseResult(landmarks) },
            onError = { msg ->
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Apply flash mode whenever it changes.
    LaunchedEffect(viewModel.flashOn) {
        imageCapture.flashMode =
            if (viewModel.flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    DisposableEffect(Unit) {
        onDispose {
            poseHelper.close()
            analysisExecutor.shutdown()
        }
    }

    // (Re)bind camera whenever the lens direction changes.
    LaunchedEffect(viewModel.isFrontCamera) {
        bindCamera(
            context = context,
            previewView = previewView,
            imageCapture = imageCapture,
            isFrontCamera = viewModel.isFrontCamera,
            analysisExecutor = analysisExecutor,
            onFrame = { proxy -> poseHelper.detect(proxy, viewModel.isFrontCamera) },
            lifecycleOwner = lifecycleOwner
        )
    }

    fun shoot() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        takePhoto(context, imageCapture, analysisExecutor) { uri -> viewModel.onPhotoSaved(uri) }
    }

    fun startCapture() {
        val seconds = viewModel.timerSeconds
        if (seconds <= 0) {
            shoot()
            return
        }
        scope.launch {
            countdown = seconds
            while (countdown > 0) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(1000)
                countdown--
            }
            shoot()
        }
    }

    // Auto-capture trigger.
    LaunchedEffect(viewModel.captureRequestId) {
        if (viewModel.captureRequestId > 0) {
            shoot()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (viewModel.showGrid) {
            CompositionGrid(modifier = Modifier.fillMaxSize())
        }

        PoseOverlay(
            template = viewModel.selectedTemplate.points,
            live = viewModel.liveLandmarks,
            matched = viewModel.matchScore >= CameraViewModel.MATCH_THRESHOLD,
            modifier = Modifier.fillMaxSize()
        )

        // Top: score panel + category filter + trending pose cards.
        Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 16.dp)) {
            ScorePanel(
                overall = viewModel.overallScore,
                pose = viewModel.matchScore,
                composition = viewModel.compositionScore,
                status = viewModel.statusMessage
            )
            Spacer(Modifier.height(10.dp))
            CategoryChips(
                categories = viewModel.categories,
                selected = viewModel.selectedCategory,
                onSelect = viewModel::selectCategory
            )
            Spacer(Modifier.height(8.dp))
            PoseCardStrip(
                templates = viewModel.visibleTemplates,
                selectedId = viewModel.selectedTemplate.id,
                onSelect = viewModel::selectTemplate
            )
        }

        // Right-side quick toggles: flash, timer, grid.
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleChip(active = viewModel.flashOn, label = "\u26A1", onClick = viewModel::toggleFlash)
            ToggleChip(
                active = viewModel.timerSeconds > 0,
                label = if (viewModel.timerSeconds > 0) "${viewModel.timerSeconds}s" else "\u23F1",
                onClick = viewModel::cycleTimer
            )
            ToggleChip(active = viewModel.showGrid, label = "\u29C9", onClick = viewModel::toggleGrid)
        }

        // Big countdown number.
        if (countdown > 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("$countdown", color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom controls.
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryButton(uri = viewModel.lastPhotoUri, onClick = { openGallery(context, viewModel.lastPhotoUri) })

            ShutterButton(onClick = { startCapture() })

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = viewModel::toggleAutoCapture,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (viewModel.autoCaptureEnabled) Color(0xFF4CFF7A) else Color(0x66FFFFFF)
                    )
                ) {
                    Text("\u26A1", fontSize = 18.sp)
                }
                FilledIconButton(
                    onClick = viewModel::toggleCamera,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0x66FFFFFF))
                ) {
                    Text("\uD83D\uDD04", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun ScorePanel(overall: Int, pose: Int, composition: Int, status: String) {
    val overallColor = when {
        overall >= CameraViewModel.MATCH_THRESHOLD -> Color(0xFF4CFF7A)
        overall >= 70 -> Color(0xFFFFD43B)
        else -> Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .background(Color(0x88000000), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(text = "Score  $overall", color = overallColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(Color(0x66000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Pose $pose", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("•", color = Color.White, fontSize = 12.sp)
            Text("Frame $composition", color = Color(0xFFFFB37B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = status,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .background(Color(0x66000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<PoseCategory>,
    selected: PoseCategory?,
    onSelect: (PoseCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0x99000000),
                    labelColor = Color.White,
                    selectedContainerColor = Color(0xFF4CFF7A),
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun ToggleChip(active: Boolean, label: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (active) Color(0xFF4CFF7A) else Color(0x66000000)
        )
    ) {
        Text(label, fontSize = 16.sp, color = if (active) Color.Black else Color.White)
    }
}

@Composable
private fun GalleryButton(uri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(Color(0x66000000), RoundedCornerShape(12.dp))
            .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Text(if (uri != null) "\uD83D\uDDBC" else "\uD83C\uDFDE", fontSize = 22.sp)
        }
    }
}

@Composable
private fun PoseCardStrip(
    templates: List<PoseTemplate>,
    selectedId: String,
    onSelect: (PoseTemplate) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(templates) { template ->
            val selected = template.id == selectedId
            Card(
                onClick = { onSelect(template) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF4CFF7A) else Color(0xAA000000)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(78.dp).height(78.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(template.emoji, fontSize = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        template.name,
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(74.dp)
            .background(Color.White, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
                .background(Color(0xFF111111), CircleShape)
        )
    }
}

private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    isFrontCamera: Boolean,
    analysisExecutor: java.util.concurrent.ExecutorService,
    onFrame: (androidx.camera.core.ImageProxy) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        val provider = providerFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(analysisExecutor) { proxy -> onFrame(proxy) } }

        val selector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera bind failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onSaved: (Uri?) -> Unit
) {
    val name = "Lumora_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

    val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lumora")
        }
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()
    } else {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Lumora"
        ).apply { mkdirs() }
        ImageCapture.OutputFileOptions.Builder(File(dir, "$name.jpg")).build()
    }

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                ContextCompat.getMainExecutor(context).execute {
                    onSaved(result.savedUri)
                    Toast.makeText(context, "Saved \u2728", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exc: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

private fun openGallery(context: Context, uri: Uri?) {
    val intent = if (uri != null) {
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No gallery app found", Toast.LENGTH_SHORT).show()
    }
}
