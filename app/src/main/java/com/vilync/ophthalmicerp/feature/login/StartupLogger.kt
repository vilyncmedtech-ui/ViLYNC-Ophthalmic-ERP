package com.vilync.ophthalmicerp.feature.login

import android.content.Context
import android.util.Log
import com.vilync.ophthalmicerp.feature.login.model.StartupFacts
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Permanent Startup Diagnostic Log.
 * Persists the last 100 startup decisions for forensic analysis.
 */
object StartupLogger {

    private const val TAG = "STARTUP_LOG"
    private const val LOG_FILE_NAME = "startup_diagnostics.log"
    private const val MAX_ENTRIES = 100

    fun logStartup(
        context: Context,
        appVersion: String,
        report: StartupFacts,
        destination: String,
        error: String? = null
    ) {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            val logArray = if (logFile.exists()) {
                try {
                    JSONArray(logFile.readText())
                } catch (_: Exception) {
                    JSONArray()
                }
            } else {
                JSONArray()
            }

            val entry = JSONObject().apply {
                put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("appVersion", appVersion)
                put("dbExists", report.fileExists)
                put("dbVersion", report.versionOnDisk)
                put("expectedVersion", report.expectedVersion)
                put("canOpen", report.canOpen)
                put("infraHealthy", report.infrastructureHealthy)
                put("adminExists", report.administratorExists)
                put("businessDataExists", report.businessDataExists)
                put("destination", destination)
                put("failureCode", report.failure?.code ?: JSONObject.NULL)
                put("error", error ?: report.failure?.message ?: JSONObject.NULL)
            }

            // Circular retention: Keep last 100
            val updatedList = mutableListOf<JSONObject>()
            for (i in 0 until logArray.length()) {
                updatedList.add(logArray.getJSONObject(i))
            }
            updatedList.add(entry)
            
            val finalArray = JSONArray()
            updatedList.takeLast(MAX_ENTRIES).forEach { finalArray.put(it) }

            logFile.writeText(finalArray.toString(2))
            Log.d(TAG, "Startup decision logged: $destination")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist startup log", e)
        }
    }

    fun getLogs(context: Context): String {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        return if (logFile.exists()) logFile.readText() else "[]"
    }
}
