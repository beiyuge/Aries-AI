package com.ai.phoneagent.net

import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.logto.sdk.android.constant.StorageKey
import io.logto.sdk.android.storage.PersistStorage
import io.logto.sdk.core.Core
import io.logto.sdk.core.exception.CallbackUriVerificationException
import io.logto.sdk.core.type.CodeTokenResponse
import io.logto.sdk.core.type.OidcConfigResponse
import io.logto.sdk.core.util.CallbackUriUtils
import io.logto.sdk.core.util.TokenUtils
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.lang.JoseException

class AipingDirectLogtoActivity : Activity() {
    data class DirectLoginResult(
        val success: Boolean,
        val accessToken: String = "",
        val message: String = "",
    )

    data class PendingSession(
        val oidcConfig: OidcConfigResponse,
        val clientId: String,
        val redirectUri: String,
        val codeVerifier: String,
        val state: String,
        val storageName: String,
        val completion: (DirectLoginResult) -> Unit,
    )

    companion object {
        const val EXTRA_AUTH_URL = "extra_auth_url"
        var pendingSession: PendingSession? = null
    }

    private var completed = false
    private var finalizing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = pendingSession
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL).orEmpty()
        if (session == null || authUrl.isBlank()) {
            finishWith(DirectLoginResult(success = false, message = "AI Ping 登录会话已失效"))
            return
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            flush()
        }

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    handleUrl(request.url.toString(), session)

                @Suppress("OVERRIDE_DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    handleUrl(url, session)
            }

        setContentView(webView)
        webView.loadUrl(authUrl)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        finishWith(DirectLoginResult(success = false, message = "AI Ping 登录已取消"))
    }

    override fun onDestroy() {
        if (!completed) {
            finishWith(DirectLoginResult(success = false, message = "AI Ping 登录已取消"))
        }
        super.onDestroy()
    }

    private fun handleUrl(url: String, session: PendingSession): Boolean {
        if (!url.startsWith(session.redirectUri)) return false
        if (finalizing) return true
        finalizing = true

        val code =
            try {
                CallbackUriUtils.verifyAndParseCodeFromCallbackUri(
                    callbackUri = url,
                    redirectUri = session.redirectUri,
                    state = session.state,
                )
            } catch (e: CallbackUriVerificationException) {
                finishWith(
                    DirectLoginResult(
                        success = false,
                        message =
                            e.errorDesc
                                ?.let { "AI Ping 授权被拒绝：$it" }
                                ?: e.error?.let { "AI Ping 授权被拒绝：$it" }
                                ?: "AI Ping 登录回调校验失败",
                    ),
                )
                return true
            }

        Core.fetchTokenByAuthorizationCode(
            tokenEndpoint = session.oidcConfig.tokenEndpoint,
            clientId = session.clientId,
            redirectUri = session.redirectUri,
            codeVerifier = session.codeVerifier,
            code = code,
            resource = null,
        ) { error, codeToken ->
            if (error != null || codeToken == null) {
                finishWith(
                    DirectLoginResult(
                        success = false,
                        message = error?.message.orEmpty().ifBlank { "Logto 授权码换取令牌失败" },
                    ),
                )
                return@fetchTokenByAuthorizationCode
            }
            verifyAndPersistToken(session, codeToken)
        }
        return true
    }

    private fun verifyAndPersistToken(session: PendingSession, codeToken: CodeTokenResponse) {
        Core.fetchJwksJson(session.oidcConfig.jwksUri) { error, jwksJson ->
            if (error != null || jwksJson == null) {
                finishWith(
                    DirectLoginResult(
                        success = false,
                        message = error?.message.orEmpty().ifBlank { "Logto JWKS 获取失败" },
                    ),
                )
                return@fetchJwksJson
            }

            try {
                TokenUtils.verifyIdToken(
                    idToken = codeToken.idToken,
                    clientId = session.clientId,
                    issuer = session.oidcConfig.issuer,
                    jwks = JsonWebKeySet(jwksJson),
                )
            } catch (e: InvalidJwtException) {
                finishWith(DirectLoginResult(success = false, message = "Logto ID Token 校验失败"))
                return@fetchJwksJson
            } catch (e: JoseException) {
                finishWith(DirectLoginResult(success = false, message = "Logto JWKS 解析失败"))
                return@fetchJwksJson
            }

            PersistStorage(applicationContext, session.storageName).apply {
                setItem(StorageKey.ID_TOKEN, codeToken.idToken)
                setItem(StorageKey.REFRESH_TOKEN, codeToken.refreshToken)
            }
            finishWith(
                DirectLoginResult(
                    success = true,
                    accessToken = codeToken.accessToken,
                ),
            )
        }
    }

    private fun finishWith(result: DirectLoginResult) {
        if (completed) return
        completed = true
        val session = pendingSession
        pendingSession = null
        runOnUiThread {
            session?.completion?.invoke(result)
            finish()
        }
    }
}
