package com.bvs.smart.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE_NAME = "app_log.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun i(tag: String, message: String) {
        write("INFO", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.stackTraceToString() ?: ""
        write("ERROR", tag, "$message $stackTrace")
    }

    fun d(tag: String, message: String) {
        write("DEBUG", tag, message)
    }

    private fun write(level: String, tag: String, message: String) {
        // Log to Android Logcat as well
        when (level) {
            "INFO" -> Log.i(tag, message)
            "ERROR" -> Log.e(tag, message)
            "DEBUG" -> Log.d(tag, message)
        }

        scope.launch {
            try {
                appendLog(level, tag, message)
            } catch (e: Exception) {
                Log.e("LogManager", "Failed to write log", e)
            }
        }
    }
    
    // Internal file reference
    private var logFile: File? = null

    fun init(context: Context) {
        if (logFile == null) {
            val dir = File(context.getExternalFilesDir(null), LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            logFile = File(dir, LOG_FILE_NAME)
            i("LogManager", "Logger initialized at ${logFile?.absolutePath}")
        }
    }

    fun getLogFile(): File? = logFile

    fun appendLog(level: String, tag: String, message: String) {
        val file = logFile ?: return
        try {
            val timestamp = dateFormat.format(Date())
            val line = "$timestamp [$level/$tag]: $message\n"
            
            // Synchronized to prevent concurrent write issues
            synchronized(this) {
                FileWriter(file, true).use { fw ->
                    PrintWriter(fw).use { pw ->
                        pw.print(line)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Error writing to file", e)
        }
    }
}

