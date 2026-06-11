package com.ai.phoneagent.net

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ai.phoneagent.R
import io.logto.sdk.android.constant.StorageKey
import io.logto.sdk.android.storage.PersistStorage
import io.logto.sdk.core.Core
import io.logto.sdk.core.exception.CallbackUriVerificationException
import io.logto.sdk.core.type.CodeTokenResponse
import io.logto.sdk.core.type.OidcConfigResponse
import io.logto.sdk.core.util.CallbackUriUtils
import io.logto.sdk.core.util.TokenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.lang.JoseException
import kotlin.coroutines.resume

class AipingDirectLogtoActivity : Activity() {
    data class DirectLoginResult(
        val success: Boolean,
        val accessToken: String = "",
        val apiKey: String = "",
        val webAccessToken: String = "",
        val displayName: String = "",
        val accountInfo: String = "",
        val message: String = "",
    )

    data class ExchangeResult(
        val success: Boolean,
        val apiKey: String = "",
        val displayName: String = "",
        val accountInfo: String = "",
        val message: String = "",
    )

    data class PendingSession(
        val oidcConfig: OidcConfigResponse,
        val clientId: String,
        val redirectUri: String,
        val codeVerifier: String,
        val state: String,
        val storageName: String,
        val exchange: (String, (ExchangeResult) -> Unit) -> Unit,
        val completion: (DirectLoginResult) -> Unit,
    )

    companion object {
        const val EXTRA_AUTH_URL = "extra_auth_url"
        var pendingSession: PendingSession? = null

        private const val AIPING_FRONTEND_URL = "https://aiping.cn/user/apikey"
        private const val MAX_PROBE_ATTEMPTS = 240
        private const val FRONTEND_LOGIN_EXPIRED_MESSAGE = "AI Ping 登录已过期，请重新登录"

        private const val START_PROBE_SCRIPT =
            """
            (function() {
              if (window.__ariesAipingKeyProbeRunning) return 'running';
              window.__ariesAipingKeyProbeRunning = true;
              window.__ariesAipingKeyProbeResult = '';
              (async function() {
                function finish(payload) {
                  window.__ariesAipingKeyProbeResult = JSON.stringify(payload || {});
                  window.__ariesAipingKeyProbeRunning = false;
                }
                function entries(storage) {
                  var out = {};
                  try {
                    for (var i = 0; i < storage.length; i++) {
                      var key = storage.key(i);
                      out[key] = storage.getItem(key);
                    }
                  } catch (e) {}
                  return out;
                }
                function parse(value) {
                  if (!value || typeof value !== 'string') return null;
                  try { return JSON.parse(value); } catch (e) { return null; }
                }
                function tokenFromUrl() {
                  try {
                    var names = ['accessToken', 'access_token', 'token'];
                    var search = new URLSearchParams(window.location.search || '');
                    for (var i = 0; i < names.length; i++) {
                      var direct = search.get(names[i]);
                      if (direct) return direct;
                    }
                    var hash = String(window.location.hash || '').replace(/^#/, '');
                    var hashParams = new URLSearchParams(hash);
                    for (var j = 0; j < names.length; j++) {
                      var hashToken = hashParams.get(names[j]);
                      if (hashToken) return hashToken;
                    }
                  } catch (e) {}
                  return '';
                }
                function pickToken(value) {
                  if (!value) return '';
                  if (typeof value === 'string') {
                    var trimmed = value.replace(/^Bearer\s+/i, '').trim();
                    var nested = parse(trimmed);
                    if (nested) return pickToken(nested);
                    return trimmed.length >= 16 && trimmed.indexOf(' ') < 0 ? trimmed : '';
                  }
                  if (Array.isArray(value)) {
                    for (var i = 0; i < value.length; i++) {
                      var arrToken = pickToken(value[i]);
                      if (arrToken) return arrToken;
                    }
                    return '';
                  }
                  if (typeof value !== 'object') return '';
                  var preferred = ['accessToken', 'access_token', 'token', 'userAccessToken', 'user_access_token'];
                  for (var p = 0; p < preferred.length; p++) {
                    var direct = pickToken(value[preferred[p]]);
                    if (direct) return direct;
                  }
                  for (var key in value) {
                    if (!Object.prototype.hasOwnProperty.call(value, key)) continue;
                    if (key.toLowerCase().indexOf('token') >= 0) {
                      var token = pickToken(value[key]);
                      if (token) return token;
                    }
                  }
                  for (var nestedKey in value) {
                    if (!Object.prototype.hasOwnProperty.call(value, nestedKey)) continue;
                    var nestedToken = pickToken(value[nestedKey]);
                    if (nestedToken) return nestedToken;
                  }
                  return '';
                }
                function pickApiKey(value) {
                  if (!value) return '';
                  if (typeof value === 'string') {
                    var trimmed = value.trim();
                    if (trimmed.indexOf('QC-') === 0 || trimmed.indexOf('sk-') === 0) return trimmed;
                    var parsed = parse(trimmed);
                    return parsed ? pickApiKey(parsed) : '';
                  }
                  if (Array.isArray(value)) {
                    for (var i = 0; i < value.length; i++) {
                      var arrKey = pickApiKey(value[i]);
                      if (arrKey) return arrKey;
                    }
                    return '';
                  }
                  if (typeof value !== 'object') return '';
                  var preferred = ['apikey', 'apiKey', 'api_key', 'key'];
                  for (var p = 0; p < preferred.length; p++) {
                    var direct = pickApiKey(value[preferred[p]]);
                    if (direct) return direct;
                  }
                  return pickApiKey(value.data) || pickApiKey(value.apikeyBaseInfo);
                }
                try {
                  var token = pickToken(tokenFromUrl()) || pickToken({
                    local: entries(window.localStorage),
                    session: entries(window.sessionStorage)
                  });
                  if (!token) {
                    finish({
                      success: false,
                      stage: 'token',
                      href: window.location.href,
                      keys: Object.keys(entries(window.localStorage)).concat(Object.keys(entries(window.sessionStorage))).join(',')
                    });
                    return;
                  }
                  window.localStorage.setItem('accessToken', token);
                  var response = await fetch('/api/v1/user/apikey/get', {
                    credentials: 'include',
                    headers: {
                      'Accept': 'application/json',
                      'Authorization': 'Bearer ' + token
                    }
                  });
                  var text = await response.text();
                  var obj = {};
                  try { obj = JSON.parse(text); } catch (e) {}
                  var apiKey = pickApiKey(obj);
                  finish({
                    success: !!apiKey,
                    apiKey: apiKey || '',
                    webAccessToken: token || '',
                    stage: 'apikey',
                    status: response.status,
                    href: window.location.href,
                    tokenLength: token.length,
                    message: obj && (obj.msg || obj.message || obj.error || ''),
                    bodyLength: (text || '').length
                  });
                } catch (error) {
                  finish({
                    success: false,
                    stage: 'exception',
                    href: window.location.href,
                    message: String(error)
                  });
                }
              })();
              return 'started';
            })();
            """
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var completed = false
    private var finalizing = false
    private var frontendMode = false
    private var exchangeResult = ExchangeResult(success = false)
    private var pollJob: Job? = null
    private lateinit var webView: WebView
    private lateinit var progressContent: View

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = pendingSession
        if (session == null) {
            finishWith(DirectLoginResult(success = false, message = "AI Ping 登录会话已失效"))
            return
        }

        webView =
            WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = handleNavigation(request.url.toString(), session)

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String,
                        ): Boolean = handleNavigation(url, session)

