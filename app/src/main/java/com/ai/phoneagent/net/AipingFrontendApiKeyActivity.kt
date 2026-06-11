package com.ai.phoneagent.net

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ai.phoneagent.R
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
import kotlin.coroutines.resume

class AipingFrontendApiKeyActivity : Activity() {
    data class FrontendApiKeyResult(
        val success: Boolean,
        val apiKey: String = "",
        val webAccessToken: String = "",
        val message: String = "",
    )

    data class PendingSession(
        val providerAccessToken: String,
        val cachedWebAccessToken: String = "",
        val completion: (FrontendApiKeyResult) -> Unit,
    )

    companion object {
        var pendingSession: PendingSession? = null
        private const val AIPING_FRONTEND_URL = "https://aiping.cn/user/apikey"
        private const val AIPING_LOGIN_URL = "https://central.qc-ai.cn/login"
        private const val TOKEN_QUERY = "accessToken"
        private const val CACHED_TOKEN_PROBE_ATTEMPTS = 4
        private const val MAX_PROBE_ATTEMPTS = 240

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
                  var token = pickToken({
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
    private var pollJob: Job? = null
    private var interactiveLoginLoaded = false
    private lateinit var webView: WebView
    private lateinit var progressContent: View

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = pendingSession
        if (session == null || session.providerAccessToken.isBlank()) {
            finishWith(FrontendApiKeyResult(success = false, message = "AI Ping 前端取 Key 会话已失效"))
            return
        }

        webView =
            WebView(this).apply {
                visibility = View.INVISIBLE
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            if (interactiveLoginLoaded) {
                                webView.visibility = View.VISIBLE
                                progressContent.visibility = View.GONE
                            }
                            startPolling(session)
                        }
                    }
            }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }
        progressContent = createProgressContent()
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(ContextCompat.getColor(this@AipingFrontendApiKeyActivity, R.color.m3t_background))
                addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                addView(progressContent, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            },
        )
        if (session.cachedWebAccessToken.isNotBlank()) {
            webView.loadUrl(buildInjectedFrontendUrl(session.cachedWebAccessToken))
        } else {
            showInteractiveLogin()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        finishWith(FrontendApiKeyResult(success = false, message = "AI Ping 前端取 Key 已取消"))
    }

    override fun onDestroy() {
        pollJob?.cancel()
        if (!completed) {
            finishWith(FrontendApiKeyResult(success = false, message = "AI Ping 前端取 Key 已取消"))
        }
        super.onDestroy()
    }

    private fun startPolling(session: PendingSession) {
        if (completed || pollJob?.isActive == true) return
        pollJob =
            CoroutineScope(Dispatchers.Main).launch {
                var lastFailure = "AI Ping 前端尚未返回 API Key"
                repeat(MAX_PROBE_ATTEMPTS) { attempt ->
                    delay(900)
                    evaluateJavascript(START_PROBE_SCRIPT)
                    delay(700)
                    val raw = evaluateJavascript("window.__ariesAipingKeyProbeResult || ''")
                    val decoded = decodeJavascriptString(raw)
                    if (decoded.isBlank()) return@repeat
                    val parsed = parseProbeResult(decoded)
                    if (parsed.success) {
                        finishWith(parsed)
                        return@launch
                    }
                    lastFailure = parsed.message.ifBlank { lastFailure }
                    evaluateJavascript("window.__ariesAipingKeyProbeResult = ''; window.__ariesAipingKeyProbeRunning = false; ''")
                    if (!interactiveLoginLoaded && attempt >= CACHED_TOKEN_PROBE_ATTEMPTS) {
                        showInteractiveLogin()
                    }
                }
                finishWith(FrontendApiKeyResult(success = false, message = lastFailure))
            }
    }

    private fun buildInjectedFrontendUrl(providerAccessToken: String): String =
        Uri.parse(AIPING_FRONTEND_URL)
            .buildUpon()
            .appendQueryParameter(TOKEN_QUERY, providerAccessToken)
            .build()
            .toString()

    private fun createProgressContent(): View {
        val spacingLg = resources.getDimensionPixelSize(R.dimen.m3t_spacing_lg)
        val spacingMd = resources.getDimensionPixelSize(R.dimen.m3t_spacing_md)
        val spacingSm = resources.getDimensionPixelSize(R.dimen.m3t_spacing_sm)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(spacingLg, spacingLg, spacingLg, spacingLg)
            addView(
                ProgressBar(this@AipingFrontendApiKeyActivity),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    bottomMargin = spacingMd
                },
            )
            addView(
                TextView(this@AipingFrontendApiKeyActivity).apply {
                    text = getString(R.string.settings_model_api_aiping_finishing_login_title)
                    setTextAppearance(R.style.TextAppearance_M3t_Title_Large)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = spacingSm
                },
            )
            addView(
                TextView(this@AipingFrontendApiKeyActivity).apply {
                    text = getString(R.string.settings_model_api_aiping_finishing_login_body)
                    setTextAppearance(R.style.TextAppearance_M3t_Body_Small)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            )
        }
    }

    private fun buildLoginFallbackUrl(): String =
        Uri.parse(AIPING_LOGIN_URL)
            .buildUpon()
            .appendQueryParameter("fromUrl", AIPING_FRONTEND_URL)
            .build()
            .toString()

    private fun showInteractiveLogin() {
        interactiveLoginLoaded = true
        webView.visibility = View.VISIBLE
        progressContent.visibility = View.GONE
        webView.loadUrl(buildLoginFallbackUrl())
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

    private fun parseProbeResult(raw: String): FrontendApiKeyResult =
        runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val apiKey = textAt(obj, "apiKey")
            val webAccessToken = textAt(obj, "webAccessToken")
            if (obj["success"]?.jsonPrimitive?.booleanOrNull == true && apiKey.isNotBlank()) {
                FrontendApiKeyResult(success = true, apiKey = apiKey, webAccessToken = webAccessToken)
            } else {
                FrontendApiKeyResult(
                    success = false,
                    message =
                        buildString {
                            append("stage=")
                            append(textAt(obj, "stage"))
                            append(" status=")
                            append(textAt(obj, "status"))
                            append(" href=")
                            append(textAt(obj, "href"))
                            val tokenLength = textAt(obj, "tokenLength")
                            if (tokenLength.isNotBlank()) append(" tokenLength=$tokenLength")
                            val message = textAt(obj, "message")
                            if (message.isNotBlank()) append(" message=$message")
                            val bodyLength = textAt(obj, "bodyLength")
                            if (bodyLength.isNotBlank()) append(" bodyLength=$bodyLength")
                            val keys = textAt(obj, "keys")
                            if (keys.isNotBlank()) append(" keys=$keys")
                        }.trim(),
                )
            }
        }.getOrElse { error ->
            FrontendApiKeyResult(success = false, message = "AI Ping 前端取 Key 结果解析失败：${error.message.orEmpty()}")
        }

    private fun textAt(obj: JsonObject, key: String): String =
        obj[key]?.primitiveContent().orEmpty()

    private fun JsonElement.primitiveContent(): String =
        (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun finishWith(result: FrontendApiKeyResult) {
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
