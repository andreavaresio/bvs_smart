package com.bvs.smart

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.bvs.smart.data.BEEHIVES
import com.bvs.smart.data.Beehive
import com.bvs.smart.network.NetworkModule
import com.bvs.smart.ui.components.YellowPrimary
import com.bvs.smart.ui.screens.GalleryScreen
import com.bvs.smart.ui.screens.HomeScreen
import com.bvs.smart.ui.screens.InternalCameraScreen
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent is the entry point for Jetpack Compose.
        // It replaces XML layout inflation (setContentView) and defines the UI using Composable functions.
        setContent {
            // State: Variables that trigger a UI redraw (recomposition) when changed.
            // remember { ... } preserves the value across recompositions (when the UI redraws).
            // mutableStateOf(...) creates an observable state holder.
            // by keyword allows using the variable directly (delegation).
            var currentScreen by remember { mutableStateOf("home") }
            var selectedBeehive by remember { mutableStateOf(BEEHIVES.first()) }
            var scale by remember { mutableStateOf(1.0) }
            var isUploading by remember { mutableStateOf(false) }

            // External Camera State
            var tempExternalUri by remember { mutableStateOf<Uri?>(null) }

            // registerForActivityResult is used to launch other activities (like the Camera app)
            // and handle the result (success/failure/data) in a callback.
            val externalCameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success ->
                if (success && tempExternalUri != null) {
                    val uri = tempExternalUri!!
                    // Handle upload
                    isUploading = true
                    uploadPhoto(
                        context = this,
                        uri = uri,
                        beehive = selectedBeehive,
                        scale = scale,
                        onComplete = { isUploading = false }
                    )
                }
            }

            // Sync Capabilities on start
            val deviceManager = remember { com.bvs.smart.network.DeviceManager(this@MainActivity) }
            LaunchedEffect(Unit) {
                deviceManager.syncCapabilities()
            }

            // Box: A layout composable that stacks children on top of each other.
            // Equivalent to FrameLayout in classic Views.
            // Modifier.fillMaxSize() makes it take up the entire screen.
            Box(modifier = Modifier.fillMaxSize()) {
                // Simple State-based Navigation:
                // We simply switch which Composable function is shown based on the 'currentScreen' string.
                when (currentScreen) {
                    "home" -> HomeScreen(
                        selectedBeehive = selectedBeehive,
                        scale = scale,
                        onInternalCamera = { currentScreen = "internal_camera" },
                        onExternalCamera = {
                            val photoFile = createPhotoFile(this@MainActivity)
                            val uri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${applicationContext.packageName}.fileprovider",
                                photoFile
                            )
                            tempExternalUri = uri
                            externalCameraLauncher.launch(uri)
                        },
                        onGallery = { currentScreen = "gallery" },
                        onUpdateSettings = { newBeehive, newScale ->
                            selectedBeehive = newBeehive
                            scale = newScale
                        }
                    )

                    "internal_camera" -> InternalCameraScreen(
                        beehiveLabel = selectedBeehive.label,
                        scale = scale,
                        onPhotoCaptured = { uri, _, _ ->
                            currentScreen = "home"
                            isUploading = true
                            uploadPhoto(
                                context = this@MainActivity,
                                uri = uri,
                                beehive = selectedBeehive,
                                scale = scale,
                                onComplete = { isUploading = false }
                            )
                        },
                        onBack = { currentScreen = "home" }
                    )

                    "gallery" -> GalleryScreen(
                        onPhotoSelected = { uri ->
                            currentScreen = "home"
                            isUploading = true
                            uploadPhoto(
                                context = this@MainActivity,
                                uri = uri,
                                beehive = selectedBeehive,
                                scale = scale,
                                onComplete = { isUploading = false }
                            )
                        },
                        onBack = { currentScreen = "home" }
                    )
                }

                if (isUploading) {
                    // Overlay a loading spinner on top of everything
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = YellowPrimary)
                    }
                }
            }
        }
    }

    private fun createPhotoFile(context: Context): File {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun uploadPhoto(
        context: Context,
        uri: Uri,
        beehive: Beehive,
        scale: Double,
        onComplete: () -> Unit
    ) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            try {
                // Prepare File
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
                tempFile.outputStream().use { output ->
                    inputStream?.copyTo(output)
                }

                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("files[]", tempFile.name, requestFile)

                val now = Date()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
                val timestampFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'", Locale.US) // RN: iso with -

                val params = mapOf(
                    "username" to "test@test.com".toRequestBody(MultipartBody.FORM),
                    "password" to "Ap1sf3ro.123".toRequestBody(MultipartBody.FORM),
                    "arniaId" to beehive.id.toRequestBody(MultipartBody.FORM),
                    "note" to "Foto scattata il ${
                        SimpleDateFormat(
                            "dd MMMM yyyy HH:mm",
                            Locale.ITALY
                        ).format(now)
                    }".toRequestBody(MultipartBody.FORM),
                    "ScaleforConta" to String.format(Locale.US, "%.2f", scale)
                        .toRequestBody(MultipartBody.FORM),
                    "timestamp" to timestampFormat.format(now).replace(":", "-")
                        .toRequestBody(MultipartBody.FORM),
                    "GPS" to "45.0352891,7.5168128".toRequestBody(MultipartBody.FORM),
                    "NumeroGGPermanenza" to "0".toRequestBody(MultipartBody.FORM),
                    "data_prelievo_data" to dateFormat.format(now)
                        .toRequestBody(MultipartBody.FORM),
                    "data_prelievo_time" to timeFormat.format(now)
                        .toRequestBody(MultipartBody.FORM),
                    "tipo_misura" to "CadutaNaturale".toRequestBody(MultipartBody.FORM)
                )

                val response = NetworkModule.apiService.uploadPhoto(
                    url = "https://apisferoweb.it/api/v4/APIUploadImage",
                    parts = params,
                    file = body
                )

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            context,
                            "Upload completato: ${response.body()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Upload failed: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }
}
