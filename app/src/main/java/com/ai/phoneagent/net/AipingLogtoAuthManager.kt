package com.ai.phoneagent.net

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.webkit.CookieManager
import com.ai.phoneagent.BuildConfig
import com.ai.phoneagent.data.preferences.AppPreferencesRepository
import io.logto.sdk.android.LogtoClient
import io.logto.sdk.android.constant.StorageKey
import io.logto.sdk.android.extension.oidcConfigEndpoint
import io.logto.sdk.android.storage.PersistStorage
import io.logto.sdk.android.type.LogtoConfig
import io.logto.sdk.core.Core
import io.logto.sdk.core.constant.UserScope
import io.logto.sdk.core.type.OidcConfigResponse
import io.logto.sdk.core.util.GenerateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class AipingLogtoAuthManager(
    private val application: Application,
    private val prefs: AppPreferencesRepository,
) {
    data class AuthResult(
        val success: Boolean,
        val apiKey: String = "",
        val displayName: String = "",
        val accountInfo: String = "",
        val message: String = "",
    )

    private data class DirectSignInResult(
        val success: Boolean,
        val accessToken: String = "",
        val apiKey: String = "",
        val webAccessToken: String = "",
        val displayName: String = "",
        val accountInfo: String = "",
        val message: String = "",
    )

    private data class ProviderTokenResult(
        val success: Boolean,
        val providerAccessToken: String = "",
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
        if (signInResult.webAccessToken.isNotBlank()) {
            prefs.setAipingWebAccessToken(signInResult.webAccessToken)
        }
        return if (signInResult.success && signInResult.apiKey.isNotBlank()) {
            AuthResult(
                success = true,
                apiKey = signInResult.apiKey,
                displayName = signInResult.displayName.ifBlank { "AI Ping" },
                accountInfo = signInResult.accountInfo,
            )
        } else {
            val message = cleanErrorMessage(signInResult.message)
            AuthResult(
                success = false,
                displayName = signInResult.displayName,
                accountInfo = signInResult.accountInfo,
                message =
                    if (message.contains("登录已过期")) {
                        message
                    } else {
                        "AI Ping API Key 获取失败：$message"
                    },
            )
        }
    }

    private suspend fun signInAndFetchProviderToken(activity: Activity): ProviderTokenResult {
        val firstResult = signInAndFetchProviderTokenOnce(activity)
        if (firstResult.success || !shouldRetryAipingAuthorization(firstResult.message)) {
            return firstResult
        }
        clearAipingAuthorizationSession()
        return signInAndFetchProviderTokenOnce(activity, forceLogin = true)
            .let { result ->
                if (result.success) {
                    result
                } else {
                    result.copy(
                        message =
                            buildString {
                                append("AI Ping 授权已重新发起，但 Secret Vault 仍读取失败：")
                                append(cleanErrorMessage(result.message))
                            },
                    )
                }
            }
    }

    private suspend fun signInAndFetchProviderTokenOnce(
        activity: Activity,
        forceLogin: Boolean = false,
    ): ProviderTokenResult {
        val signInResult = signInDirectly(activity, forceLogin)
        if (!signInResult.success) {
            return ProviderTokenResult(success = false, message = signInResult.message)
        }
        val logtoAccessToken = signInResult.accessToken
        if (logtoAccessToken.isBlank()) {
            return ProviderTokenResult(success = false, message = "Logto 访问令牌获取失败")
        }
        return withContext(Dispatchers.IO) { fetchAipingAccessToken(logtoAccessToken) }
            .fold(
                onSuccess = { token ->
                    ProviderTokenResult(success = true, providerAccessToken = token)
                },
                onFailure = { error ->
                    ProviderTokenResult(
                        success = false,
                        message = "Logto Secret Vault 令牌读取失败：${cleanErrorMessage(error.message)}",
                    )
                },
            )
    }

    private suspend fun fetchFrontendApiKey(
        activity: Activity,
        providerAccessToken: String,
    ): AipingFrontendApiKeyActivity.FrontendApiKeyResult =
        withContext(Dispatchers.Main) {
            val cachedWebAccessToken = withContext(Dispatchers.IO) {
                prefs.getAipingWebAccessToken()
            }
            suspendCancellableCoroutine { continuation ->
                AipingFrontendApiKeyActivity.pendingSession =
                    AipingFrontendApiKeyActivity.PendingSession(
                        providerAccessToken = providerAccessToken,
                        cachedWebAccessToken = cachedWebAccessToken,
                    ) { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                continuation.invokeOnCancellation {
                    AipingFrontendApiKeyActivity.pendingSession = null
                }
                activity.startActivity(Intent(activity, AipingFrontendApiKeyActivity::class.java))
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

    private suspend fun signInDirectly(
        activity: Activity,
        forceLogin: Boolean = false,
    ): DirectSignInResult {
        val oidcConfig = fetchOidcConfig()
            ?: return DirectSignInResult(success = false, message = "Logto OIDC 配置获取失败")
        val codeVerifier = GenerateUtils.generateCodeVerifier()
        val state = GenerateUtils.generateState()
        val directSignInUri =
            try {
                val builder = Core.generateSignInUri(
                    authorizationEndpoint = oidcConfig.authorizationEndpoint,
                    clientId = logtoConfig.appId,
                    redirectUri = BuildConfig.ARIES_LOGTO_REDIRECT_URI,
                    codeChallenge = GenerateUtils.generateCodeChallenge(codeVerifier),
                    state = state,
                    scopes = logtoConfig.scopes,
                    resources = logtoConfig.resources,
                    prompt = if (forceLogin) FORCE_LOGIN_PROMPT else logtoConfig.prompt,
                )
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter(
                        DIRECT_SIGN_IN_QUERY,
                        "$DIRECT_SIGN_IN_SOCIAL_PREFIX${BuildConfig.AIPING_LOGTO_CONNECTOR_TARGET}",
                    )
                if (forceLogin) {
                    builder.addQueryParameter(MAX_AGE_QUERY, FORCE_LOGIN_MAX_AGE)
                }
                builder.build().toString()
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
                    exchange = { logtoAccessToken, completion ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val exchangeResult = exchangeLogtoAccessToken(logtoAccessToken)
                            withContext(Dispatchers.Main) {
                                completion(exchangeResult)
                            }
                        }
                    },
                ) { result ->
                    if (continuation.isActive) {
                        continuation.resume(
                            DirectSignInResult(
                                success = result.success,
                                accessToken = result.accessToken,
                                apiKey = result.apiKey,
                                webAccessToken = result.webAccessToken,
                                displayName = result.displayName,
                                accountInfo = result.accountInfo,
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

    private fun exchangeLogtoAccessToken(logtoAccessToken: String): AipingDirectLogtoActivity.ExchangeResult {
        if (logtoAccessToken.isBlank()) {
            return AipingDirectLogtoActivity.ExchangeResult(
                success = false,
                message = "Logto 访问令牌获取失败",
            )
        }
        val aipingAccessToken =
            fetchAipingAccessToken(logtoAccessToken)
                .getOrElse { error ->
                    return AipingDirectLogtoActivity.ExchangeResult(
                        success = false,
                        message = "Logto Secret Vault 令牌读取失败：${cleanErrorMessage(error.message)}",
                    )
                }
        val accountInfoResult = AipingOAuthClient.requestUserInfo(aipingAccessToken)
        val apiKeyResult = AipingOAuthClient.requestApiKey(aipingAccessToken)
        return if (apiKeyResult.success) {
            AipingDirectLogtoActivity.ExchangeResult(
                success = true,
                apiKey = apiKeyResult.apiKey,
                displayName = accountInfoResult.displayName.ifBlank { "AI Ping" },
                accountInfo = accountInfoResult.accountInfo,
            )
        } else {
            AipingDirectLogtoActivity.ExchangeResult(
                success = false,
                displayName = accountInfoResult.displayName,
                accountInfo = accountInfoResult.accountInfo,
                message = "文档接口未返回可用 API Key：${cleanErrorMessage(apiKeyResult.message)}",
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

    private suspend fun clearAipingAuthorizationSession() {
        clearLogtoStorage()
        AipingDirectLogtoActivity.pendingSession = null
        AipingFrontendApiKeyActivity.pendingSession = null
        withContext(Dispatchers.IO) {
            prefs.setAipingWebAccessToken("")
        }
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
        }
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
        const val FORCE_LOGIN_PROMPT = "login"
        const val MAX_AGE_QUERY = "max_age"
        const val FORCE_LOGIN_MAX_AGE = "0"
    }

    private fun shouldRetryAipingAuthorization(message: String): Boolean {
        val normalized = message.lowercase()
        return listOf(
            "error occurred in connector",
            "invalid access token",
            "invalid token",
            "expired",
            "revoked",
            "secret vault",
            "access-token",
        ).any { keyword -> normalized.contains(keyword) }
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
