package com.bvs.smart.ui.screens

import android.Manifest
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bvs.smart.ui.components.BeehiveBadge
import com.bvs.smart.ui.components.DarkBackground
import com.bvs.smart.ui.components.PrimaryButton
import com.bvs.smart.ui.components.SecondaryButton
import com.bvs.smart.ui.components.YellowPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InternalCameraScreen(
    beehiveLabel: String?,
    scale: Double,
    onPhotoCaptured: (Uri, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    // Permission Handling in Compose (using Accompanist library).
    // It monitors the state of the permission (Granted, Denied, etc.).
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    
    if (cameraPermissionState.status.isGranted) {
        CameraContent(
            beehiveLabel = beehiveLabel,
            scale = scale,
            onPhotoCaptured = onPhotoCaptured,
            onBack = onBack
        )
    } else {
        // Request permission as soon as this composable enters the composition
        LaunchedEffect(Unit) {
            cameraPermissionState.launchPermissionRequest()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Camera permission is required", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                SecondaryButton(text = "Request Permission", onClick = { cameraPermissionState.launchPermissionRequest() })
            }
        }
    }
}

@Composable
fun CameraContent(
    beehiveLabel: String?,
    scale: Double,
    onPhotoCaptured: (Uri, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    // LocalContext.current gives access to the Android Context (Activity/Application) within a Composable.
    val context = LocalContext.current
    // LocalLifecycleOwner.current gives access to the Lifecycle (needed for CameraX to know when to start/stop).
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Remember the camera configuration to avoid re-initializing it on every redraw.
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build() 
    }
    val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
    
    var isSaving by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // AndroidView: The bridge between Compose and classic Android Views.
        // CameraX uses a "PreviewView" which is a classic View, so we wrap it here.
        AndroidView(
            factory = { ctx ->
                // This block runs once to create the View.
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                // This block runs when the view needs an update.
                // Here we bind the camera lifecycle.
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Overlay UI controls on top of the camera preview
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (beehiveLabel != null) {
                BeehiveBadge(label = beehiveLabel)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = "Scale: $scale", color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            PrimaryButton(
                text = if (isSaving) "Saving..." else "Scatta",
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onImageCaptured = { uri, width, height ->
                                isSaving = false
                                onPhotoCaptured(uri, width, height)
                            },
                            onError = {
                                isSaving = false
                            }
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            SecondaryButton(text = "Back", onClick = onBack)
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri, Int, Int) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = createPhotoFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
                onError(exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                // Scan the file so it appears in gallery immediately (optional, but good practice)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(photoFile.absolutePath),
                    null,
                    null
                )
                // We don't have exact dimensions from OutputFileResults usually, 
                // but we can decode or just pass 0 if backend doesn't strictly require it immediately.
                // For now, let's pass 0, 0 or read if critical.
                onImageCaptured(savedUri, 0, 0)
            }
        }
    )
}

private fun createPhotoFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
}
