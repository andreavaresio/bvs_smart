package com.bvs.smart.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bvs.smart.data.ResourceOwner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AuthManager(context: Context) {

    private val gson = Gson()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(username: String, pass: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER, username)
            putString(KEY_PASS, pass)
            apply()
        }
    }

    fun getUsername(): String? = sharedPreferences.getString(KEY_USER, null)
    fun getPassword(): String? = sharedPreferences.getString(KEY_PASS, null)

    fun saveResources(resources: List<ResourceOwner>) {
        val json = gson.toJson(resources)
        sharedPreferences.edit()
            .putString(KEY_RESOURCES, json)
            .apply()
    }

    fun getCachedResources(): List<ResourceOwner> {
        val json = sharedPreferences.getString(KEY_RESOURCES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<ResourceOwner>>() {}.type
            gson.fromJson<List<ResourceOwner>>(json, type)
        }.getOrDefault(emptyList())
    }

    fun saveSelection(apiaryName: String?, hiveCode: String?, scale: Double) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_APIARY_NAME, apiaryName)
            .putString(KEY_SELECTED_HIVE_CODE, hiveCode)
            .putFloat(KEY_SELECTED_SCALE, scale.toFloat())
            .apply()
    }

    fun saveScanSettings(
        scale: Double,
        permanenceDays: Int,
        measureType: String,
        photosPerScan: Int
    ) {
        sharedPreferences.edit()
            .putFloat(KEY_SELECTED_SCALE, scale.toFloat())
            .putInt(KEY_PERMANENCE_DAYS, permanenceDays)
            .putString(KEY_MEASURE_TYPE, measureType)
            .putInt(KEY_PHOTOS_PER_SCAN, photosPerScan)
            .apply()
    }

    fun loadSelection(): SelectionSnapshot {
        val scale = if (sharedPreferences.contains(KEY_SELECTED_SCALE)) {
            sharedPreferences.getFloat(KEY_SELECTED_SCALE, DEFAULT_SCALE.toFloat()).toDouble()
        } else {
            DEFAULT_SCALE
        }
        return SelectionSnapshot(
            apiaryName = sharedPreferences.getString(KEY_SELECTED_APIARY_NAME, null),
            hiveCode = sharedPreferences.getString(KEY_SELECTED_HIVE_CODE, null),
            scale = scale
        )
    }

    fun loadScanSettings(): ScanSettings {
        return ScanSettings(
            scale = sharedPreferences.getFloat(KEY_SELECTED_SCALE, DEFAULT_SCALE.toFloat()).toDouble(),
            permanenceDays = sharedPreferences.getInt(KEY_PERMANENCE_DAYS, 1),
            measureType = sharedPreferences.getString(KEY_MEASURE_TYPE, "CadutaNaturale") ?: "CadutaNaturale",
            photosPerScan = sharedPreferences.getInt(KEY_PHOTOS_PER_SCAN, 1)
        )
    }

    fun hasCredentials(): Boolean = getUsername() != null && getPassword() != null

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    data class SelectionSnapshot(
        val apiaryName: String?,
        val hiveCode: String?,
        val scale: Double?
    )

    data class ScanSettings(
        val scale: Double,
        val permanenceDays: Int,
        val measureType: String,
        val photosPerScan: Int
    )

    private companion object {
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
        private const val KEY_RESOURCES = "resources"
        private const val KEY_SELECTED_APIARY_NAME = "selected_apiary_name"
        private const val KEY_SELECTED_HIVE_CODE = "selected_hive_code"
        private const val KEY_SELECTED_SCALE = "selected_scale"
        
        private const val KEY_PERMANENCE_DAYS = "permanence_days"
        private const val KEY_MEASURE_TYPE = "measure_type"
        private const val KEY_PHOTOS_PER_SCAN = "photos_per_scan"
        
        private const val DEFAULT_SCALE = 1.0
    }
}
