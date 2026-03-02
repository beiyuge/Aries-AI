/*
 * Aries AI - Android UI Automation Framework
 * Copyright (C) 2025-2026 ZG0704666
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.ai.phoneagent.helper

import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.ai.phoneagent.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * AI 流式消息渲染助手。
 * - 流式阶段：限频纯文本刷新，避免抖动和卡顿
 * - 完成阶段：一次性做完整 markdown/latex 渲染
 */
object StreamRenderHelper {

    data class ViewHolder(
        val thinkingLayout: LinearLayout,
        val thinkingHeader: LinearLayout,
        val thinkingText: TextView,
        val thinkingIndicator: TextView,
        val thinkingContentArea: View,
        val messageContent: TextView,
        val authorName: TextView,
        val actionArea: View,
        val retryButton: View?,
        val copyButton: View?
    )

    /**
     * 文本流式渲染器：对 setText 做限频，减少重排和滚动闪烁。
     */
    private class TextAnimator(
        textView: TextView,
        private val scope: CoroutineScope,
        private val onUpdate: () -> Unit,
        val useMarkdown: Boolean = false
    ) {
        private val viewRef = WeakReference(textView)
        private val textBuilder = StringBuilder()
        private var renderJob: Job? = null
        private var lastRenderAtMs = 0L

        fun append(delta: String) {
            if (delta.isEmpty()) return
            synchronized(textBuilder) {
                textBuilder.append(delta)
            }
            scheduleRender()
        }

        fun setFullText(text: String) {
            renderJob?.cancel()
            synchronized(textBuilder) {
                textBuilder.clear()
                textBuilder.append(text)
            }
            renderNow(notifyScroll = true)
        }

        fun getText(): String = synchronized(textBuilder) { textBuilder.toString() }

        fun appendRaw(delta: String) {
            if (delta.isEmpty()) return
            synchronized(textBuilder) {
                textBuilder.append(delta)
            }
        }

        fun clear() {
            renderJob?.cancel()
            synchronized(textBuilder) {
                textBuilder.clear()
            }
            val view = viewRef.get() ?: return
            view.text = ""
        }

        fun stop() {
            renderJob?.cancel()
            renderNow(notifyScroll = false)
        }

        private fun scheduleRender() {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRenderAtMs
            if (elapsed >= RENDER_INTERVAL_MS) {
                renderJob?.cancel()
                renderNow(notifyScroll = true)
                return
            }
            if (renderJob?.isActive == true) return

            val waitMs = (RENDER_INTERVAL_MS - elapsed).coerceAtLeast(0L)
            renderJob = scope.launch {
                delay(waitMs)
                renderNow(notifyScroll = true)
            }
        }

        private fun renderNow(notifyScroll: Boolean) {
            val view = viewRef.get() ?: return
            val currentText = synchronized(textBuilder) { textBuilder.toString() }
            applyText(view, currentText)
            lastRenderAtMs = System.currentTimeMillis()
            if (notifyScroll) {
                view.post { onUpdate() }
            }
        }

        private fun applyText(view: TextView, text: String) {
            if (!useMarkdown) {
                view.text = text
                return
            }
            try {
                MarkdownRenderer.getInstance(view.context).render(view, text)
            } catch (_: Throwable) {
                view.text = SimpleMarkdownRenderer.render(text)
            }
        }

        companion object {
            private const val RENDER_INTERVAL_MS = 48L
        }
    }

    private val animators = ConcurrentHashMap<Int, TextAnimator>()
    private val parsers = ConcurrentHashMap<Int, AriesStreamParser>()
    private var thinkingStartTime = 0L

    fun bindViews(aiView: View): ViewHolder {
        return ViewHolder(
            thinkingLayout = aiView.findViewById(R.id.thinking_layout),
            thinkingHeader = aiView.findViewById(R.id.thinking_header),
            thinkingText = aiView.findViewById(R.id.thinking_text),
            thinkingIndicator = aiView.findViewById(R.id.thinking_indicator_text),
            thinkingContentArea = aiView.findViewById(R.id.thinking_content_area),
            messageContent = aiView.findViewById(R.id.message_content),
            authorName = aiView.findViewById(R.id.ai_author_name),
            actionArea = aiView.findViewById(R.id.action_area),
            retryButton = aiView.findViewById(R.id.btn_retry),
            copyButton = aiView.findViewById(R.id.btn_copy)
        )
    }

