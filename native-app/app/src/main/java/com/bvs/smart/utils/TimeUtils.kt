package com.bvs.smart.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object TimeUtils {

    fun getRelativeTimeDisplay(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null

        try {
            // ISO 8601 format (e.g., 2025-12-29T08:55:00.000000Z)
            // Note: The input might have variable precision for fractional seconds or timezone 'Z'
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC") // Assuming server sends UTC

            // Handle potential fractional seconds by trimming or using a more flexible parser if needed
            // For simplicity, we parse the first 19 chars if the string is long enough
            val cleanDateString = if (dateString.length >= 19) dateString.substring(0, 19) else dateString
            
            val date = format.parse(cleanDateString) ?: return null
            val now = Date()
            
            val diffMillis = now.time - date.time
            // If the date is in the future (e.g. server clock drift), treat as "just now"
            if (diffMillis < 0) return "un attimo fa"

            val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                diffMin < 1 -> "un attimo fa"
                diffMin < 60 -> "$diffMin min"
                diffHours < 24 -> "$diffHours h"
                diffDays <= 3 -> "$diffDays gg"
                else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple date part if parsing fails
            return dateString.substringBefore('T')
        }
    }
}
