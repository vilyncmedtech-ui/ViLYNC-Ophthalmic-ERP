package com.vilync.ophthalmicerp.feature.master.party.gst

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class GstinLookupService(
    private val apiKey: String
) {

    companion object {
        private const val TAG = "GST_LOOKUP"
        private const val BASE_URL =
            "https://gstverify.co.in/api/v1/verify/"
    }


    // =========================================================
    // VERIFY GSTIN
    // =========================================================

    suspend fun verifyGstin(
        gstin: String
    ): com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse =
        withContext(Dispatchers.IO) {

            var connection: HttpURLConnection? = null

            try {
                Log.d(TAG, "GST API Key present: ${apiKey.isNotBlank()}")

                val cleanGstin =
                    gstin
                        .trim()
                        .uppercase()

                Log.d(TAG, "Request GSTIN: $cleanGstin")

                if (cleanGstin.length != 15) {
                    return@withContext com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(
                        success = false,
                        message =
                            "Enter a valid 15-character GSTIN."
                    )
                }


                if (apiKey.isBlank()) {
                    return@withContext com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(
                        success = false,
                        message =
                            "GST API key is not configured."
                    )
                }


                val url =
                    URL(
                        BASE_URL +
                                cleanGstin
                    )

                Log.d(TAG, "Request URL: $url")

                connection =
                    url.openConnection()
                            as HttpURLConnection


                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    15000

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "X-API-Key",
                    apiKey
                )


                val responseCode =
                    connection.responseCode

                Log.d(TAG, "HTTP Response Code: $responseCode")

                val inputStream =
                    if (
                        responseCode in 200..299
                    ) {

                        connection.inputStream

                    } else {

                        connection.errorStream
                    }


                val responseText =
                    inputStream
                        ?.let { stream ->

                            BufferedReader(
                                InputStreamReader(
                                    stream
                                )
                            ).use { reader ->

                                reader.readText()
                            }
                        }
                        .orEmpty()

                Log.d(TAG, "Raw Response Body: $responseText")

                if (responseText.isBlank()) {
                    return@withContext com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(
                        success = false,
                        message =
                            "GST server returned an empty response."
                    )
                }


                parseResponse(
                    responseText = responseText,
                    responseCode = responseCode
                )


            } catch (exception: Exception) {
                Log.e(TAG, "GST Lookup Exception: ${exception.message}", exception)
                com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(
                    success = false,

                    message =
                        exception.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Unable to connect to GST service."
                )


            } finally {

                connection?.disconnect()
            }
        }


    // =========================================================
    // PARSE JSON RESPONSE
    // =========================================================

    private fun parseResponse(
        responseText: String,
        responseCode: Int
    ): com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse {

        return try {

            val root =
                JSONObject(
                    responseText
                )


            val success =
                root.optBoolean(
                    "success",
                    responseCode in 200..299
                )


            val cached =
                root.optBoolean(
                    "cached",
                    false
                )


            val creditsRemaining =
                if (
                    root.has(
                        "credits_remaining"
                    ) &&
                    !root.isNull(
                        "credits_remaining"
                    )
                ) {

                    root.optInt(
                        "credits_remaining"
                    )

                } else {

                    null
                }


            val message =
                root.optString(
                    "message"
                ).takeIf {
                    it.isNotBlank()
                }


            val dataObject =
                root.optJSONObject(
                    "data"
                )


            val data =
                dataObject?.let {

                    val apiData = com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiData(

                        gstin =
                            it.stringOrNull(
                                "gstin"
                            ),

                        legal_name =
                            it.stringOrNull(
                                "legal_name"
                            ),

                        trade_name =
                            it.stringOrNull(
                                "trade_name"
                            ),

                        status =
                            it.stringOrNull(
                                "status"
                            ),

                        constitution =
                            it.stringOrNull(
                                "constitution"
                            ),

                        taxpayer_type =
                            it.stringOrNull(
                                "taxpayer_type"
                            ),

                        registration_date =
                            it.stringOrNull(
                                "registration_date"
                            ),

                        state =
                            it.stringOrNull(
                                "state"
                            ),

                        pan =
                            it.stringOrNull(
                                "pan"
                            ),

                        address =
                            it.stringOrNull(
                                "address"
                            ),

                        nature_of_business =
                            it.stringListOrNull(
                                "nature_of_business"
                            )
                    )
                    
                    Log.d(TAG, "Parsed Name: ${apiData.legal_name}")
                    Log.d(TAG, "Parsed GSTIN: ${apiData.gstin}")
                    Log.d(TAG, "Parsed Address: ${apiData.address}")
                    
                    apiData
                }


            com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(

                success =
                    success &&
                            data != null,

                cached =
                    cached,

                credits_remaining =
                    creditsRemaining,

                data =
                    data,

                message =
                    message
                        ?: if (
                            responseCode !in 200..299
                        ) {
                            "GST lookup failed. HTTP $responseCode"
                        } else {
                            null
                        }
            )


        } catch (exception: Exception) {
            Log.e(TAG, "JSON Parsing Exception: ${exception.message}")
            com.vilync.ophthalmicerp.feature.master.party.gst.GstinApiResponse(

                success = false,

                message =
                    "Unable to read GST server response."
            )
        }
    }


    // =========================================================
    // JSON HELPERS
    // =========================================================

    private fun JSONObject.stringOrNull(
        key: String
    ): String? {

        if (
            !has(key) ||
            isNull(key)
        ) {
            return null
        }

        return optString(
            key
        )
            .trim()
            .takeIf {
                it.isNotBlank()
            }
    }


    private fun JSONObject.stringListOrNull(
        key: String
    ): List<String>? {

        val array =
            optJSONArray(
                key
            )
                ?: return null


        val result =
            mutableListOf<String>()


        for (
        index in 0 until array.length()
        ) {

            val value =
                array.optString(
                    index
                )
                    .trim()


            if (value.isNotBlank()) {

                result.add(
                    value
                )
            }
        }


        return result
            .takeIf {
                it.isNotEmpty()
            }
    }
}