    fun initThinkingState(vh: ViewHolder) {
        val viewId = vh.hashCode()

        cleanup(vh)

        vh.thinkingText.text = ""
        vh.messageContent.text = ""

        thinkingStartTime = System.currentTimeMillis()
        parsers[viewId] = AriesStreamParser()

        vh.authorName.visibility = View.VISIBLE
        vh.thinkingLayout.visibility = View.VISIBLE
        vh.thinkingLayout.alpha = 1f
        vh.actionArea.visibility = View.GONE

        val headerTitle = vh.thinkingHeader.getChildAt(0) as? TextView
        headerTitle?.text = "思考中"

        vh.thinkingText.visibility = View.VISIBLE
        vh.thinkingContentArea.visibility = View.VISIBLE
        vh.thinkingIndicator.text = " ▼"

        if (vh.thinkingHeader.tag != "listener_set") {
            var expanded = true
            vh.thinkingHeader.setOnClickListener {
                expanded = !expanded
                vh.thinkingText.visibility = if (expanded) View.VISIBLE else View.GONE
                vh.thinkingContentArea.visibility = if (expanded) View.VISIBLE else View.GONE
                vh.thinkingIndicator.text = if (expanded) " ▼" else " ▶"
            }
            vh.thinkingHeader.tag = "listener_set"
        }
    }

    private fun getParser(vh: ViewHolder): AriesStreamParser {
        return parsers.getOrPut(vh.hashCode()) { AriesStreamParser() }
    }

    private fun getAnimator(
        textView: TextView,
        scope: CoroutineScope,
        onScroll: () -> Unit,
        useMarkdown: Boolean = false
    ): TextAnimator {
        val id = textView.hashCode()

        val existing = animators[id]
        if (existing != null) {
            if (existing.useMarkdown != useMarkdown) {
                existing.stop()
                animators.remove(id)
            } else {
                return existing
            }
        }

        val newAnimator = TextAnimator(textView, scope, onScroll, useMarkdown)
        animators[id] = newAnimator
        return newAnimator
    }

    fun processReasoningDelta(
        vh: ViewHolder,
        delta: String,
        coroutineScope: CoroutineScope,
        onScroll: () -> Unit
    ) {
        if (delta.isEmpty()) return

        vh.thinkingLayout.visibility = View.VISIBLE
        vh.thinkingLayout.alpha = 1f
        vh.thinkingText.visibility = View.VISIBLE
        vh.thinkingContentArea.visibility = View.VISIBLE

        val parser = getParser(vh)
        parser.processReasoningDelta(delta)

        // 思考阶段不渲染 LaTeX，保持可读和稳定滚动。
        val animator = getAnimator(vh.thinkingText, coroutineScope, onScroll, useMarkdown = false)
        animator.append(delta)
    }

    @Suppress("UNUSED_PARAMETER")
    fun processContentDelta(
        vh: ViewHolder,
        delta: String,
        coroutineScope: CoroutineScope,
        context: Context,
        onScroll: () -> Unit,
        onPhaseChange: (Boolean) -> Unit
    ) {
        if (delta.isEmpty()) return

        // 正文流式阶段只做纯文本，结束后再统一做 markdown/latex 渲染。
        val answerAnimator = getAnimator(vh.messageContent, coroutineScope, onScroll, useMarkdown = false)
        val isFirstAnswerChunk = answerAnimator.getText().isEmpty()
        if (isFirstAnswerChunk) {
            onPhaseChange(true)
        }
        answerAnimator.append(delta)
    }

