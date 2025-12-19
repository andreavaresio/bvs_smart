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
import com.bvs.smart.ui.components.YellowPrimary
import com.bvs.smart.ui.screens.GalleryScreen
import com.bvs.smart.ui.screens.HomeScreen
import com.bvs.smart.ui.screens.InternalCameraScreen
import com.bvs.smart.ui.screens.LoginScreen
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

private const val API_BASE_URL = "https://ec200296.seewebcloud.it/api/v4/"
private const val DEFAULT_SCANNER = "SCANNER_DEMO_1"

private enum class MainScreen {
    LOGIN,
    HOME,
    INTERNAL_CAMERA,
    GALLERY
}

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val apiRepository by lazy { ApiRepository(authManager, API_BASE_URL) }
    private val deviceManager by lazy { DeviceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val cachedResources = remember { authManager.getCachedResources() }
            val selectionSnapshot = remember { authManager.loadSelection() }
            val initialApiaries = remember(cachedResources) { cachedResources.flatMap { it.apiaries } }
            val initialApiary = remember(selectionSnapshot, initialApiaries) {
                initialApiaries.find { it.name == selectionSnapshot.apiaryName } ?: initialApiaries.firstOrNull()
            }
            val initialHives = remember(initialApiary) { initialApiary?.hives.orEmpty() }
            val initialHive = remember(selectionSnapshot, initialHives) {
                initialHives.find { it.code == selectionSnapshot.hiveCode } ?: initialHives.firstOrNull()
            }
            val initialScale = selectionSnapshot.scale ?: 1.0
            val hasCachedCredentials = remember { authManager.hasCredentials() }

            val scope = rememberCoroutineScope()
            var currentScreen by remember {
                mutableStateOf(
                    if (hasCachedCredentials && initialApiaries.isNotEmpty()) MainScreen.HOME else MainScreen.LOGIN
                )
            }
            var apiaryList by remember { mutableStateOf(initialApiaries) }
            var selectedApiary by remember { mutableStateOf(initialApiary) }
            var hiveList by remember { mutableStateOf(initialHives) }
            var selectedHive by remember { mutableStateOf(initialHive) }
            var scale by remember { mutableStateOf(initialScale) }
            var isUploading by remember { mutableStateOf(false) }
            var isLoggingIn by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }
            var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
            var tempExternalUri by remember { mutableStateOf<Uri?>(null) }
            var savedUsername by remember { mutableStateOf(authManager.getUsername().orEmpty()) }
            var savedPassword by remember { mutableStateOf(authManager.getPassword().orEmpty()) }
            var savedScanner by remember { mutableStateOf(DEFAULT_SCANNER) }

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
                        hiveCode = selectedHive?.code,
                        scale = scale
                    )
                }
                // Syncing device capabilities is disabled for now; re-enable if backend needs the data again.
