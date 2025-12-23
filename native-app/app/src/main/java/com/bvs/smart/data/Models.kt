package com.bvs.smart.data

import com.google.gson.annotations.SerializedName

data class ResourceOwner(
    @SerializedName("proprietario") val ownerName: String,
    @SerializedName("codice_bda") val bdaCode: String,
    @SerializedName("apiari") val apiaries: List<Apiary>
)

data class Apiary(
    @SerializedName("apiaro_nome") val name: String,
    @SerializedName("localita") val location: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("alveari") val hives: List<Arnia>
) {
    var ownerName: String? = null
}

data class Arnia(
    @SerializedName("nome") val name: String,
    @SerializedName("codice") val code: String,
    @SerializedName("data_prelievo") val lastSampleDate: String?
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
