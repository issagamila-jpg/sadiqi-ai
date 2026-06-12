package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or invalid: $apiKey")
            return@withContext "خطأ: مفتاح API (Gemini API Key) غير متوفر. يرجى إضافته في لوحة أسرار AI Studio (Secrets Manager)."
        }

        try {
            val rootJson = JSONObject()

            // Contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // System instruction if available
            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                rootJson.put("systemInstruction", sysInstObj)
            }

            // Generation config
            val configObj = JSONObject()
            configObj.put("temperature", 0.7)
            rootJson.put("generationConfig", configObj)

            val requestBody = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: $bodyString"
                    Log.e(TAG, errMsg)
                    return@withContext "السماح بالولوج: عذراً، وقع خطأ في الاتصال بالذكاء الاصطناعي ($errMsg)"
                }

                if (bodyString.isNullOrEmpty()) {
                    return@withContext "عذراً، لم أستطع استلام إجابة فارغة من المساعد."
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "لا يوجد نص متاح.")
                        }
                    }
                }
                "عذراً، لم أتوصل برد واضح من الخادم."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini: ", e)
            "وقع خطأ أثناء الاتصال: ${e.localizedMessage}"
        }
    }
}
