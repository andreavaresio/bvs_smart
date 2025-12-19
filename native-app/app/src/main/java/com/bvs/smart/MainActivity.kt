import com.bvs.smart.data.BEEHIVES
import com.bvs.smart.data.Beehive
import com.bvs.smart.network.NetworkModule
import com.bvs.smart.ui.components.YellowPrimary
import com.bvs.smart.ui.screens.GalleryScreen
import com.bvs.smart.ui.screens.HomeScreen
import com.bvs.smart.ui.screens.InternalCameraScreen
import com.bvs.smart.ui.screens.LoginScreen
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super.onCreate
        // installSplashScreen() // Reverted as per user request
        
        super.onCreate(savedInstanceState)
        // setContent is the entry point for Jetpack Compose.
        // It replaces XML layout inflation (setContentView) and defines the UI using Composable functions.
        setContent {
            // State: Variables that trigger a UI redraw (recomposition) when changed.
            // remember { ... } preserves the value across recompositions (when the UI redraws).
            // mutableStateOf(...) creates an observable state holder.
            // by keyword allows using the variable directly (delegation).
            var currentScreen by remember { mutableStateOf("login") }
            var selectedBeehive by remember { mutableStateOf<Beehive>(BEEHIVES.first()) }
            var scale by remember { mutableStateOf(1.0) }
            var isUploading by remember { mutableStateOf(false) }
            var isLoggingIn by remember { mutableStateOf(false) }
            var userToken by remember { mutableStateOf<String?>(null) }
            var beehiveList by remember { mutableStateOf(BEEHIVES) }

            // External Camera State
            var tempExternalUri by remember { mutableStateOf<Uri?>(null) }
            
            // Pending Upload State (for confirmation dialog)
            var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }

            val scope = kotlinx.coroutines.rememberCoroutineScope()

            // ... (keep externalCameraLauncher and cameraPermissionLauncher)

            // Sync Capabilities on start
            val deviceManager = remember { com.bvs.smart.network.DeviceManager(this@MainActivity) }
            LaunchedEffect(Unit) {
                deviceManager.syncCapabilities()
            }

            // ... (keep package info)

            // Box: A layout composable that stacks children on top of each other.
            // Equivalent to FrameLayout in classic Views.
            // Modifier.fillMaxSize() makes it take up the entire screen.
            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade<String>(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        "login" -> LoginScreen(
                            isLoading = isLoggingIn,
                            onLoginSuccess = { username, password ->
                                scope.launch {
                                    isLoggingIn = true
                                    try {
                                        val deviceId = android.provider.Settings.Secure.getString(
                                            contentResolver,
                                            android.provider.Settings.Secure.ANDROID_ID
                                        ) ?: "unknown"
                                        
                                        val response = NetworkModule.apiService.login(
                                            url = "https://apisferoweb.it/api/v4/APILogin",
                                            payload = com.bvs.smart.data.LoginRequest(username, password, deviceId)
                                        )
                                        
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val token = response.body()?.token ?: ""
                                            userToken = token
                                            
                                            // Fetch Beehives
                                            try {
                                                val beehivesResponse = NetworkModule.apiService.getBeehives(
                                                    url = "https://apisferoweb.it/api/v4/APIGetBeehives",
                                                    token = token
                                                )
                                                if (beehivesResponse.isSuccessful) {
                                                    val list = beehivesResponse.body()
                                                    if (!list.isNullOrEmpty()) {
                                                        beehiveList = list
                                                        selectedBeehive = list.first()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("BeeVS", "Failed to fetch beehives", e)
                                            }

                                            currentScreen = "home"
                                            Toast.makeText(this@MainActivity, "Benvenuto ${response.body()?.username}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = response.body()?.message ?: "Credenziali non valide"
                                            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Errore di rete: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoggingIn = false
                                    }
                                }
                            }
                        )

                        "home" -> HomeScreen(
                            selectedBeehive = selectedBeehive,
                            scale = scale,
                            versionName = versionName,
                            versionCode = versionCode,
                            beehiveList = beehiveList,
                            onInternalCamera = { currentScreen = "internal_camera" },
                            onExternalCamera = {
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        android.Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                                    val photoFile = createPhotoFile(this@MainActivity)
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${applicationContext.packageName}.fileprovider",
                                        photoFile
                                    )
                                    tempExternalUri = uri
                                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    externalCameraLauncher.launch(intent)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            onGallery = { currentScreen = "gallery" },
                            onUpdateSettings = { newBeehive: Beehive, newScale: Double ->
                                selectedBeehive = newBeehive
                                scale = newScale
                            }
                        )

                        "internal_camera" -> InternalCameraScreen(
                            beehiveLabel = selectedBeehive.label,
                            scale = scale,
                            onPhotoCaptured = { uri, _, _ ->
                                // Instead of uploading, we set the pending URI and go back to home
                                pendingUploadUri = uri
                                currentScreen = "home"
                            },
                            onBack = { currentScreen = "home" }
                        )

                        "gallery" -> GalleryScreen(
                            onPhotoSelected = { uri ->
                                currentScreen = "home"
                                pendingUploadUri = uri
                            },
                            onBack = { currentScreen = "home" }
                        )
                    }
                }

                // Confirmation Dialog
                if (pendingUploadUri != null) {
                    AlertDialog(
                        onDismissRequest = { pendingUploadUri = null },
                        title = { Text("Conferma Invio") },
                        text = { Text("Vuoi inviare la foto al server?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val uri = pendingUploadUri!!
                                    pendingUploadUri = null
                                    isUploading = true
                                    uploadPhoto(
                                        context = this@MainActivity,
                                        uri = uri,
                                        beehive = selectedBeehive,
                                        scale = scale,
                                        onComplete = { isUploading = false }
                                    )
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
                    // Loading Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable(enabled = false) {} // Block interactions
                            .zIndex(10f), // Ensure it's on top
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = YellowPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Invio in corso...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                        val message = response.body()?.string() ?: "Upload OK"
                        Toast.makeText(
                            context,
                            "Upload completato: $message",
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
