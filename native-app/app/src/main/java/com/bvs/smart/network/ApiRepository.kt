package com.bvs.smart.network

import com.bvs.smart.data.ResourceOwner
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response

class ApiRepository(
    private val authManager: AuthManager,
    baseUrl: String
) {

    private val normalizedBaseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    private val api = NetworkModule.createApiService(normalizedBaseUrl)

    private companion object {
        private const val DEFAULT_SCANNER = "SCANNER_APP"
    }
    private val getResourcesUrl = "${normalizedBaseUrl}get-resources"
    private val uploadImageUrl = "${normalizedBaseUrl}APIUploadImage"
    private val syncCapabilitiesUrl = "${normalizedBaseUrl}APISyncDeviceCapabilities"

    suspend fun getResources(username: String, password: String): Response<List<ResourceOwner>> {
        val params = mapOf(
            "username" to username.toRequestBody(MultipartBody.FORM),
            "password" to password.toRequestBody(MultipartBody.FORM),
            "scanner" to DEFAULT_SCANNER.toRequestBody(MultipartBody.FORM)
        )

        val response = api.getResources(
            url = getResourcesUrl,
            parts = params
        )

        if (response.isSuccessful) {
            authManager.saveCredentials(username, password)
            response.body()?.let { owners ->
                authManager.saveResources(owners)
            }
        }

        return response
    }

    suspend fun uploadFoto(
        params: Map<String, RequestBody>,
        file: MultipartBody.Part
    ): Response<ResponseBody> {
        return api.uploadPhoto(
            url = uploadImageUrl,
            parts = params,
            file = file
        )
    }

    // Capability sync is currently disabled; retain the call for future reuse.
    /*
    suspend fun syncCapabilities(payload: com.bvs.smart.data.DeviceCapabilities): Response<ResponseBody> {
        return api.sendDeviceCapabilities(
            url = syncCapabilitiesUrl,
            apiKey = null,
            payload = payload
        )
    }
    */
}
