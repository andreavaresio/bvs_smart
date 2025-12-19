package com.bvs.smart.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.camera.lifecycle.ProcessCameraProvider
import com.bvs.smart.data.CameraSummary
import com.bvs.smart.data.DeviceCapabilities
import com.bvs.smart.data.PhotoResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ExecutionException

class DeviceManager(private val context: Context) {

    suspend fun getDeviceCapabilities(): DeviceCapabilities = withContext(Dispatchers.IO) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val platform = mapOf(
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "model" to Build.MODEL,
            "sdk" to Build.VERSION.SDK_INT,
            "version" to Build.VERSION.RELEASE
        )

        // Camera Info using CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var backCamera: CameraSummary? = null
        var frontCamera: CameraSummary? = null

        try {
            val cameraProvider = cameraProviderFuture.get()
            val cameraInfos = cameraProvider.availableCameraInfos
            
            cameraInfos.forEach { info ->
                val isBack = info.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK
                val summary = CameraSummary(
                    id = info.toString(), // Simplified ID
                    name = if (isBack) "Back Camera" else "Front Camera",
                    position = if (isBack) "back" else "front",
                    hasFlash = info.hasFlashUnit(),
                    hasTorch = info.hasFlashUnit(),
                    photoResolutions = emptyList() // Getting exact resolutions requires binding or Camera2 extensions
                )
                
                if (isBack) backCamera = summary else frontCamera = summary
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        DeviceCapabilities(
            deviceid = deviceId,
            timestamp = timestamp,
            appVersion = appVersion,
            platform = platform,
            supportsLowLightBoost = mapOf("supported" to false),
            frontCamera = frontCamera,
            backCamera = backCamera,
            photoResolutions = listOf(PhotoResolution(1920, 1080, 2.07)) // Placeholder or fetched from specific use cases
        )
    }

    // Capability sync is disabled for now; keep the implementation for future reuse.
    /*
    suspend fun syncCapabilities() {
        try {
            val capabilities = getDeviceCapabilities()
            val service = NetworkModule.createApiService("https://apisferoweb.it/api/v4/")
            val response = service.sendDeviceCapabilities(
                url = "https://apisferoweb.it/api/v4/APISyncDeviceCapabilities",
                apiKey = null,
                payload = capabilities
            )
            if (response.isSuccessful) {
                android.util.Log.d("DeviceManager", "Capabilities synced successfully")
            } else {
                android.util.Log.e("DeviceManager", "Sync failed: ${response.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceManager", "Error syncing capabilities", e)
        }
    }
    */
}