//                deviceManager.syncCapabilities()
            }

            val externalCameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    tempExternalUri?.let { pendingUploadUri = it }
                } else {
                    tempExternalUri = null
                }
            }

            val startExternalCamera: () -> Unit = {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile = createPhotoFile(context)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempExternalUri = uri
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                externalCameraLauncher.launch(intent)
            }

            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    startExternalCamera()
                } else {
                    Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_LONG).show()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        MainScreen.LOGIN -> LoginScreen(
                            onLoginSuccess = { username, password, scanner ->
                                scope.launch {
                                    isLoggingIn = true
                                    loginError = null
                                    try {
                                        val response = apiRepository.getResources(username, password, scanner)
                                        if (response.isSuccessful) {
                                            val payload = response.body().orEmpty()
                                            val allApiaries = payload.flatMap { it.apiaries }
                                            apiaryList = allApiaries
                                            if (allApiaries.isNotEmpty()) {
                                                val previousSelection = authManager.loadSelection()
                                                val matchingApiary = allApiaries.find { it.name == previousSelection.apiaryName }
                                                    ?: allApiaries.first()
                                                val matchingHives = matchingApiary.hives
                                                val matchingHive = matchingHives.find { it.code == previousSelection.hiveCode }
                                                    ?: matchingHives.firstOrNull()

                                                selectedApiary = matchingApiary
                                                hiveList = matchingHives
                                                selectedHive = matchingHive
                                                previousSelection.scale?.let { scale = it }

                                                savedUsername = username
                                                savedPassword = password
                                                savedScanner = scanner
                                                authManager.saveSelection(
                                                    apiaryName = selectedApiary?.name,
                                                    hiveCode = selectedHive?.code,
                                                    scale = scale
                                                )
                                                currentScreen = MainScreen.HOME
                                            } else {
                                                selectedApiary = null
                                                hiveList = emptyList()
                                                selectedHive = null
                                                loginError =
                                                    "Nessun dato disponibile per il profilo selezionato."
                                            }
                                        } else {
                                            loginError =
                                                "Forse hai sbagliato le credenziali di accesso, riprova o contattaci per risolvere il problema"
                                        }
                                    } catch (error: Exception) {
                                        if (apiaryList.isNotEmpty() && username == savedUsername && password == savedPassword) {
                                            loginError = null
                                            Toast.makeText(
                                                context,
                                                "Connessione non disponibile, uso i dati salvati.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            currentScreen = MainScreen.HOME
                                        } else {
                                            loginError =
                                                "Forse hai sbagliato le credenziali di accesso, riprova o contattaci per risolvere il problema"
                                            Toast.makeText(
                                                context,
                                                "Errore di rete: ${error.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
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
                            initialScanner = savedScanner,
                            versionName = versionName,
                            versionCode = versionCode
                        )

                        MainScreen.HOME -> HomeScreen(
                            selectedApiary = selectedApiary,
                            selectedArnia = selectedHive,
                            scale = scale,
                            versionName = versionName,
                            versionCode = versionCode,
                            apiaryList = apiaryList,
                            hiveList = hiveList,
                            onInternalCamera = { currentScreen = MainScreen.INTERNAL_CAMERA },
                            onExternalCamera = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    startExternalCamera()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            onGallery = { currentScreen = MainScreen.GALLERY },
                            onApiarySelected = { apiary ->
                                selectedApiary = apiary
                                hiveList = apiary.hives
                                selectedHive = hiveList.firstOrNull()
                                authManager.saveSelection(
                                    apiaryName = selectedApiary?.name,
                                    hiveCode = selectedHive?.code,
                                    scale = scale
                                )
                            },
                            onArniaSelected = { arnia ->
                                selectedHive = arnia
                                authManager.saveSelection(
                                    apiaryName = selectedApiary?.name,
                                    hiveCode = selectedHive?.code,
                                    scale = scale
                                )
                            },
                            onScaleUpdated = { newScale ->
                                scale = newScale
                                authManager.saveSelection(
                                    apiaryName = selectedApiary?.name,
                                    hiveCode = selectedHive?.code,
                                    scale = scale
                                )
                            }
                        )

                        MainScreen.INTERNAL_CAMERA -> InternalCameraScreen(
                            beehiveLabel = selectedHive?.name.orEmpty(),
                            scale = scale,
                            onPhotoCaptured = { uri, _, _ ->
                                pendingUploadUri = uri
                                currentScreen = MainScreen.HOME
                            },
                            onBack = { currentScreen = MainScreen.HOME }
                        )

                        MainScreen.GALLERY -> GalleryScreen(
                            onPhotoSelected = { uri ->
                                pendingUploadUri = uri
                                currentScreen = MainScreen.HOME
                            },
                            onBack = { currentScreen = MainScreen.HOME }
                        )
                    }
                }

                if (pendingUploadUri != null) {
                    AlertDialog(
                        onDismissRequest = { pendingUploadUri = null },
                        title = { Text(text = "Conferma Invio") },
                        text = { Text(text = "Vuoi inviare la foto al server?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val uri = pendingUploadUri
                                    val hive = selectedHive
                                    if (uri != null && hive != null) {
                                        pendingUploadUri = null
                                        isUploading = true
                                        uploadPhoto(
                                            context = context,
                                            uri = uri,
                                            hive = hive,
                                            scale = scale,
                                            onComplete = { isUploading = false }
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Seleziona un'arnia prima di caricare una foto.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            ) {
                                Text("Sì", color = YellowPrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingUploadUri = null }) {
                                Text("No", color = Color.Gray)
                            }
                        },
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White,
                        textContentColor = Color.White
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
                            Text(
                                text = "Invio in corso...",
                                color = Color.White
                            )
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

    private fun uploadPhoto(
        context: Context,
        uri: Uri,
        hive: Arnia,
        scale: Double,
        onComplete: () -> Unit
    ) {
        val username = authManager.getUsername()
        val password = authManager.getPassword()

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            Toast.makeText(
                context,
                "Effettua il login per poter inviare le foto.",
                Toast.LENGTH_LONG
            ).show()
            onComplete()
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
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
                val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'", Locale.US)

                val params: Map<String, RequestBody> = mapOf(
                    "username" to username.toRequestBody(MultipartBody.FORM),
                    "password" to password.toRequestBody(MultipartBody.FORM),
                    "arniaId" to hive.code.toRequestBody(MultipartBody.FORM),
                    "note" to "Foto scattata il ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.ITALY).format(now)}"
                        .toRequestBody(MultipartBody.FORM),
                    "ScaleforConta" to String.format(Locale.US, "%.2f", scale)
                        .toRequestBody(MultipartBody.FORM),
                    "timestamp" to timestampFormat.format(now).replace(":", "-")
                        .toRequestBody(MultipartBody.FORM),
                    "GPS" to "45.0352891,7.5168128".toRequestBody(MultipartBody.FORM),
                    "NumeroGGPermanenza" to "0".toRequestBody(MultipartBody.FORM),
                    "data_prelievo_data" to dateFormat.format(now).toRequestBody(MultipartBody.FORM),
                    "data_prelievo_time" to timeFormat.format(now).toRequestBody(MultipartBody.FORM),
                    "tipo_misura" to "CadutaNaturale".toRequestBody(MultipartBody.FORM)
                )

                val response = apiRepository.uploadFoto(params, body)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val message = response.body()?.string() ?: "Upload completato"
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Upload non riuscito: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onComplete()
                }
            } catch (error: Exception) {
                error.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Errore durante l'upload: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                }
            }
        }
    }
}
