package com.ai.phoneagent.net

import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
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

    data class UserInfoResult(
        val success: Boolean,
        val displayName: String = "",
        val accountInfo: String = "",
        val message: String = "",
    )

    fun requestApiKey(providerAccessToken: String): ApiKeyResult {
        val token = providerAccessToken.trim()
        if (token.isBlank()) {
            return ApiKeyResult(success = false, message = "AI Ping 访问令牌为空")
        }

        return requestApiKeyList(token)
    }

    fun requestUserInfo(providerAccessToken: String): UserInfoResult {
        val token = providerAccessToken.trim()
        if (token.isBlank()) {
            return UserInfoResult(success = false, message = "AI Ping 访问令牌为空")
        }
        val url =
            Uri.parse(USER_INFO_URL)
                .buildUpon()
                .appendQueryParameter("access_token", token)
                .build()
                .toString()
        val request = Request.Builder().url(url).header("Accept", "*/*").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return UserInfoResult(
                        success = false,
                        message = parseErrorMessage(raw).ifBlank { "userinfo HTTP ${response.code}" },
                    )
                }
                val root = json.parseToJsonElement(raw).jsonObject
                val obj = responseContainer(root)
                val displayName =
                    firstText(
                        obj,
                        "another_name",
                        "nickname",
                        "username",
                        "name",
                        "short_phone_number",
                        "phone",
                    ).ifBlank { "AI Ping" }
                val accountInfo =
                    buildList {
                        addField("昵称", firstText(obj, "another_name", "nickname", "name"))
                        addField("用户名", firstText(obj, "username"))
                        addField("手机号", firstText(obj, "short_phone_number", "phone_number", "phone"))
                        addField("邮箱", firstText(obj, "email"))
                        addBalanceFields(obj)
                        addField("用户 ID", firstText(obj, "sub", "user_id", "userId", "id"))
                    }.distinct().joinToString("\n")
                UserInfoResult(
                    success = true,
                    displayName = displayName,
                    accountInfo = accountInfo.ifBlank { displayName },
                )
            }
        } catch (e: IOException) {
            UserInfoResult(success = false, message = e.message.orEmpty().ifBlank { "账户信息请求失败" })
        } catch (e: SerializationException) {
            UserInfoResult(success = false, message = e.message.orEmpty().ifBlank { "账户信息响应解析失败" })
        } catch (e: IllegalArgumentException) {
            UserInfoResult(success = false, message = e.message.orEmpty().ifBlank { "账户信息请求参数无效" })
        }
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
                val container = responseContainer(obj)
                val apiKey =
                    container["apikeyBaseInfo"]
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

    private fun firstText(
        obj: JsonObject,
        vararg keys: String,
    ): String =
        keys.firstNotNullOfOrNull { key ->
            (obj[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()

    private fun responseContainer(root: JsonObject): JsonObject =
        root["data"] as? JsonObject ?: root

    private fun MutableList<String>.addBalanceFields(obj: JsonObject) {
        val pointRemain = firstText(obj, "point_remain")
        val rechargeRemain = firstText(obj, "recharge_remain")
        val total = sumRemain(pointRemain, rechargeRemain)
        addField("余额", total)
        addField("赠送余额", formatRemain(pointRemain))
        addField("充值余额", formatRemain(rechargeRemain))
    }

    private fun sumRemain(pointRemain: String, rechargeRemain: String): String {
        val total =
            listOf(pointRemain, rechargeRemain)
                .mapNotNull { parseRemain(it) }
                .takeIf { it.isNotEmpty() }
                ?.fold(BigDecimal.ZERO, BigDecimal::add)
                ?: return ""
        return "${formatDecimal(total)} 算力点"
    }

    private fun formatRemain(value: String): String =
        parseRemain(value)?.let { "${formatDecimal(it)} 算力点" }.orEmpty()

    private fun parseRemain(value: String): BigDecimal? =
        value
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                }.getOrNull()
            }

    private fun formatDecimal(value: BigDecimal): String {
        val normalized = value.stripTrailingZeros()
        return if (normalized.scale() < 0) {
            normalized.setScale(0).toPlainString()
        } else {
            normalized.toPlainString()
        }
    }

    private fun MutableList<String>.addField(label: String, value: String) {
        if (value.isNotBlank()) add("$label：$value")
    }
}
