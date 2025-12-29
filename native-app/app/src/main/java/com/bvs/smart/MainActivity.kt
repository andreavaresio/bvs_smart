package com.bvs.smart

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bvs.smart.data.Apiary
import com.bvs.smart.data.Arnia
import com.bvs.smart.network.ApiRepository
import com.bvs.smart.network.AuthManager
import com.bvs.smart.network.DeviceManager
import com.bvs.smart.utils.LogManager
import com.bvs.smart.ui.components.YellowPrimary
import com.bvs.smart.ui.screens.GalleryScreen
import com.bvs.smart.ui.screens.HomeScreen
import com.bvs.smart.ui.screens.LoginScreen
import com.bvs.smart.ui.screens.ScanSessionScreen
import com.bvs.smart.ui.screens.SourceSelectionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainScreen {
    LOGIN,
    HOME,
    GALLERY,
    SCAN_SESSION
}

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val apiRepository by lazy { ApiRepository(authManager, Config.API_BASE_URL) }
    private val deviceManager by lazy { DeviceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        LogManager.init(this)
        LogManager.i("App", "App Started - Version: ${packageManager.getPackageInfo(packageName, 0).versionName}")

        lifecycleScope.launch {
            try {
                val caps = deviceManager.getDeviceCapabilities()
                LogManager.i("DeviceCaps", "Capabilities: $caps")
            } catch (e: Exception) {
                LogManager.e("DeviceCaps", "Failed to get capabilities", e)
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            LogManager.e("Crash", "Uncaught Exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            val cachedResources = remember { authManager.getCachedResources() }
            val selectionSnapshot = remember { authManager.loadSelection() }
            
            val initialApiaries = remember(cachedResources) {
                cachedResources.flatMap { owner ->
                    owner.apiaries.onEach { it.ownerName = owner.ownerName }
                }
            }
            val initialApiary = remember(selectionSnapshot, initialApiaries) {
                initialApiaries.find { it.name == selectionSnapshot.apiaryName } ?: initialApiaries.firstOrNull()
            }
            val hasCachedCredentials = remember { authManager.hasCredentials() }
            
            var scanSettings by remember { mutableStateOf(authManager.loadScanSettings()) }

            val scope = rememberCoroutineScope()
            var currentScreen by remember {
                mutableStateOf(
                    if (hasCachedCredentials && initialApiaries.isNotEmpty()) MainScreen.HOME else MainScreen.LOGIN
                )
            }
            var apiaryList by remember { mutableStateOf(initialApiaries) }
            var selectedApiary by remember { mutableStateOf(initialApiary) }
            
            // Session State
            var currentSessionHive by remember { mutableStateOf<Arnia?>(null) }
            var currentSessionPhotos by remember { mutableStateOf<List<Uri?>>(emptyList()) }
            var activeSlotIndex by remember { mutableStateOf<Int?>(null) }
            var showSourceDialog by remember { mutableStateOf(false) }

            var isUploading by remember { mutableStateOf(false) }
            var isLoggingIn by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }
            
            // Temporary URI for camera capture
            var tempCaptureUri by remember { mutableStateOf<Uri?>(null) }
            
            var savedUsername by remember { mutableStateOf(authManager.getUsername().orEmpty()) }
            var savedPassword by remember { mutableStateOf(authManager.getPassword().orEmpty()) }

            val context = this@MainActivity

            val packageInfo = remember {
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val flags = PackageManager.PackageInfoFlags.of(0)
                        context.packageManager.getPackageInfo(context.packageName, flags)
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                }.getOrNull()
            }
            val versionName = packageInfo?.versionName ?: "-"
            val versionCode = (packageInfo?.longVersionCode ?: 0L).toInt()

            LaunchedEffect(Unit) {
                if (currentScreen == MainScreen.HOME) {
                    authManager.saveSelection(
                        apiaryName = selectedApiary?.name,
                        hiveCode = null,
                        scale = scanSettings.scale
                    )
                }
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val slot = activeSlotIndex
                    if (slot != null && tempCaptureUri != null) {
                        val newList = currentSessionPhotos.toMutableList()
                        if (slot in newList.indices) {
                            newList[slot] = tempCaptureUri
                            currentSessionPhotos = newList
                        }
                    }
                }
                activeSlotIndex = null
                tempCaptureUri = null
            }

            val startCameraForSlot: (Int) -> Unit = { slotIndex ->
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile = createPhotoFile(context)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCaptureUri = uri
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                activeSlotIndex = slotIndex
                cameraLauncher.launch(intent)
            }
            
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && activeSlotIndex != null) {
                    startCameraForSlot(activeSlotIndex!!)
                } else {
                    Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_LONG).show()
                }
            }

            val shareLogs: () -> Unit = {
                val logFile = LogManager.getLogFile()
                if (logFile != null && logFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        logFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Condividi Log"))
                } else {
                    Toast.makeText(context, "Nessun file di log trovato", Toast.LENGTH_SHORT).show()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        MainScreen.LOGIN -> LoginScreen(
                            onLoginSuccess = { username, password ->
                                scope.launch {
                                    isLoggingIn = true
                                    loginError = null
                                    try {
                                        val response = apiRepository.getResources(username, password, Config.SCANNER_ID)
                                        if (response.isSuccessful) {
                                            val payload = response.body().orEmpty()
                                            payload.forEach { owner ->
                                                owner.apiaries.forEach { it.ownerName = owner.ownerName }
                                            }
                                            val allApiaries = payload.flatMap { it.apiaries }
                                            apiaryList = allApiaries
                                            if (allApiaries.isNotEmpty()) {
                                                val previousSelection = authManager.loadSelection()
                                                val matchingApiary = allApiaries.find { it.name == previousSelection.apiaryName }
                                                    ?: allApiaries.first()
                                                
                                                selectedApiary = matchingApiary
                                                scanSettings = authManager.loadScanSettings()

                                                savedUsername = username
                                                savedPassword = password
                                                authManager.saveSelection(
                                                    apiaryName = selectedApiary?.name,
                                                    hiveCode = null,
                                                    scale = scanSettings.scale
                                                )
                                                currentScreen = MainScreen.HOME
                                            } else {
                                                selectedApiary = null
                                                loginError = "Nessun dato disponibile."
                                            }
                                        } else {
                                            loginError = "Credenziali errate."
                                        }
                                    } catch (error: Exception) {
                                        if (apiaryList.isNotEmpty() && username == savedUsername && password == savedPassword) {
                                            currentScreen = MainScreen.HOME
                                        } else {
                                            loginError = "Errore di rete: ${error.message}"
                                        }
                                    } finally {
                                        isLoggingIn = false
                                    }
                                }
                            },
                            isLoading = isLoggingIn,
                            errorMessage = loginError,
                            initialUsername = savedUsername,
                            initialPassword = savedPassword,
                            versionName = versionName,
                            versionCode = versionCode
                        )

                        MainScreen.HOME -> HomeScreen(
                            selectedApiary = selectedApiary,
                            apiaryList = apiaryList,
                            scanSettings = scanSettings,
                            versionName = versionName,
                            versionCode = versionCode,
                            baseUrl = Config.API_BASE_URL,
                            loggedUsername = savedUsername,
                            onApiarySelected = { apiary ->
                                selectedApiary = apiary
                                authManager.saveSelection(
                                    apiaryName = selectedApiary?.name,
                                    hiveCode = null,
                                    scale = scanSettings.scale
                                )
                            },
                            onScanRequest = { hive, settings ->
                                // Prepare Session
                                currentSessionHive = hive
                                scanSettings = settings
                                // Initialize photo slots (nulls)
                                currentSessionPhotos = List(settings.photosPerScan) { null }
                                
                                authManager.saveScanSettings(
                                    settings.scale, settings.permanenceDays, settings.measureType, settings.photosPerScan
                                )
                                
                                currentScreen = MainScreen.SCAN_SESSION
                            },
                            onShareLogs = shareLogs,
                            onLogout = {
                                authManager.logout()
                                apiaryList = emptyList()
                                selectedApiary = null
                                savedPassword = "" // Reset UI state password
                                currentScreen = MainScreen.LOGIN
                            }
                        )

                        MainScreen.SCAN_SESSION -> {
                            val hive = currentSessionHive
                            if (hive != null) {
                                ScanSessionScreen(
                                    hive = hive,
                                    settings = scanSettings,
                                    photos = currentSessionPhotos,
                                    onPhotoSlotClicked = { index ->
                                        activeSlotIndex = index
                                        showSourceDialog = true
                                    },
                                    onDeletePhoto = { index ->
                                        val newList = currentSessionPhotos.toMutableList()
                                        newList[index] = null
                                        currentSessionPhotos = newList
                                    },
                                    onSend = {
                                        val validUris = currentSessionPhotos.filterNotNull()
                                        if (validUris.size == scanSettings.photosPerScan) {
                                            isUploading = true
                                            uploadPhotos(
                                                context = context,
                                                uris = validUris,
                                                hive = hive,
                                                settings = scanSettings,
                                                onComplete = { 
                                                    isUploading = false 
                                                    currentScreen = MainScreen.HOME // Go back home after upload
                                                }
                                            )
                                        }
                                    },
                                    onBack = { currentScreen = MainScreen.HOME }
                                )
                            }
                        }

                        MainScreen.GALLERY -> GalleryScreen(
                            onPhotoSelected = { uri ->
                                val slot = activeSlotIndex
                                if (slot != null && slot in currentSessionPhotos.indices) {
                                    val newList = currentSessionPhotos.toMutableList()
                                    newList[slot] = uri
                                    currentSessionPhotos = newList
                                }
                                activeSlotIndex = null
                                currentScreen = MainScreen.SCAN_SESSION
                            },
                            onBack = { currentScreen = MainScreen.SCAN_SESSION }
                        )
                    }
                }

                if (showSourceDialog) {
                    SourceSelectionDialog(
                        onDismiss = { 
                            showSourceDialog = false 
                            activeSlotIndex = null
                        },
                        onCamera = {
                            showSourceDialog = false
                            val slot = activeSlotIndex
                            if (slot != null) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    startCameraForSlot(slot)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        onGallery = {
                            showSourceDialog = false
                            currentScreen = MainScreen.GALLERY
                        }
                    )
                }

                if (isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable(enabled = false) {}
                            .zIndex(10f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = YellowPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Invio ${scanSettings.photosPerScan} foto...", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    private fun createPhotoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun uploadPhotos(
        context: Context,
        uris: List<Uri>,
        hive: Arnia,
        settings: AuthManager.ScanSettings,
        onComplete: () -> Unit
    ) {
        val username = authManager.getUsername()
        val password = authManager.getPassword()

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            Toast.makeText(context, "Login richiesto.", Toast.LENGTH_LONG).show()
            onComplete()
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Prepare Multipart Body parts for files[]
                val fileParts = uris.mapIndexed { index, uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("upload_$index", ".jpg", context.cacheDir)
                    tempFile.outputStream().use { output ->
                        inputStream?.copyTo(output)
                    }
                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files[]", tempFile.name, requestFile)
                }

                val now = Date()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
                val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'", Locale.US)
                val textMediaType = "text/plain".toMediaTypeOrNull()

                val params: Map<String, RequestBody> = mapOf(
                    "username" to username.toRequestBody(textMediaType),
                    "password" to password.toRequestBody(textMediaType),
                    "arniaId" to hive.code.toRequestBody(textMediaType),
                    "note" to "Sessione ${settings.photosPerScan} foto. ${SimpleDateFormat("dd/MM HH:mm", Locale.ITALY).format(now)}"
                        .toRequestBody(textMediaType),
                    "ScaleforConta" to String.format(Locale.US, "%.2f", settings.scale).toRequestBody(textMediaType),
                    "timestamp" to timestampFormat.format(now).replace(":", "-").toRequestBody(textMediaType),
                    "GPS" to "45.0352891,7.5168128".toRequestBody(textMediaType),
                    "NumeroGGPermanenza" to settings.permanenceDays.toString().toRequestBody(textMediaType),
                    "data_prelievo_data" to dateFormat.format(now).toRequestBody(textMediaType),
                    "data_prelievo_time" to timeFormat.format(now).toRequestBody(textMediaType),
                    "tipo_misura" to settings.measureType.toRequestBody(textMediaType)
                )

                // Call API (Need to update ApiService to accept List<MultipartBody.Part>?)
                // Current ApiService expects @Part file: MultipartBody.Part. 
                // We need to change it to accept List. But wait, ApiRepository/ApiService signature needs update.
                // For now, let's assume we update ApiRepository next.
                
                val response = apiRepository.uploadFotoMulti(params, fileParts)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Upload completato!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Errore upload: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                    onComplete()
                }
            } catch (error: Exception) {
                error.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore: ${error.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }
}
