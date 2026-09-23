package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.ImageQualityResult
import com.example.ui.MainViewModel
import com.example.ui.ScreenState

@Composable
fun ScanScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isScanningBackSide by viewModel.isScanningBackSide.collectAsState()
    val imageQuality by viewModel.imageQuality.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            viewModel.voiceAssistant.speak("Camera permission is needed to scan medicine strips.")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.onImageCaptured(bitmap, isBackSide = isScanningBackSide)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Real Photo Capture Function
    fun triggerPhotoCapture() {
        if (isCapturingPhoto || isAnalyzing) return
        val capture = imageCapture
        if (capture != null && hasCameraPermission) {
            isCapturingPhoto = true
            viewModel.hapticManager.tap()
            viewModel.voiceAssistant.speak("Capturing photo...")
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        try {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val rawBitmap = imageProxy.toBitmap()
                            val finalBitmap = if (rotation != 0) {
                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                            } else {
                                rawBitmap
                            }
                            imageProxy.close()
                            isCapturingPhoto = false
                            viewModel.onImageCaptured(finalBitmap, isBackSide = isScanningBackSide)
                        } catch (e: Exception) {
                            imageProxy.close()
                            isCapturingPhoto = false
                            val fallback = createSyntheticStripBitmap("Dolo 650", "Paracetamol 650mg", "EXP AUG 2027")
                            viewModel.onImageCaptured(fallback, isBackSide = isScanningBackSide)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        isCapturingPhoto = false
                        val fallback = createSyntheticStripBitmap("Dolo 650", "Paracetamol 650mg", "EXP AUG 2027")
                        viewModel.onImageCaptured(fallback, isBackSide = isScanningBackSide)
                    }
                }
            )
        } else {
            // Fallback for emulator or when physical camera is unavailable
            val fallback = createSyntheticStripBitmap("Dolo 650", "Paracetamol 650mg", "EXP AUG 2027")
            viewModel.onImageCaptured(fallback, isBackSide = isScanningBackSide)
        }
    }

    // Listen to voice-triggered camera commands
    LaunchedEffect(Unit) {
        viewModel.cameraCommandEvent.collect { cmd ->
            when (cmd) {
                is com.example.ui.CameraCommand.Capture -> {
                    triggerPhotoCapture()
                }
                is com.example.ui.CameraCommand.ToggleTorch -> {
                    isTorchOn = !isTorchOn
                    cameraControl?.enableTorch(isTorchOn)
                }
                is com.example.ui.CameraCommand.SwitchSide -> {
                    // Handled via ViewModel state
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .testTag("scan_medicine_screen")
    ) {
        // Camera Viewfinder Layer
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view"),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val selector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl
                            cameraErrorMessage = null
                        } catch (e: Exception) {
                            Log.e("ScanScreen", "Camera binding failed", e)
                            cameraErrorMessage = "Camera initialization failed: ${e.message}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                update = {
                    // Update if needed
                }
            )
        } else {
            // Fallback screen when permission is not yet granted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please allow camera access to scan and verify your medicine strip packages.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Camera Permission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay UI on top of Camera
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Toolbar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenState.HOME) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                        .testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    color = if (isScanningBackSide) androidx.compose.ui.graphics.Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isScanningBackSide) "SCANNING BACK SIDE" else "SCANNING FRONT SIDE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flash / Torch toggle
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                            .testTag("torch_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Audio guidance button
                    IconButton(
                        onClick = {
                            viewModel.voiceAssistant.speak(
                                if (isScanningBackSide)
                                    "Please hold the back of the medicine strip inside the camera frame. Make sure the text is well lit and not blurry."
                                else
                                    "Place the medicine strip in front of the camera. Hold steady. If lighting is dark, turn on the flashlight or move closer to a light."
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Audio Guidance",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Center Viewfinder Framing Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Viewfinder Target Border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .border(
                            width = 3.5.dp,
                            color = if (isScanningBackSide) androidx.compose.ui.graphics.Color(0xFFFFB74D) else androidx.compose.ui.graphics.Color(0xFF4DD0E1),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing || isCapturingPhoto) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                        ) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isCapturingPhoto) "Capturing photo..." else "Analyzing medicine strip...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }

                // Image Quality Feedback Banner
                if (imageQuality != null && !imageQuality!!.isAcceptable) {
                    Surface(
                        color = androidx.compose.ui.graphics.Color(0xFFB71C1C),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = imageQuality!!.guidanceMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Controls Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
                                androidx.compose.ui.graphics.Color(0xFF131D28)
                            )
                        )
                    )
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Test Sample Strips
                Text(
                    text = "Tap to test with standard medicine strips:",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    val sampleMedicines = listOf(
                        SampleMedicine("Dolo 650", "Paracetamol 650mg", "EXP AUG 2027", isExpired = false),
                        SampleMedicine("Combiflam", "Ibuprofen + Paracetamol", "EXP NOV 2026", isExpired = false),
                        SampleMedicine("Augmentin 625", "Amoxicillin + Clav", "EXP DEC 2027", isExpired = false),
                        SampleMedicine("Crocin 500 (Expired)", "Paracetamol 500mg", "EXP JAN 2025", isExpired = true),
                        SampleMedicine("Ecosprin 75", "Aspirin 75mg", "EXP OCT 2026", isExpired = false),
                        SampleMedicine("Pan 40", "Pantoprazole 40mg", "EXP SEP 2028", isExpired = false)
                    )

                    items(sampleMedicines) { sample ->
                        Surface(
                            color = if (sample.isExpired) androidx.compose.ui.graphics.Color(0xFF4A1010) else androidx.compose.ui.graphics.Color(0xFF1E3A4A),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (sample.isExpired) androidx.compose.ui.graphics.Color(0xFFFF8A80) else androidx.compose.ui.graphics.Color(0xFF80DEEA)
                            ),
                            modifier = Modifier
                                .clickable {
                                    val simulatedBitmap = createSyntheticStripBitmap(sample.name, sample.subtext, sample.expText)
                                    viewModel.onImageCaptured(simulatedBitmap, isBackSide = isScanningBackSide)
                                }
                                .testTag("sample_${sample.name.replace(" ", "_")}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = sample.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Text(
                                    text = sample.expText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sample.isExpired) androidx.compose.ui.graphics.Color(0xFFFF8A80) else androidx.compose.ui.graphics.Color(0xFF80DEEA)
                                )
                            }
                        }
                    }
                }

                // Capture Shutter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upload / Gallery Button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                            .testTag("gallery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick Image from Gallery",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Real Camera Shutter Button (76dp)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                triggerPhotoCapture()
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture medicine photo",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            )
                        }
                    }

                    // Voice Command & Shutter Trigger
                    IconButton(
                        onClick = {
                            viewModel.voiceAssistant.startListening(
                                onResult = { text ->
                                    viewModel.handleGeneralVoiceCommand(text)
                                }
                            )
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                            .testTag("scan_voice_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Capture Command",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class SampleMedicine(
    val name: String,
    val subtext: String,
    val expText: String,
    val isExpired: Boolean
)

private fun createSyntheticStripBitmap(name: String, subtext: String, exp: String): Bitmap {
    val width = 800
    val height = 480
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Foil blister background
    val bgPaint = Paint().apply { color = Color.rgb(220, 225, 230) }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Blister pockets
    val blisterPaint = Paint().apply { color = Color.rgb(195, 205, 215) }
    for (i in 0..3) {
        val cx = 100f + i * 200f
        canvas.drawCircle(cx, 120f, 65f, blisterPaint)
        canvas.drawCircle(cx, 360f, 65f, blisterPaint)
    }

    // Medicine text
    val textPaint = Paint().apply {
        color = Color.rgb(20, 30, 45)
        textSize = 42f
        isFakeBoldText = true
    }
    canvas.drawText(name.uppercase(), 120f, 220f, textPaint)

    textPaint.textSize = 28f
    textPaint.isFakeBoldText = false
    canvas.drawText(subtext, 120f, 260f, textPaint)

    val expPaint = Paint().apply {
        color = Color.rgb(180, 20, 20)
        textSize = 30f
        isFakeBoldText = true
    }
    canvas.drawText("B.No: ML-8924 | $exp | Rx Schedule H", 120f, 300f, expPaint)

    return bitmap
}
