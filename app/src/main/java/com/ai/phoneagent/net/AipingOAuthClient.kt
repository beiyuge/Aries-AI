package com.ai.phoneagent.net

import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object AipingOAuthClient {
    private const val BASE_URL = "https://central.qc-ai.cn"
    private const val API_KEY_LIST_URL = "$BASE_URL/api/v1/oauth/apikey/list"
    private const val USER_INFO_URL = "$BASE_URL/api/v1/oauth/userinfo"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    data class ApiKeyResult(
        val success: Boolean,
        val apiKey: String = "",
        val displayName: String = "",
        val message: String = "",
    )

    fun requestApiKey(providerAccessToken: String): ApiKeyResult {
        val token = providerAccessToken.trim()
        if (token.isBlank()) {
            return ApiKeyResult(success = false, message = "AI Ping 访问令牌为空")
        }

        val apiKeyResult = requestApiKeyList(token)
        if (!apiKeyResult.success) return apiKeyResult

        return apiKeyResult.copy(
            displayName = requestDisplayName(token).ifBlank { "AI Ping" },
        )
    }

    private fun requestApiKeyList(accessToken: String): ApiKeyResult {
        val url =
            Uri.parse(API_KEY_LIST_URL)
                .buildUpon()
                .appendQueryParameter("access_token", accessToken)
                .build()
                .toString()
        val request = Request.Builder().url(url).header("Accept", "*/*").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiKeyResult(
                        success = false,
                        message = parseErrorMessage(raw).ifBlank { "apikey/list HTTP ${response.code}" },
                    )
                }
                val obj = json.parseToJsonElement(raw).jsonObject
                val apiKey =
                    obj["apikeyBaseInfo"]
                        ?.jsonArray
                        ?.mapNotNull { item ->
                            item.jsonObject["apikey"]?.jsonPrimitive?.contentOrNull?.trim()
                        }
                        ?.firstOrNull { it.isNotBlank() }
                        .orEmpty()
                if (apiKey.isBlank()) {
                    ApiKeyResult(success = false, message = "AI Ping 账号下没有可用 API Key")
                } else {
                    ApiKeyResult(success = true, apiKey = apiKey)
                }
            }
        } catch (e: IOException) {
            ApiKeyResult(success = false, message = e.message.orEmpty().ifBlank { "API Key 请求失败" })
        } catch (e: SerializationException) {
            ApiKeyResult(success = false, message = e.message.orEmpty().ifBlank { "API Key 响应解析失败" })
        } catch (e: IllegalArgumentException) {
            ApiKeyResult(success = false, message = e.message.orEmpty().ifBlank { "API Key 请求参数无效" })
        }
    }

    private fun requestDisplayName(accessToken: String): String {
        val url =
            Uri.parse(USER_INFO_URL)
                .buildUpon()
                .appendQueryParameter("access_token", accessToken)
                .build()
                .toString()
        val request = Request.Builder().url(url).header("Accept", "*/*").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return ""
                val obj = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                obj["another_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: obj["short_phone_number"]?.jsonPrimitive?.contentOrNull.orEmpty()
            }
        } catch (e: IOException) {
            ""
        } catch (e: SerializationException) {
            ""
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    private fun parseErrorMessage(raw: String): String {
        if (raw.isBlank()) return ""
        val message = runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj["message"]?.jsonPrimitive?.contentOrNull
                ?: obj["error_description"]?.jsonPrimitive?.contentOrNull
                ?: obj["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()?.trim().orEmpty().ifBlank { raw.trim() }
        return if (message.contains("your platform has no permission", ignoreCase = true)) {
            "AI Ping 平台未开通 API Key 列表权限，请联系 AI Ping 为当前 OAuth 平台开通 /oauth/apikey/list 资源权限"
        } else {
            message
        }
    }
}
