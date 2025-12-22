package com.bvs.smart.network

import com.bvs.smart.utils.LogManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkModule {

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val fileLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        LogManager.i("Network", "--> ${request.method} ${request.url}")
        
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            LogManager.e("Network", "<-- FAILED: ${e.message}")
            throw e
        }

        LogManager.i("Network", "<-- ${response.code} ${response.message} (${request.url})")
        response
    }

    private val trustAllTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private val trustAllSslSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustAllTrustManager), SecureRandom())
    }.socketFactory

    // WARNING: This client trusts every certificate and hostname. Use only in controlled environments.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(fileLoggingInterceptor)
        .sslSocketFactory(trustAllSslSocketFactory, trustAllTrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createApiService(baseUrl: String): ApiService {
        val normalizedBaseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