    fun transitionToAnswer(vh: ViewHolder) {
        val elapsed = (System.currentTimeMillis() - thinkingStartTime) / 1000

        val headerTitle = vh.thinkingHeader.getChildAt(0) as? TextView
        headerTitle?.text = "已思考(用时 ${elapsed} 秒)"

        vh.thinkingLayout.animate()
            .alpha(0.85f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun markCompleted(vh: ViewHolder, timeCostSec: Long) {
        val headerTitle = vh.thinkingHeader.getChildAt(0) as? TextView
        headerTitle?.text = "已思考(用时 ${timeCostSec} 秒)"

        vh.actionArea.visibility = View.VISIBLE
        vh.actionArea.alpha = 0f
        vh.actionArea.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        val thinkingAnimator = animators[vh.thinkingText.hashCode()]
        val answerAnimator = animators[vh.messageContent.hashCode()]
        thinkingAnimator?.stop()
        answerAnimator?.stop()

        val flushedChunks = parsers[vh.hashCode()]?.flush().orEmpty()
        val extraThinking = StringBuilder()
        val extraAnswer = StringBuilder()
        for (chunk in flushedChunks) {
            when (chunk.type) {
                AriesStreamParser.ChunkType.THINKING -> extraThinking.append(chunk.content)
                AriesStreamParser.ChunkType.ANSWER -> extraAnswer.append(chunk.content)
                AriesStreamParser.ChunkType.CONTROL -> Unit
            }
        }

        val extraThinkingStr = sanitizeFlushTail(extraThinking.toString())
        val extraAnswerStr = sanitizeFlushTail(extraAnswer.toString())
        if (extraThinkingStr.isNotEmpty()) thinkingAnimator?.appendRaw(extraThinkingStr)
        if (extraAnswerStr.isNotEmpty()) answerAnimator?.appendRaw(extraAnswerStr)

        val thinkingRaw = thinkingAnimator?.getText() ?: extraThinkingStr
        val answerRaw = answerAnimator?.getText() ?: extraAnswerStr

        vh.thinkingLayout.visibility = if (thinkingRaw.isBlank()) View.GONE else View.VISIBLE
        if (thinkingRaw.isNotBlank()) {
            applyPlainMarkdownToHistory(vh.thinkingText, thinkingRaw)
        }
        if (answerRaw.isNotBlank()) {
            applyMarkdownToHistory(vh.messageContent, answerRaw)
        }
    }

    fun getThinkingText(vh: ViewHolder): String {
        val animatorText = animators[vh.thinkingText.hashCode()]?.getText().orEmpty()
        if (animatorText.isNotBlank()) return animatorText
        return vh.thinkingText.text?.toString().orEmpty()
    }

    fun getAnswerText(vh: ViewHolder): String {
        val animatorText = animators[vh.messageContent.hashCode()]?.getText().orEmpty()
        if (animatorText.isNotBlank()) return animatorText
        return vh.messageContent.text?.toString().orEmpty()
    }

    fun cleanup(vh: ViewHolder) {
        val thinkingId = vh.thinkingText.hashCode()
        val contentId = vh.messageContent.hashCode()

        animators[thinkingId]?.clear()
        animators[contentId]?.clear()

        animators.remove(thinkingId)
        animators.remove(contentId)
        parsers.remove(vh.hashCode())
    }

    fun applyMarkdownToHistory(textView: TextView, content: String) {
        if (content.isBlank()) {
            textView.text = ""
            return
        }
        MarkdownRenderer.getInstance(textView.context).render(textView, content)
    }

    fun applyPlainMarkdownToHistory(textView: TextView, content: String) {
        if (content.isBlank()) {
            textView.text = ""
            return
        }
        textView.text = SimpleMarkdownRenderer.render(content)
    }

    private fun sanitizeFlushTail(tail: String): String {
        if (tail.isBlank()) return tail

        var core = tail
        val whitespaceSuffix = core.takeLastWhile { it.isWhitespace() }
        if (whitespaceSuffix.isNotEmpty()) {
            core = core.dropLast(whitespaceSuffix.length)
        }

        val tags =
            listOf(
                "【思考开始】",
                "【思考结束】",
                "【思考】",
                "【回答开始】",
                "【回答结束】",
                "【回答】",
                "<think>",
                "</think>",
                "<思考>",
                "</思考>",
                "<思考：",
                "<思考"
            )

        for (tag in tags) {
            core = core.replace(tag, "")
        }

        for (tag in tags) {
            if (core.isEmpty()) break
            for (i in 1 until tag.length) {
                val prefix = tag.substring(0, i)
                if (core.endsWith(prefix)) {
                    core = core.dropLast(prefix.length)
                    break
                }
            }
        }

        return core + whitespaceSuffix
    }
}
