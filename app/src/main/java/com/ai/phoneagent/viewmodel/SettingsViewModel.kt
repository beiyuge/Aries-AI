package com.ai.phoneagent.viewmodel

import android.app.Application
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ai.phoneagent.R
import com.ai.phoneagent.data.preferences.AppPreferencesRepository
import com.ai.phoneagent.net.AipingApiClient
import com.ai.phoneagent.net.AipingLogtoAuthManager
import com.ai.phoneagent.net.AriesApiClient
import com.ai.phoneagent.net.AriesOidcAuthManager
import com.ai.phoneagent.net.AutoGlmClient
import com.ai.phoneagent.net.ModelScopeModelDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    application: Application,
    private val prefs: AppPreferencesRepository,
    private val ariesOidcAuthManager: AriesOidcAuthManager,
    private val aipingLogtoAuthManager: AipingLogtoAuthManager,
) : AndroidViewModel(application) {

    enum class SettingsPage {
        Home,
        ModelApi,
        Membership,
        Appearance,
        About,
    }

    enum class ApiMode {
        Official,
        ThirdParty,
        Aiping,
        Local,
        Aries,
    }

    private var remoteApiOk: Boolean? = null
    private var remoteApiChecking: Boolean = false
    private var apiCheckSeq: Int = 0
    private var lastCheckedApiKey: String = ""
    private var qwenDownloadInFlight: Boolean = false
    private var apiInputTag: String = ""
    private var apiModePersistJob: Job? = null

    var localModelReady by mutableStateOf(false)
        private set

    var currentPage by mutableStateOf(SettingsPage.Home)
        private set

    var pageTransitionForward by mutableStateOf(true)
        private set

    var apiInputText by mutableStateOf("")
        private set

    var useThirdPartyApi by mutableStateOf(false)
        private set

    var useLocalModel by mutableStateOf(false)
        private set

    var useAriesApi by mutableStateOf(false)
        private set

    var useAipingApi by mutableStateOf(false)
        private set

    var currentApiMode by mutableStateOf(ApiMode.Official)
        private set

    var apiBaseUrlText by mutableStateOf("")
        private set

    var apiModelText by mutableStateOf("")
        private set

    var apiStatusText by mutableStateOf("")
        private set

    var apiStatusPositive by mutableStateOf(false)
        private set

    var qwenButtonText by mutableStateOf("")
        private set

    var qwenButtonEnabled by mutableStateOf(true)
        private set

    // ─── Aries API 区段可见性（隐藏特性，需连点 xuanyu.xyla 20 次解锁） ────
    var showAriesApiSection by mutableStateOf(false)
        private set

    // ─── Aries 登录状态 ──────────────────────────────────────────────────────
    var ariesLoggedInUser by mutableStateOf("")
        private set

    var aipingLoggedInUser by mutableStateOf("")
        private set

    var aipingAccountInfo by mutableStateOf("")
        private set

    var aipingChatModel by mutableStateOf("")
        private set

    var aipingAutomationModel by mutableStateOf("")
        private set

    // ─── Aries / AI Ping 已选模型 ────────────────────────────────────────────
    var ariesSelectedModel by mutableStateOf("")
        private set

    // ─── Aries 模型选择对话框状态 ────────────────────────────────────────────
    var showAriesModelDialog by mutableStateOf(false)
        private set

    var ariesAvailableModels by mutableStateOf<List<AriesApiClient.ModelInfo>>(emptyList())
        private set

    var showAipingChatModelDialog by mutableStateOf(false)
        private set

    var showAipingAutomationModelDialog by mutableStateOf(false)
        private set

    var aipingAvailableModels by mutableStateOf<List<AipingApiClient.ModelInfo>>(emptyList())
        private set

    var aipingChatAvailableModels by mutableStateOf<List<AipingApiClient.ModelInfo>>(emptyList())
        private set

    var aipingAutomationAvailableModels by mutableStateOf<List<AipingApiClient.ModelInfo>>(emptyList())
        private set

    var aipingModelsLoading by mutableStateOf(false)
        private set

    var showAipingApiKeyConfirmDialog by mutableStateOf(false)
        private set

    var pendingAipingApiKey by mutableStateOf("")
        private set

    var pendingAipingApiKeyInput by mutableStateOf("")
        private set

    var pendingAipingDisplayName by mutableStateOf("")
        private set

    var pendingAipingAccountInfo by mutableStateOf("")
        private set

    var aipingApiKeyConfirmError by mutableStateOf<String?>(null)
        private set

    // ─── 登录对话框状态 ──────────────────────────────────────────────────────
    var showAriesLoginDialog by mutableStateOf(false)
        private set

    var ariesLoginUsername by mutableStateOf("")
        private set

    var ariesLoginPassword by mutableStateOf("")
        private set

    var ariesLoginLoading by mutableStateOf(false)
        private set

    var ariesLoginError by mutableStateOf<String?>(null)
        private set

    init {
        localModelReady = ModelScopeModelDownloader.isQwen35ModelReady(getApplication())
        restoreSettings()
        // 响应式监听隐藏模型入口解锁状态（AboutViewModel 切换后实时生效）
        viewModelScope.launch {
            prefs.ariesApiSectionUnlockedFlow.collect { unlocked ->
                showAriesApiSection = unlocked
                if (!unlocked && (currentApiMode == ApiMode.Local || currentApiMode == ApiMode.Aries)) {
                    applyApiModeState(ApiMode.Official)
                    apiCheckSeq++
                    remoteApiOk = null
                    remoteApiChecking = false
                    lastCheckedApiKey = ""
                    persistApiMode(clearCheckResults = true)
                    updateStatusText()
                }
            }
        }
        // 响应式监听已登录用户
        viewModelScope.launch {
            prefs.ariesLoggedInUserFlow.collect { user ->
                ariesLoggedInUser = user
            }
        }
        viewModelScope.launch {
            prefs.aipingLoggedInUserFlow.collect { user ->
                aipingLoggedInUser = user
            }
        }
        viewModelScope.launch {
            prefs.aipingAccountInfoFlow.collect { info ->
                aipingAccountInfo = info
            }
        }
        viewModelScope.launch {
            prefs.aipingChatModelFlow.collect { model ->
                aipingChatModel = model
            }
        }
        viewModelScope.launch {
            prefs.aipingAutomationModelFlow.collect { model ->
                aipingAutomationModel = model
            }
        }
        // 响应式监听 Aries 已选模型
        viewModelScope.launch {
            prefs.ariesSelectedModelFlow.collect { model ->
                ariesSelectedModel = model
            }
        }
    }

    fun restoreSettings() {
        val saved = prefs.getApiKeyBlocking()
        apiInputTag = saved
        apiInputText = maskKey(saved)
        val restoredThirdParty = prefs.getApiUseThirdPartyBlocking()
        val restoredLocal = prefs.getApiUseLocalModelBlocking()
        val restoredAries = prefs.getUseAriesApiBlocking()
        val restoredAiping = prefs.getUseAipingApiBlocking()
        showAriesApiSection = prefs.getAriesApiSectionUnlockedBlocking()
        val restoredMode =
            resolveApiMode(
                useThirdParty = restoredThirdParty,
                useLocal = restoredLocal && showAriesApiSection,
                useAries = restoredAries && showAriesApiSection,
                useAiping = restoredAiping,
            )
        applyApiModeState(restoredMode)
        ariesLoggedInUser = prefs.getAriesLoggedInUserBlocking()
        aipingLoggedInUser = prefs.getAipingLoggedInUserBlocking()
        aipingAccountInfo = prefs.getAipingAccountInfoBlocking()
        ariesSelectedModel = prefs.getAriesSelectedModelBlocking()
        aipingChatModel = prefs.getAipingChatModelBlocking()
        aipingAutomationModel = prefs.getAipingAutomationModelBlocking()
        apiBaseUrlText = prefs.getApiThirdPartyBaseUrlBlocking().ifBlank { AutoGlmClient.DEFAULT_BASE_URL }
        apiModelText = prefs.getApiThirdPartyModelBlocking().ifBlank { AutoGlmClient.DEFAULT_MODEL }
        normalizePersistedApiModeIfNeeded(
            restoredThirdParty = restoredThirdParty,
            restoredLocal = restoredLocal,
            restoredAries = restoredAries,
            restoredAiping = restoredAiping,
        )

        val lastSig = prefs.getApiLastCheckSigBlocking()
        val currentSig =
            apiConfigSignature(
                apiKey = saved,
                baseUrl = resolveApiBaseUrl(),
                model = resolveApiModel(),
            )
        if (saved.isNotBlank() && prefs.hasApiLastCheckOkBlocking() && lastSig == currentSig) {
            remoteApiOk = prefs.getApiLastCheckOkBlocking()
            remoteApiChecking = false
            lastCheckedApiKey = saved
            apiStatusText =
                stringRes(
                    if (remoteApiOk == true) {
                        R.string.settings_api_available
                    } else {
                        R.string.settings_api_failed
                    },
                )
        } else {
            remoteApiOk = null
            remoteApiChecking = false
            lastCheckedApiKey = ""
            apiStatusText = stringRes(R.string.m3t_sidebar_api_not_checked)
        }
        updateQwenDownloadButtonState()
        updateStatusText()
    }

    fun refreshLocalModelState() {
        localModelReady = ModelScopeModelDownloader.isQwen35ModelReady(getApplication())
        updateQwenDownloadButtonState()
        updateStatusText()
    }

    fun openModelApiPage() {
        pageTransitionForward = true
        currentPage = SettingsPage.ModelApi
    }

    fun openMembershipPage() {
        pageTransitionForward = true
        currentPage = SettingsPage.Membership
    }

    fun openHomePage() {
        pageTransitionForward = false
        currentPage = SettingsPage.Home
    }

    fun navigateTo(page: SettingsPage) {
        pageTransitionForward = page != SettingsPage.Home
        currentPage = page
    }

    fun onApiModeChange(mode: ApiMode, onToast: (String) -> Unit) {
        if (apiModePersistJob?.isActive == true) return
        if (mode == currentApiMode) return
        if (!showAriesApiSection && (mode == ApiMode.Local || mode == ApiMode.Aries)) return
        applyApiModeState(mode)
        apiCheckSeq++
        remoteApiOk = null
        remoteApiChecking = false
        lastCheckedApiKey = ""
        persistApiMode(clearCheckResults = true)
        if (mode == ApiMode.Local && !localModelReady) {
            onToast(stringRes(R.string.m3t_sidebar_local_model_not_ready))
        }
        updateStatusText()
    }

    fun onApiInputChanged(value: String) {
        apiInputTag = ""
        apiInputText = value
        onApiConfigChanged(clearApiValue = value.isBlank())
    }

    fun onUseThirdPartyChange(checked: Boolean) {
        onApiModeChange(if (checked) ApiMode.ThirdParty else ApiMode.Official) {}
    }

    fun onApiBaseUrlChange(value: String) {
        apiBaseUrlText = value
        if (useThirdPartyApi) {
            onApiConfigChanged(clearApiValue = false)
        }
    }

    fun onApiModelChange(value: String) {
        apiModelText = value
        if (useThirdPartyApi) {
            onApiConfigChanged(clearApiValue = false)
        }
    }

    fun onUseLocalModelChange(checked: Boolean, onToast: (String) -> Unit) {
        onApiModeChange(if (checked) ApiMode.Local else ApiMode.Official, onToast)
    }

    fun onUseAriesApiChange(checked: Boolean) {
        onApiModeChange(if (checked) ApiMode.Aries else ApiMode.Official) {}
    }

    fun pasteApiKey(context: Context, onToast: (String) -> Unit) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pasted =
            clipboard?.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.trim()
                .orEmpty()
        if (pasted.isBlank()) {
            onToast(stringRes(R.string.settings_clipboard_empty))
            return
        }
        apiInputTag = ""
        apiInputText = pasted
        onToast(stringRes(R.string.settings_api_key_pasted))
        onApiConfigChanged(clearApiValue = false)
    }

    fun openApiKeyPage(context: Context) {
        runCatching {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://open.bigmodel.cn/usercenter/proj-mgmt/apikeys"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }

    fun onApiConfigChanged(clearApiValue: Boolean) {
        apiCheckSeq++
        remoteApiOk = null
        remoteApiChecking = false
        lastCheckedApiKey = ""
        viewModelScope.launch {
            prefs.writeApiConfig(
                removeApiKey = clearApiValue,
                useThirdParty = useThirdPartyApi,
                useLocalModel = useLocalModel,
                thirdPartyBaseUrl = if (useThirdPartyApi) apiBaseUrlText.trim() else null,
                thirdPartyModel = if (useThirdPartyApi) apiModelText.trim() else null,
                clearCheckResults = true,
            )
            prefs.setUseAriesApi(useAriesApi)
            prefs.setUseAipingApi(useAipingApi)
        }
        updateStatusText()
    }

    fun checkApiConnection(onToast: (String) -> Unit) {
        if (useLocalModel) {
            localModelReady = ModelScopeModelDownloader.isQwen35ModelReady(getApplication())
            updateQwenDownloadButtonState()
            apiStatusPositive = localModelReady
            apiStatusText =
                stringRes(
                    if (localModelReady) {
                        R.string.m3t_sidebar_local_model_ready
                    } else {
                        R.string.m3t_sidebar_local_model_not_ready
                    },
                )
            if (!localModelReady) {
                onToast(stringRes(R.string.m3t_sidebar_local_model_not_ready))
            }
            return
        }

        if (useAriesApi) {
            val key = prefs.getActiveAriesApiKeyBlocking().trim()
            if (key.isBlank()) {
                onToast(stringRes(R.string.settings_model_api_aries_login_required))
                return
            }
            startApiCheck(
                key = key,
                baseUrl = AriesApiClient.ARIES_API_V1_BASE_URL,
                model = ariesSelectedModel.ifBlank { AutoGlmClient.DEFAULT_MODEL },
                force = true,
                onToast = onToast,
            )
            return
        }

        if (useAipingApi) {
            val key = prefs.getAipingApiKeyBlocking().trim()
            if (key.isBlank()) {
                onToast(stringRes(R.string.settings_model_api_aiping_login_required))
                return
            }
            startApiCheck(
                key = key,
                baseUrl = AipingApiClient.AIPING_API_V1_BASE_URL,
                model = resolveAipingChatModel(),
                force = true,
                onToast = onToast,
            )
            return
        }

        val key = resolveApiKeyFromInput()
        if (key.isBlank()) {
            onToast(stringRes(R.string.settings_api_key_required))
            return
        }

        viewModelScope.launch { prefs.setApiKey(key) }
        apiInputTag = key
        apiInputText = maskKey(key)
        val baseUrl = resolveRemoteApiBaseUrl()
        val model = resolveRemoteApiModel()
        startApiCheck(key = key, baseUrl = baseUrl, model = model, force = true, onToast = onToast)
    }

    fun enqueueQwenDownloads(onToast: (String) -> Unit) {
        if (qwenDownloadInFlight) return
        qwenDownloadInFlight = true
        updateQwenDownloadButtonState()
        viewModelScope.launch {
            val result = ModelScopeModelDownloader.enqueueQwen35Downloads(getApplication())
            qwenDownloadInFlight = false
            result.onSuccess {
                localModelReady = ModelScopeModelDownloader.isQwen35ModelReady(getApplication())
                updateQwenDownloadButtonState()
                val message =
                    when {
                        it.enqueuedCount > 0 ->
                            stringRes(
                                R.string.m3t_sidebar_qwen_download_summary_format,
                                it.enqueuedCount,
                                it.skippedCount,
                                it.targetDir,
                            )
                        it.skippedCount > 0 -> stringRes(R.string.m3t_sidebar_qwen_download_cached)
                        else -> stringRes(R.string.m3t_sidebar_qwen_download_enqueued)
                    }
                onToast(message)
                updateStatusText()
            }.onFailure { err ->
                updateQwenDownloadButtonState()
                onToast(
                    stringRes(
                        R.string.m3t_sidebar_qwen_download_failed_format,
                        err.message?.trim().orEmpty().ifBlank {
                            stringRes(R.string.update_download_failed_unknown)
                        },
                    ),
                )
            }
        }
    }

    fun startApiCheck(
        key: String,
        baseUrl: String,
        model: String,
        force: Boolean,
        onToast: (String) -> Unit,
    ) {
        val normalizedBaseUrl = baseUrl.ifBlank { AutoGlmClient.DEFAULT_BASE_URL }
        val validationError = validateBaseUrlSecurity(normalizedBaseUrl)
        if (validationError != null) {
            apiStatusText = stringRes(R.string.settings_api_unsafe)
            apiStatusPositive = false
            onToast(validationError)
            return
        }
        maybeWarnInsecureHttpBaseUrl(normalizedBaseUrl, onToast)
        remoteApiChecking = true
        remoteApiOk = null
        lastCheckedApiKey = key.trim()
        apiStatusText = stringRes(R.string.settings_api_checking)
        updateStatusText()

        val seq = ++apiCheckSeq
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    AutoGlmClient.checkApiDetailed(
                        apiKey = key.trim(),
                        baseUrl = normalizedBaseUrl,
                        model = model.ifBlank { AutoGlmClient.DEFAULT_MODEL },
                    )
                }
            if (seq != apiCheckSeq) return@launch

            remoteApiChecking = false
            remoteApiOk = result.ok
            apiStatusText =
                stringRes(
                    if (result.ok) {
                        R.string.settings_api_available
                    } else {
                        R.string.settings_api_failed
                    },
                )
            prefs.writeApiConfig(
                apiKey = key.trim(),
                lastCheckKey = key.trim(),
                lastCheckOk = result.ok,
                lastCheckTime = System.currentTimeMillis(),
                lastCheckSig = apiConfigSignature(key.trim(), normalizedBaseUrl, model),
            )
            if (!result.ok && force) {
                onToast(formatApiCheckFailureReason(result.statusCode, result.message))
            }
            updateStatusText()
        }
    }

    fun updateQwenDownloadButtonState() {
        when {
            qwenDownloadInFlight -> {
                qwenButtonEnabled = false
                qwenButtonText = stringRes(R.string.m3t_sidebar_qwen_download_preparing)
            }

            localModelReady -> {
                qwenButtonEnabled = true
                qwenButtonText = stringRes(R.string.m3t_sidebar_qwen_download_ready)
            }

            else -> {
                qwenButtonEnabled = true
                qwenButtonText = stringRes(R.string.m3t_sidebar_qwen_download)
            }
        }
    }

    fun updateStatusText() {
        if (useAriesApi) {
            val hasAriesKey = prefs.getActiveAriesApiKeyBlocking().isNotBlank()
            apiStatusPositive = remoteApiOk == true || (remoteApiOk == null && hasAriesKey)
            if (!remoteApiChecking) {
                apiStatusText =
                    when {
                        remoteApiOk == true -> stringRes(R.string.settings_api_available)
                        remoteApiOk == false -> stringRes(R.string.settings_api_failed)
                        hasAriesKey -> stringRes(R.string.settings_model_api_aries_ready)
                        else -> stringRes(R.string.settings_model_api_aries_login_required)
                    }
            }
        } else if (useAipingApi) {
            val hasAipingKey = prefs.getAipingApiKeyBlocking().isNotBlank()
            apiStatusPositive = remoteApiOk == true || (remoteApiOk == null && hasAipingKey)
            if (!remoteApiChecking) {
                apiStatusText =
                    when {
                        remoteApiOk == true -> stringRes(R.string.settings_api_available)
                        remoteApiOk == false -> stringRes(R.string.settings_api_failed)
                        hasAipingKey -> stringRes(R.string.settings_model_api_aiping_ready)
                        else -> stringRes(R.string.settings_model_api_aiping_login_required)
                    }
            }
        } else if (useLocalModel) {
            apiStatusPositive = localModelReady
            apiStatusText =
                stringRes(
                    if (localModelReady) {
                        R.string.m3t_sidebar_local_model_ready
                    } else {
                        R.string.m3t_sidebar_local_model_not_ready
                    },
                )
        } else {
            apiStatusPositive = remoteApiOk == true
            if (!remoteApiChecking && remoteApiOk == null) {
                apiStatusText = stringRes(R.string.m3t_sidebar_api_not_checked)
            }
        }
    }

    fun resolveApiKeyFromInput(): String {
        val displayed = apiInputText
        val tagKey = apiInputTag.trim()
        val savedKey = prefs.getApiKeyBlocking().trim()
        return when {
            tagKey.isNotBlank() && displayed == maskKey(tagKey) -> tagKey
            savedKey.isNotBlank() && displayed == maskKey(savedKey) -> savedKey
            displayed.contains("*") && savedKey.isNotBlank() -> savedKey
            else -> displayed
        }.trim()
    }

    fun resolveApiBaseUrl(): String {
        if (useAriesApi) return AriesApiClient.ARIES_API_V1_BASE_URL
        if (useAipingApi) return AipingApiClient.AIPING_API_V1_BASE_URL
        if (useLocalModel) return AutoGlmClient.DEFAULT_BASE_URL
        return resolveRemoteApiBaseUrl()
    }

    fun resolveApiModel(): String {
        if (useAriesApi) return AriesApiClient.ARIES_CHAT_MODEL
        if (useAipingApi) return resolveAipingChatModel()
        if (useLocalModel) return ModelScopeModelDownloader.QWEN35_MODEL_NAME
        return resolveRemoteApiModel()
    }

    private fun resolveRemoteApiBaseUrl(): String {
        if (!useThirdPartyApi) return AutoGlmClient.DEFAULT_BASE_URL
        return apiBaseUrlText.trim().ifBlank { AutoGlmClient.DEFAULT_BASE_URL }
    }

    private fun resolveRemoteApiModel(): String {
        if (!useThirdPartyApi) return AutoGlmClient.DEFAULT_MODEL
        return apiModelText.trim().ifBlank { AutoGlmClient.DEFAULT_MODEL }
    }

    private fun resolveAipingChatModel(): String =
        aipingChatModel.trim().ifBlank { AipingApiClient.AIPING_DEFAULT_CHAT_MODEL }

    private fun resolveAipingAutomationModel(): String =
        aipingAutomationModel.trim().ifBlank { AipingApiClient.AIPING_DEFAULT_AUTOMATION_MODEL }

    fun maskKey(raw: String): String {
        if (raw.length <= 8) return raw
        return raw.take(4) + "*".repeat(raw.length - 8) + raw.takeLast(4)
    }

    fun formatApiCheckFailureReason(statusCode: Int?, message: String?): String {
        val cleanMessage = message?.trim().orEmpty()
        return when {
            statusCode != null && cleanMessage.isNotBlank() ->
                stringRes(R.string.settings_api_failed_http_message, statusCode, cleanMessage)

            statusCode != null ->
                stringRes(R.string.settings_api_failed_http, statusCode)

            cleanMessage.isNotBlank() ->
                stringRes(R.string.settings_api_failed_message, cleanMessage)

            else ->
                stringRes(R.string.settings_api_failed_generic)
        }
    }

    fun apiConfigSignature(apiKey: String, baseUrl: String, model: String): String {
        val mode =
            when {
                useAriesApi -> "aries"
                useAipingApi -> "aiping"
                useThirdPartyApi -> "third_party"
                else -> "official"
            }
        return "$mode|${apiKey.trim()}|${baseUrl.ifBlank { AutoGlmClient.DEFAULT_BASE_URL }}|${model.ifBlank { AutoGlmClient.DEFAULT_MODEL }}"
    }

    fun validateBaseUrlSecurity(baseUrl: String): String? {
        val parsed = runCatching { Uri.parse(baseUrl.trim()) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()
        if (scheme.isNullOrBlank() || host.isNullOrBlank()) {
            return stringRes(R.string.settings_api_invalid_url)
        }
        if (scheme != "https" && scheme != "http") {
            return stringRes(R.string.settings_api_invalid_scheme)
        }
        return null
    }

    fun maybeWarnInsecureHttpBaseUrl(baseUrl: String, onToast: (String) -> Unit) {
        val parsed = runCatching { Uri.parse(baseUrl.trim()) }.getOrNull() ?: return
        val scheme = parsed.scheme?.lowercase()
        val host = parsed.host?.lowercase()
        val localHosts = setOf("localhost", "127.0.0.1", "0.0.0.0", "::1")
        if (scheme == "http" && host !in localHosts) {
            onToast(stringRes(R.string.settings_api_http_warning))
        }
    }

    // ─── Aries SSO 登录 ────────────────────────────────────────────────────

    fun openAriesLoginDialog() {
        ariesLoginUsername = ""
        ariesLoginPassword = ""
        ariesLoginError = null
        showAriesLoginDialog = true
    }

    fun dismissAriesLoginDialog() {
        showAriesLoginDialog = false
        ariesLoginLoading = false
        ariesLoginError = null
    }

    fun onAriesLoginUsernameChange(value: String) {
        ariesLoginUsername = value
        ariesLoginError = null
    }

    fun onAriesLoginPasswordChange(value: String) {
        ariesLoginPassword = value
        ariesLoginError = null
    }

    fun submitAriesSsoLogin(activity: Activity, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        ariesLoginLoading = true
        ariesLoginError = null
        viewModelScope.launch {
            val result = ariesOidcAuthManager.signIn(activity)
            ariesLoginLoading = false
            if (result.success) {
                prefs.setAriesApiKey(result.accessToken)
                val displayName = result.displayName.ifBlank { stringRes(R.string.aries_sso_user_display) }
                prefs.setAriesLoggedInUser(displayName)
                applyApiModeState(ApiMode.Aries)
                persistApiModeState(clearCheckResults = true)
                ariesLoggedInUser = displayName
                showAriesLoginDialog = false
                remoteApiOk = null
                remoteApiChecking = false
                lastCheckedApiKey = ""
                updateStatusText()
                onSuccess(stringRes(R.string.aries_login_success, displayName))
            } else {
                ariesLoginError = result.message.ifBlank { stringRes(R.string.aries_login_failed) }
                onError(ariesLoginError ?: "")
            }
        }
    }

    fun ariesLogout() {
        viewModelScope.launch {
            ariesOidcAuthManager.signOut()
            prefs.setAriesLoggedInUser("")
            prefs.setAriesApiKey("")
            ariesLoggedInUser = ""
            if (currentApiMode == ApiMode.Aries) {
                applyApiModeState(ApiMode.Official)
                persistApiModeState(clearCheckResults = true)
                remoteApiOk = null
                remoteApiChecking = false
                lastCheckedApiKey = ""
            }
            updateStatusText()
        }
    }

    fun applyAipingLoginResult(
        apiKey: String,
        displayName: String,
        accountInfo: String,
        onSuccess: (String) -> Unit,
    ) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return
        viewModelScope.launch {
            prefs.setAipingApiKey(cleanKey)
            val resolvedDisplayName = displayName.ifBlank { stringRes(R.string.settings_model_api_aiping_login) }
            prefs.setAipingLoggedInUser(resolvedDisplayName)
            prefs.setAipingAccountInfo(accountInfo.ifBlank { resolvedDisplayName })
            applyApiModeState(ApiMode.Aiping)
            persistApiModeState(clearCheckResults = true)
            aipingLoggedInUser = resolvedDisplayName
            aipingAccountInfo = accountInfo.ifBlank { resolvedDisplayName }
            remoteApiOk = null
            remoteApiChecking = false
            lastCheckedApiKey = ""
            updateStatusText()
            onSuccess(stringRes(R.string.settings_model_api_aiping_login_success, resolvedDisplayName))
            refreshAipingModels()
        }
    }

    fun submitAipingLogin(activity: Activity, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        ariesLoginLoading = true
        viewModelScope.launch {
            val result = aipingLogtoAuthManager.signInAndGetApiKey(activity)
            ariesLoginLoading = false
            if (result.success) {
                showAipingApiKeyConfirmation(
                    apiKey = result.apiKey,
                    displayName = result.displayName,
                    accountInfo = result.accountInfo,
                )
                onSuccess(stringRes(R.string.settings_model_api_aiping_api_key_confirm_pending))
            } else {
                persistAipingAccountInfo(
                    displayName = result.displayName,
                    accountInfo = result.accountInfo,
                )
                onError(result.message.ifBlank { stringRes(R.string.settings_model_api_aiping_login_required) })
            }
        }
    }

    private fun showAipingApiKeyConfirmation(
        apiKey: String,
        displayName: String,
        accountInfo: String,
    ) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return
        val resolvedDisplayName = displayName.ifBlank { stringRes(R.string.settings_model_api_aiping_login) }
        pendingAipingApiKey = cleanKey
        pendingAipingApiKeyInput = ""
        pendingAipingDisplayName = resolvedDisplayName
        pendingAipingAccountInfo = accountInfo.ifBlank { resolvedDisplayName }
        aipingApiKeyConfirmError = null
        showAipingApiKeyConfirmDialog = true
        aipingLoggedInUser = resolvedDisplayName
        aipingAccountInfo = accountInfo.ifBlank { resolvedDisplayName }
        applyApiModeState(ApiMode.Aiping)
        viewModelScope.launch {
            prefs.setAipingLoggedInUser(resolvedDisplayName)
            prefs.setAipingAccountInfo(accountInfo.ifBlank { resolvedDisplayName })
            persistApiModeState(clearCheckResults = true)
            remoteApiOk = null
            remoteApiChecking = false
            lastCheckedApiKey = ""
            updateStatusText()
        }
    }

    fun onPendingAipingApiKeyInputChange(value: String) {
        pendingAipingApiKeyInput = value
        aipingApiKeyConfirmError = null
    }

    fun confirmPendingAipingApiKey(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val expected = pendingAipingApiKey.trim()
        val input = pendingAipingApiKeyInput.trim()
        when {
            expected.isBlank() -> {
                val message = stringRes(R.string.settings_model_api_aiping_api_key_confirm_missing)
                aipingApiKeyConfirmError = message
                onError(message)
            }
            input.isBlank() -> {
                val message = stringRes(R.string.settings_model_api_aiping_api_key_confirm_input_required)
                aipingApiKeyConfirmError = message
                onError(message)
            }
            input != expected -> {
                val message = stringRes(R.string.settings_model_api_aiping_api_key_confirm_mismatch)
                aipingApiKeyConfirmError = message
                onError(message)
            }
            else -> {
                val displayName = pendingAipingDisplayName
                val accountInfo = pendingAipingAccountInfo
                showAipingApiKeyConfirmDialog = false
                pendingAipingApiKey = ""
                pendingAipingApiKeyInput = ""
                pendingAipingDisplayName = ""
                pendingAipingAccountInfo = ""
                aipingApiKeyConfirmError = null
                applyAipingLoginResult(
                    apiKey = input,
                    displayName = displayName,
                    accountInfo = accountInfo,
                    onSuccess = onSuccess,
                )
            }
        }
    }

    private fun persistAipingAccountInfo(displayName: String, accountInfo: String) {
        if (displayName.isBlank() && accountInfo.isBlank()) return
        viewModelScope.launch {
            val resolvedDisplayName = displayName.ifBlank { stringRes(R.string.settings_model_api_aiping_login) }
            prefs.setAipingLoggedInUser(resolvedDisplayName)
            prefs.setAipingAccountInfo(accountInfo.ifBlank { resolvedDisplayName })
            aipingLoggedInUser = resolvedDisplayName
            aipingAccountInfo = accountInfo.ifBlank { resolvedDisplayName }
            updateStatusText()
        }
    }

    fun aipingLogout() {
        viewModelScope.launch {
            aipingLogtoAuthManager.signOut()
            prefs.setAipingLoggedInUser("")
            prefs.setAipingAccountInfo("")
            prefs.setAipingApiKey("")
            prefs.setAipingWebAccessToken("")
            prefs.setAipingChatModel("")
            prefs.setAipingAutomationModel("")
            aipingLoggedInUser = ""
            aipingAccountInfo = ""
            aipingChatModel = ""
            aipingAutomationModel = ""
            aipingAvailableModels = emptyList()
            aipingChatAvailableModels = emptyList()
            aipingAutomationAvailableModels = emptyList()
            showAipingChatModelDialog = false
            showAipingAutomationModelDialog = false
            showAipingApiKeyConfirmDialog = false
            pendingAipingApiKey = ""
            pendingAipingApiKeyInput = ""
            pendingAipingDisplayName = ""
            pendingAipingAccountInfo = ""
            aipingApiKeyConfirmError = null
            if (currentApiMode == ApiMode.Aiping) {
                persistApiModeState(clearCheckResults = true)
                remoteApiOk = null
                remoteApiChecking = false
                lastCheckedApiKey = ""
            }
            updateStatusText()
        }
    }

    // ─── Aries 模型选择 ────────────────────────────────────────────────

    private fun fetchAndShowModels(apiKey: String) {
        viewModelScope.launch {
            val modelsResult = withContext(Dispatchers.IO) {
                AriesApiClient.fetchModels(apiKey)
            }
            modelsResult.onSuccess { models ->
                ariesAvailableModels = models
                if (models.isNotEmpty()) showAriesModelDialog = true
            }
        }
    }

    fun openAriesModelSelectionDialog() {
        val key = prefs.getActiveAriesApiKeyBlocking().trim()
        if (ariesAvailableModels.isEmpty() && key.isNotBlank()) {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { AriesApiClient.fetchModels(key) }
                result.onSuccess { models ->
                    ariesAvailableModels = models
                    if (models.isNotEmpty()) showAriesModelDialog = true
                }
            }
        } else if (ariesAvailableModels.isNotEmpty()) {
            showAriesModelDialog = true
        }
    }

    fun dismissAriesModelDialog() {
        showAriesModelDialog = false
    }

    fun selectAriesModel(modelId: String) {
        ariesSelectedModel = modelId
        showAriesModelDialog = false
        viewModelScope.launch { prefs.setAriesSelectedModel(modelId) }
        updateStatusText()
    }

    fun refreshAipingModels(onToast: ((String) -> Unit)? = null) {
        val key = prefs.getAipingApiKeyBlocking().trim()
        if (key.isBlank()) {
            onToast?.invoke(stringRes(R.string.settings_model_api_aiping_login_required))
            return
        }
        if (aipingModelsLoading) return
        aipingModelsLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AipingApiClient.fetchModels(key) }
            aipingModelsLoading = false
            result.onSuccess { models ->
                updateAipingModelLists(models)
                onToast?.invoke(stringRes(R.string.settings_model_api_aiping_models_loaded, aipingAvailableModels.size))
            }.onFailure { error ->
                onToast?.invoke(
                    stringRes(
                        R.string.settings_model_api_aiping_models_failed,
                        error.message.orEmpty().ifBlank { stringRes(R.string.settings_api_failed_generic) },
                    ),
                )
            }
        }
    }

    fun openAipingChatModelSelectionDialog(onToast: (String) -> Unit) {
        openAipingModelSelectionDialog(
            onToast = onToast,
            modelsProvider = { aipingChatAvailableModels },
            showDialog = { showAipingChatModelDialog = true },
        )
    }

    fun openAipingAutomationModelSelectionDialog(onToast: (String) -> Unit) {
        openAipingModelSelectionDialog(
            onToast = onToast,
            modelsProvider = { aipingAutomationAvailableModels },
            showDialog = { showAipingAutomationModelDialog = true },
        )
    }

    fun dismissAipingModelDialog() {
        showAipingChatModelDialog = false
        showAipingAutomationModelDialog = false
    }

    fun selectAipingChatModel(modelId: String) {
        aipingChatModel = modelId
        showAipingChatModelDialog = false
        viewModelScope.launch { prefs.setAipingChatModel(modelId) }
        remoteApiOk = null
        updateStatusText()
    }

    fun selectAipingAutomationModel(modelId: String) {
        aipingAutomationModel = modelId
        showAipingAutomationModelDialog = false
        viewModelScope.launch { prefs.setAipingAutomationModel(modelId) }
    }

    private fun openAipingModelSelectionDialog(
        onToast: (String) -> Unit,
        modelsProvider: () -> List<AipingApiClient.ModelInfo>,
        showDialog: () -> Unit,
    ) {
        if (modelsProvider().isNotEmpty()) {
            showDialog()
            return
        }
        val key = prefs.getAipingApiKeyBlocking().trim()
        if (key.isBlank()) {
            onToast(stringRes(R.string.settings_model_api_aiping_login_required))
            return
        }
        if (aipingModelsLoading) {
            onToast(stringRes(R.string.settings_model_api_aiping_models_loading))
            return
        }
        aipingModelsLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AipingApiClient.fetchModels(key) }
            aipingModelsLoading = false
            result.onSuccess { models ->
                updateAipingModelLists(models)
                if (modelsProvider().isEmpty()) {
                    onToast(stringRes(R.string.settings_model_api_aiping_models_empty))
                } else {
                    showDialog()
                }
            }.onFailure { error ->
                onToast(
                    stringRes(
                        R.string.settings_model_api_aiping_models_failed,
                        error.message.orEmpty().ifBlank { stringRes(R.string.settings_api_failed_generic) },
                    ),
                )
            }
        }
    }

    private fun updateAipingModelLists(models: List<AipingApiClient.ModelInfo>) {
        aipingAvailableModels = models
        aipingChatAvailableModels = AipingApiClient.chatModels(models)
        aipingAutomationAvailableModels = AipingApiClient.automationVisionModels(models)
        ensureAipingModelDefaults()
    }

    private fun ensureAipingModelDefaults() {
        val defaultChatModel =
            aipingChatAvailableModels.firstOrNull { it.id.equals(AipingApiClient.AIPING_DEFAULT_CHAT_MODEL, ignoreCase = true) }?.id
                ?: aipingChatAvailableModels.firstOrNull()?.id.orEmpty()
        if ((aipingChatModel.isBlank() || aipingChatModel == "DeepSeek-R1-0528") && defaultChatModel.isNotBlank()) {
            aipingChatModel = defaultChatModel
            viewModelScope.launch { prefs.setAipingChatModel(defaultChatModel) }
        }
        val defaultAutomationModel =
            aipingAutomationAvailableModels.firstOrNull { it.id.equals(AipingApiClient.AIPING_DEFAULT_AUTOMATION_MODEL, ignoreCase = true) }?.id
                ?: aipingAutomationAvailableModels.firstOrNull()?.id.orEmpty()
        if ((aipingAutomationModel.isBlank() || aipingAutomationModel == "DeepSeek-R1-0528") && defaultAutomationModel.isNotBlank()) {
            aipingAutomationModel = defaultAutomationModel
            viewModelScope.launch { prefs.setAipingAutomationModel(defaultAutomationModel) }
        }
    }

    private fun resolveApiMode(
        useThirdParty: Boolean = useThirdPartyApi,
        useLocal: Boolean = useLocalModel,
        useAries: Boolean = useAriesApi,
        useAiping: Boolean = useAipingApi,
    ): ApiMode =
        when {
            useAries -> ApiMode.Aries
            useAiping -> ApiMode.Aiping
            useLocal -> ApiMode.Local
            useThirdParty -> ApiMode.ThirdParty
            else -> ApiMode.Official
        }

    private fun applyApiModeState(mode: ApiMode) {
        currentApiMode = mode
        useThirdPartyApi = mode == ApiMode.ThirdParty
        useLocalModel = mode == ApiMode.Local
        useAriesApi = mode == ApiMode.Aries
        useAipingApi = mode == ApiMode.Aiping
    }

    private fun normalizePersistedApiModeIfNeeded(
        restoredThirdParty: Boolean,
        restoredLocal: Boolean,
        restoredAries: Boolean,
        restoredAiping: Boolean,
    ) {
        if (
            restoredThirdParty == useThirdPartyApi &&
            restoredLocal == useLocalModel &&
            restoredAries == useAriesApi &&
            restoredAiping == useAipingApi
        ) {
            return
        }
        persistApiMode(clearCheckResults = false)
    }

    private fun persistApiMode(clearCheckResults: Boolean) {
        apiModePersistJob?.cancel()
        apiModePersistJob = viewModelScope.launch {
            persistApiModeState(clearCheckResults = clearCheckResults)
        }
    }

    private suspend fun persistApiModeState(clearCheckResults: Boolean) {
        prefs.writeApiConfig(
            useThirdParty = useThirdPartyApi,
            useLocalModel = useLocalModel,
            clearCheckResults = clearCheckResults,
        )
        prefs.setUseAriesApi(useAriesApi)
        prefs.setUseAipingApi(useAipingApi)
    }

    private fun stringRes(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)
}
