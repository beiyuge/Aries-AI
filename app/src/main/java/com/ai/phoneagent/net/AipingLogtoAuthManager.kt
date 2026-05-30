package com.ai.phoneagent.net

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.webkit.CookieManager
import com.ai.phoneagent.BuildConfig
import io.logto.sdk.android.LogtoClient
import io.logto.sdk.android.constant.StorageKey
import io.logto.sdk.android.extension.oidcConfigEndpoint
import io.logto.sdk.android.storage.PersistStorage
import io.logto.sdk.android.type.LogtoConfig
import io.logto.sdk.core.Core
import io.logto.sdk.core.constant.UserScope
import io.logto.sdk.core.type.OidcConfigResponse
import io.logto.sdk.core.util.GenerateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class AipingLogtoAuthManager(private val application: Application) {
    data class AuthResult(
        val success: Boolean,
        val apiKey: String = "",
        val displayName: String = "",
        val message: String = "",
    )

    private data class DirectSignInResult(
        val success: Boolean,
        val accessToken: String = "",
        val message: String = "",
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    private val logtoConfig =
        LogtoConfig(
            endpoint = BuildConfig.ARIES_LOGTO_ENDPOINT,
            appId = BuildConfig.ARIES_LOGTO_APP_ID,
            scopes = listOf(
                UserScope.PROFILE,
                UserScope.EMAIL,
                UserScope.PHONE,
                UserScope.IDENTITIES,
            ),
            resources = null,
            usingPersistStorage = true,
        )
    private val logtoClient =
        LogtoClient(
            logtoConfig,
            application,
        )

    suspend fun signInAndGetApiKey(activity: Activity): AuthResult {
        if (BuildConfig.AIPING_LOGTO_CONNECTOR_TARGET.isBlank()) {
            return AuthResult(success = false, message = "AI Ping Logto connector target 未配置")
        }
        val signInResult = signInDirectly(activity)
        if (!signInResult.success) {
            return AuthResult(success = false, message = signInResult.message)
        }
        val logtoAccessToken = signInResult.accessToken
        if (logtoAccessToken.isBlank()) {
            return AuthResult(success = false, message = "Logto 访问令牌获取失败")
        }
        val aipingAccessToken =
            withContext(Dispatchers.IO) { fetchAipingAccessToken(logtoAccessToken) }
                .getOrElse { error ->
                    return AuthResult(
                        success = false,
                        message = "Logto Secret Vault 令牌读取失败：${cleanErrorMessage(error.message)}",
                    )
                }
        val apiKeyResult =
            withContext(Dispatchers.IO) {
                AipingOAuthClient.requestApiKey(aipingAccessToken)
            }
        return if (apiKeyResult.success) {
            AuthResult(
                success = true,
                apiKey = apiKeyResult.apiKey,
                displayName = apiKeyResult.displayName,
            )
        } else {
            AuthResult(
                success = false,
                message = "AI Ping API Key 换取失败：${cleanErrorMessage(apiKeyResult.message)}",
            )
        }
    }

    suspend fun signOut(): String? =
        suspendCancellableCoroutine { continuation ->
            val finish: (String?) -> Unit = { message ->
                clearLogtoStorage()
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
                if (continuation.isActive) {
                    continuation.resume(message)
                }
            }
            if (!logtoClient.isAuthenticated) {
                finish(null)
            } else {
                logtoClient.signOut { error -> finish(error?.message) }
            }
        }

    private suspend fun signInDirectly(activity: Activity): DirectSignInResult {
        val oidcConfig = fetchOidcConfig()
            ?: return DirectSignInResult(success = false, message = "Logto OIDC 配置获取失败")
        val codeVerifier = GenerateUtils.generateCodeVerifier()
        val state = GenerateUtils.generateState()
        val directSignInUri =
            try {
                Core.generateSignInUri(
                    authorizationEndpoint = oidcConfig.authorizationEndpoint,
                    clientId = logtoConfig.appId,
                    redirectUri = BuildConfig.ARIES_LOGTO_REDIRECT_URI,
                    codeChallenge = GenerateUtils.generateCodeChallenge(codeVerifier),
                    state = state,
                    scopes = logtoConfig.scopes,
                    resources = logtoConfig.resources,
                    prompt = logtoConfig.prompt,
                )
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter(
                        DIRECT_SIGN_IN_QUERY,
                        "$DIRECT_SIGN_IN_SOCIAL_PREFIX${BuildConfig.AIPING_LOGTO_CONNECTOR_TARGET}",
                    )
                    .build()
                    .toString()
            } catch (e: IllegalArgumentException) {
                return DirectSignInResult(success = false, message = e.message.orEmpty().ifBlank { "AI Ping Direct sign-in URL 生成失败" })
            }

        return suspendCancellableCoroutine { continuation ->
            AipingDirectLogtoActivity.pendingSession =
                AipingDirectLogtoActivity.PendingSession(
                    oidcConfig = oidcConfig,
                    clientId = logtoConfig.appId,
                    redirectUri = BuildConfig.ARIES_LOGTO_REDIRECT_URI,
                    codeVerifier = codeVerifier,
                    state = state,
                    storageName = logtoStorageName(),
                ) { result ->
                    if (continuation.isActive) {
                        continuation.resume(
                            DirectSignInResult(
                                success = result.success,
                                accessToken = result.accessToken,
                                message = result.message,
                            ),
                        )
                    }
                }
            continuation.invokeOnCancellation {
                AipingDirectLogtoActivity.pendingSession = null
            }
            activity.startActivity(
                Intent(activity, AipingDirectLogtoActivity::class.java)
                    .putExtra(AipingDirectLogtoActivity.EXTRA_AUTH_URL, directSignInUri),
            )
        }
    }

    private suspend fun fetchOidcConfig(): OidcConfigResponse? =
        suspendCancellableCoroutine { continuation ->
            Core.fetchOidcConfig(logtoConfig.oidcConfigEndpoint) { error, response ->
                if (continuation.isActive) {
                    continuation.resume(if (error == null) response else null)
                }
            }
        }

    private fun clearLogtoStorage() {
        val storage = PersistStorage(application, logtoStorageName())
        storage.setItem(StorageKey.REFRESH_TOKEN, null)
        storage.setItem(StorageKey.ID_TOKEN, null)
    }

    private fun logtoStorageName(): String =
        "${StorageKey.STORAGE_NAME_PREFIX}:${logtoConfig.appId}"

    private fun fetchAipingAccessToken(logtoAccessToken: String): Result<String> {
        val endpoint = BuildConfig.ARIES_LOGTO_ENDPOINT.trimEnd('/')
        val target = BuildConfig.AIPING_LOGTO_CONNECTOR_TARGET
        val url =
            endpoint.toHttpUrl()
                .newBuilder()
                .addPathSegments("api/my-account/identities")
                .addPathSegment(target)
                .addPathSegment("access-token")
                .build()
        val request =
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $logtoAccessToken")
                .header("Accept", "application/json")
                .get()
                .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        IOException(parseErrorMessage(raw).ifBlank { "Logto Account API HTTP ${response.code}" }),
                    )
                }
                val obj = json.parseToJsonElement(raw).jsonObject
                val accessToken = obj["access_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (accessToken.isBlank()) {
                    Result.failure(IllegalArgumentException("Logto Account API 响应缺少 access_token"))
                } else {
                    Result.success(accessToken)
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SerializationException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    private companion object {
        const val DIRECT_SIGN_IN_QUERY = "direct_sign_in"
        const val DIRECT_SIGN_IN_SOCIAL_PREFIX = "social:"
    }

    private fun cleanErrorMessage(message: String?): String =
        message?.trim().orEmpty().ifBlank { "未知错误" }

    private fun parseErrorMessage(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj["message"]?.jsonPrimitive?.contentOrNull
                ?: obj["error_description"]?.jsonPrimitive?.contentOrNull
                ?: obj["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()?.trim().orEmpty().ifBlank { raw.trim() }
    }
}
