package com.bvs.smart.network

import com.bvs.smart.data.DeviceCapabilities
import com.bvs.smart.data.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    @POST
    suspend fun sendDeviceCapabilities(
        @Url url: String,
        @Query("api-key") apiKey: String?,
        @Body payload: DeviceCapabilities
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun uploadPhoto(
        @Url url: String,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}
