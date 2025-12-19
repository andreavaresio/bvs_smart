package com.bvs.smart.data

data class Beehive(
    val id: String,
    val label: String
)

val BEEHIVES = listOf(
    Beehive("IT-de11cede-1c18-4bd3-a383-a5349ac757a9", "iPhone"),
    Beehive("IT-e6aa3784-6c9b-4116-af7b-228fa8bbe30d", "motoroloa g82"),
    Beehive("IT-a2cb09a4-eac0-4d4e-906e-894e74eb4fcd", "thinkphone")
)

data class DeviceCapabilities(
    val deviceid: String,
    val timestamp: String,
    val appVersion: String,
    val platform: Map<String, Any?>,
    val supportsLowLightBoost: Map<String, Any?>,
    val frontCamera: CameraSummary?,
    val backCamera: CameraSummary?,
    val photoResolutions: List<PhotoResolution>
)

data class CameraSummary(
    val id: String,
    val name: String?,
    val position: String?,
    val hasFlash: Boolean,
    val hasTorch: Boolean,
    val photoResolutions: List<PhotoResolution>
)

data class PhotoResolution(
    val width: Int,
    val height: Int,
    val megapixels: Double
)

data class UploadResponse(
    val message: String?
)

data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val token: String?,
    val userId: String?,
    val username: String?
)
