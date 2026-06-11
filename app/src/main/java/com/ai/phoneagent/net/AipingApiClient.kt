package com.ai.phoneagent.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

object AipingApiClient {
    const val AIPING_API_V1_BASE_URL = "https://aiping.cn/api/v1"
    const val AIPING_DEFAULT_CHAT_MODEL = "Kimi-K2.6"
    const val AIPING_DEFAULT_AUTOMATION_MODEL = "Kimi-K2.6"

    data class ModelInfo(
        val id: String,
        val ownedBy: String = "",
        val profileText: String = "",
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    suspend fun fetchModels(apiKey: String): Result<List<ModelInfo>> =
        withContext(Dispatchers.IO) {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("AI Ping API Key 为空"))
            }
            val request =
                Request.Builder()
                    .url("$AIPING_API_V1_BASE_URL/models")
                    .header("Authorization", "Bearer $cleanKey")
                    .header("Accept", "application/json")
                    .get()
                    .build()
            try {
                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    parseModelsResponse(raw, response.code)
                }
            } catch (e: IOException) {
                Result.failure(IOException("AI Ping 模型列表请求失败：${e.message.orEmpty().trim()}"))
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }

    private fun parseModelsResponse(raw: String, code: Int): Result<List<ModelInfo>> {
        if (raw.isBlank()) return Result.failure(IllegalArgumentException("AI Ping 模型列表响应为空 (HTTP $code)"))
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val dataArray = root["data"]?.jsonArray
                ?: return Result.failure(IllegalArgumentException("AI Ping 模型列表响应缺少 data 字段"))
            val models =
                dataArray.mapNotNull { element ->
                    val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    if (id.isBlank()) return@mapNotNull null
                    ModelInfo(
                        id = id,
                        ownedBy = obj["owned_by"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        profileText = obj.toString(),
                    )
                }.distinctBy { it.id }.sortedBy { it.id }
            if (models.isEmpty()) {
                Result.failure(IllegalArgumentException("AI Ping 模型列表为空"))
            } else {
                Result.success(models)
            }
        } catch (e: SerializationException) {
            Result.failure(IllegalArgumentException("AI Ping 模型列表解析失败：${e.message.orEmpty().trim()}"))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    fun chatModels(models: List<ModelInfo>): List<ModelInfo> =
        models.filter { it.isChatModel() }

    fun automationVisionModels(models: List<ModelInfo>): List<ModelInfo> =
        chatModels(models).filter { it.supportsVisionInput() || it.id.equals(AIPING_DEFAULT_AUTOMATION_MODEL, ignoreCase = true) }

    private fun ModelInfo.isChatModel(): Boolean {
        val text = searchableText()
        return NON_CHAT_MODEL_KEYWORDS.none { keyword -> text.contains(keyword) }
    }

    private fun ModelInfo.supportsVisionInput(): Boolean {
        val text = searchableText()
        return VISION_MODEL_KEYWORDS.any { keyword -> text.contains(keyword) }
    }

    private fun ModelInfo.searchableText(): String =
        "$id $ownedBy $profileText".lowercase()

    private val NON_CHAT_MODEL_KEYWORDS =
        listOf(
            "image-generation",
            "image_generation",
            "text-to-image",
            "txt2img",
            "t2i",
            "dall",
            "flux",
            "stable-diffusion",
            "sdxl",
            "midjourney",
            "生图",
            "文生图",
            "图像生成",
            "绘图",
            "画图",
            "video",
            "kling",
            "sora",
            "veo",
            "wan2",
            "hailuo",
            "runway",
            "视频",
            "embedding",
            "embed",
            "vector",
            "bge-m3",
            "jina-embeddings",
            "向量",
            "嵌入",
            "ocr",
            "文字识别",
            "rerank",
            "re-rank",
            "ranker",
            "重排",
            "tts",
            "whisper",
            "asr",
            "audio",
            "speech",
            "语音",
            "moderation",
        )

    private val VISION_MODEL_KEYWORDS =
        listOf(
            "vision",
            "visual",
            "multimodal",
            "multi-modal",
            "omni",
            "image_input",
            "input_image",
            "image_url",
            "image input",
            "\"image\"",
            "image",
            "\"input_modality\"",
            "\"input_modalities\"",
            "\"modalities\"",
            "vl",
            "-v",
            "4v",
            "mm",
            "qwen-vl",
            "qwen2-vl",
            "qwen2.5-vl",
            "glm-4v",
            "glm-4.1v",
            "gpt-4o",
            "gpt-4.1",
            "claude-3",
            "claude-sonnet",
            "claude-opus",
            "gemini",
            "deepseek-vl",
            "doubao-vision",
            "kimi",
            "moonshot",
            "doubao",
            "hunyuan",
            "minimax",
            "internvl",
            "llava",
            "pixtral",
            "grok-vision",
            "step",
            "ernie",
            "yi-vision",
            "图像",
            "图片",
            "图",
            "视觉",
            "识图",
            "图文",
            "多模态",
        )
}