                        override fun onPageFinished(view: WebView, url: String) {
                            if (handleNavigation(url, session)) return
                            if (frontendMode) {
                                showProgress()
                                startFrontendPolling()
                            }
                        }
                    }
            }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }
        progressContent = createProgressContent()
        progressContent.visibility = View.GONE
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(ContextCompat.getColor(this@AipingDirectLogtoActivity, R.color.m3t_background))
                addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                addView(progressContent, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            },
        )

        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL).orEmpty()
        if (authUrl.isBlank()) {
            finishWith(DirectLoginResult(success = false, message = "AI Ping 登录地址为空"))
            return
        }
        webView.loadUrl(authUrl)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        finishWith(DirectLoginResult(success = false, message = "AI Ping 登录已取消"))
    }

    override fun onDestroy() {
        pollJob?.cancel()
        if (!completed) {
            finishWith(DirectLoginResult(success = false, message = "AI Ping 登录已取消"))
        }
        super.onDestroy()
    }

    private fun handleNavigation(url: String, session: PendingSession): Boolean {
        if (!url.startsWith(session.redirectUri)) return false
        handleUrl(url, session)
        return true
    }

    private fun handleUrl(url: String, session: PendingSession) {
        if (finalizing || completed) return
        finalizing = true
        showProgress()

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
                return
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
            verifyAndContinue(session, codeToken)
        }
    }

    private fun verifyAndContinue(session: PendingSession, codeToken: CodeTokenResponse) {
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
            session.exchange(codeToken.accessToken) { result ->
                runOnUiThread {
                    if (completed) return@runOnUiThread
                    exchangeResult = result
                    if (result.success && result.apiKey.isNotBlank()) {
                        finishWith(
                            DirectLoginResult(
                                success = true,
                                accessToken = codeToken.accessToken,
                                apiKey = result.apiKey,
                                displayName = result.displayName,
                                accountInfo = result.accountInfo,
                            ),
                        )
                    } else {
                        frontendMode = true
                        webView.visibility = View.INVISIBLE
                        showProgress()
                        webView.loadUrl(AIPING_FRONTEND_URL)
                    }
                }
            }
        }
    }

    private fun startFrontendPolling() {
        if (completed || pollJob?.isActive == true) return
        pollJob =
            CoroutineScope(Dispatchers.Main).launch {
                repeat(MAX_PROBE_ATTEMPTS) {
                    delay(900)
                    evaluateJavascript(START_PROBE_SCRIPT)
                    delay(700)
                    val raw = evaluateJavascript("window.__ariesAipingKeyProbeResult || ''")
                    val decoded = decodeJavascriptString(raw)
                    if (decoded.isBlank()) return@repeat
                    val parsed = parseProbeResult(decoded)
                    if (parsed.success) {
                        finishWith(
                            DirectLoginResult(
                                success = true,
                                apiKey = parsed.apiKey,
                                webAccessToken = parsed.webAccessToken,
                                displayName = exchangeResult.displayName,
                                accountInfo = exchangeResult.accountInfo,
                            ),
                        )
                        return@launch
                    }
                    if (parsed.message == FRONTEND_LOGIN_EXPIRED_MESSAGE) {
                        finishWith(DirectLoginResult(success = false, message = FRONTEND_LOGIN_EXPIRED_MESSAGE))
                        return@launch
                    }
                    evaluateJavascript("window.__ariesAipingKeyProbeResult = ''; window.__ariesAipingKeyProbeRunning = false; ''")
                }
                finishWith(DirectLoginResult(success = false, message = FRONTEND_LOGIN_EXPIRED_MESSAGE))
            }
    }

    private fun createProgressContent(): View {
        val spacingLg = resources.getDimensionPixelSize(R.dimen.m3t_spacing_lg)
        val spacingMd = resources.getDimensionPixelSize(R.dimen.m3t_spacing_md)
        val spacingSm = resources.getDimensionPixelSize(R.dimen.m3t_spacing_sm)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(spacingLg, spacingLg, spacingLg, spacingLg)
            addView(
                ProgressBar(this@AipingDirectLogtoActivity),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    bottomMargin = spacingMd
                },
            )
            addView(
                TextView(this@AipingDirectLogtoActivity).apply {
                    text = getString(R.string.settings_model_api_aiping_finishing_login_title)
                    setTextAppearance(R.style.TextAppearance_M3t_Title_Large)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = spacingSm
                },
            )
            addView(
                TextView(this@AipingDirectLogtoActivity).apply {
                    text = getString(R.string.settings_model_api_aiping_finishing_login_body)
                    setTextAppearance(R.style.TextAppearance_M3t_Body_Small)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            )
        }
    }

    private fun showProgress() {
        progressContent.visibility = View.VISIBLE
    }

    private suspend fun evaluateJavascript(script: String): String =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { value ->
                if (continuation.isActive) continuation.resume(value.orEmpty())
            }
        }

    private fun decodeJavascriptString(raw: String): String =
        runCatching { json.parseToJsonElement(raw).jsonPrimitive.contentOrNull.orEmpty() }
            .getOrDefault(raw)

    private fun parseProbeResult(raw: String): DirectLoginResult =
        runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val apiKey = textAt(obj, "apiKey")
            val webAccessToken = textAt(obj, "webAccessToken")
            if (obj["success"]?.jsonPrimitive?.booleanOrNull == true && apiKey.isNotBlank()) {
                DirectLoginResult(success = true, apiKey = apiKey, webAccessToken = webAccessToken)
            } else {
                DirectLoginResult(
                    success = false,
                    message = if (textAt(obj, "stage") == "token") "" else FRONTEND_LOGIN_EXPIRED_MESSAGE,
                )
            }
        }.getOrElse { error ->
            DirectLoginResult(success = false, message = FRONTEND_LOGIN_EXPIRED_MESSAGE)
        }

    private fun textAt(obj: JsonObject, key: String): String =
        obj[key]?.primitiveContent().orEmpty()

    private fun JsonElement.primitiveContent(): String =
        (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun finishWith(result: DirectLoginResult) {
        if (completed) return
        completed = true
        pollJob?.cancel()
        val session = pendingSession
        pendingSession = null
        runOnUiThread {
            session?.completion?.invoke(result)
            finish()
        }
    }
}
