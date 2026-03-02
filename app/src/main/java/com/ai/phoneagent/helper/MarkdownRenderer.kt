package com.ai.phoneagent.helper

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.LruCache
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle

/**
 * 统一的 Markdown + LaTeX 渲染器。
 * 流式阶段用纯文本，落盘/历史展示时走这里的一次性完整渲染。
 */
@PrismBundle(includeAll = true)
class MarkdownRenderer(context: Context) {

    private val baseMarkwon: Markwon
    private val latexMarkwon: Markwon
    private val mathCache = object : LruCache<String, Spanned>(120) {}

    init {
        val prism4j = Prism4j(GrammarLocatorDef())

        fun buildMarkwon(enableLatex: Boolean): Markwon {
            val builder =
                Markwon.builder(context)
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDarkula.create()))
            if (enableLatex) {
                builder.usePlugin(JLatexMathPlugin.create(44f))
            }
            return builder.build()
        }

        baseMarkwon = buildMarkwon(enableLatex = false)
        latexMarkwon = buildMarkwon(enableLatex = true)
    }

    fun render(textView: TextView, markdown: String) {
        textView.text = toSpanned(markdown)
    }

    fun toSpanned(markdown: String): Spanned {
        if (markdown.isEmpty()) return SpannableStringBuilder()

        val normalized = normalizeLatexDelimiters(markdown)
        val extracted = extractMathTokens(normalized)

        val baseSpanned =
            try {
                baseMarkwon.toMarkdown(extracted.markdown)
            } catch (_: Throwable) {
                SimpleMarkdownRenderer.render(extracted.markdown)
            }

        return mergeMathTokens(baseSpanned, extracted.tokens)
    }

    fun renderIncremental(currentText: String, newDelta: String): Spanned {
        return toSpanned(currentText + newDelta)
    }

    private fun mergeMathTokens(base: Spanned, tokens: List<MathToken>): Spanned {
        if (tokens.isEmpty()) return base

        val builder = SpannableStringBuilder(base)
        for (token in tokens) {
            while (true) {
                val fullText = builder.toString()
                val index = fullText.indexOf(token.placeholder)
                if (index < 0) break

                val rendered = renderMathToken(token)
                builder.replace(index, index + token.placeholder.length, rendered)
            }
        }
        return builder
    }

    private fun renderMathToken(token: MathToken): Spanned {
        val cacheKey = (if (token.displayMode) "D:" else "I:") + token.body
        mathCache.get(cacheKey)?.let { return it }

        val source = if (token.displayMode) "\n$$\n${token.body}\n$$\n" else "$$${token.body}$$"

        val rendered =
            try {
                latexMarkwon.toMarkdown(source)
            } catch (_: Throwable) {
                SpannableStringBuilder(token.original)
            }

        mathCache.put(cacheKey, rendered)
        return rendered
    }

    private fun extractMathTokens(markdown: String): ExtractedMath {
        val output = StringBuilder(markdown.length + 32)
        val tokens = mutableListOf<MathToken>()

        var i = 0
        var inFence = false
        while (i < markdown.length) {
            if (markdown.startsWith("```", i)) {
                inFence = !inFence
                output.append("```")
                i += 3
                continue
            }

            if (inFence) {
                output.append(markdown[i])
                i++
                continue
            }

            val ch = markdown[i]

            // 行内代码中的 $ 不参与公式识别。
            if (ch == '`') {
                val end = markdown.indexOf('`', i + 1)
                if (end < 0) {
                    output.append(ch)
                    i++
                } else {
                    output.append(markdown, i, end + 1)
                    i = end + 1
                }
                continue
            }

            val block = readDelimited(markdown, i, "$$", "$$", allowNewLine = true)
            if (block != null) {
                val body = block.body.trim()
                if (body.isNotEmpty() && looksLikeFormula(body)) {
                    val placeholder = "XLA_MATH_${tokens.size}_TOKEN"
                    tokens +=
                        MathToken(
                            placeholder = placeholder,
                            body = body,
                            displayMode = true,
                            original = block.original
                        )
                    output.append(placeholder)
                } else {
                    output.append(block.original)
                }
                i = block.nextIndex
                continue
            }

            val escapedBlock = readDelimited(markdown, i, "\\[", "\\]", allowNewLine = true)
            if (escapedBlock != null) {
                val body = escapedBlock.body.trim()
                if (body.isNotEmpty() && looksLikeFormula(body)) {
                    val placeholder = "XLA_MATH_${tokens.size}_TOKEN"
                    tokens +=
                        MathToken(
                            placeholder = placeholder,
                            body = body,
                            displayMode = true,
                            original = escapedBlock.original
                        )
                    output.append(placeholder)
                } else {
                    output.append(escapedBlock.original)
                }
                i = escapedBlock.nextIndex
                continue
            }

            val escapedInline = readDelimited(markdown, i, "\\(", "\\)", allowNewLine = false)
            if (escapedInline != null) {
                val body = escapedInline.body.trim()
                if (body.isNotEmpty() && looksLikeFormula(body)) {
                    val placeholder = "XLA_MATH_${tokens.size}_TOKEN"
                    tokens +=
                        MathToken(
                            placeholder = placeholder,
                            body = body,
                            displayMode = false,
                            original = escapedInline.original
                        )
                    output.append(placeholder)
                } else {
                    output.append(escapedInline.original)
                }
                i = escapedInline.nextIndex
                continue
            }

            val inline = readInlineMath(markdown, i)
            if (inline != null) {
                val body = inline.body.trim()
                if (body.isNotEmpty() && looksLikeFormula(body)) {
                    val placeholder = "XLA_MATH_${tokens.size}_TOKEN"
                    tokens +=
                        MathToken(
                            placeholder = placeholder,
                            body = body,
                            displayMode = false,
                            original = inline.original
                        )
                    output.append(placeholder)
                } else {
                    output.append(inline.original)
                }
                i = inline.nextIndex
                continue
            }

            output.append(ch)
            i++
        }

        return ExtractedMath(output.toString(), tokens)
    }

    private fun readDelimited(
        text: String,
        start: Int,
        open: String,
        close: String,
        allowNewLine: Boolean
    ): DelimitedMatch? {
        if (!text.startsWith(open, start)) return null
        if (isEscaped(text, start)) return null

        var searchFrom = start + open.length
        while (searchFrom < text.length) {
            val end = text.indexOf(close, searchFrom)
            if (end < 0) return null
            if (isEscaped(text, end)) {
                searchFrom = end + 1
                continue
            }

            val body = text.substring(start + open.length, end)
            if (!allowNewLine && body.contains('\n')) return null

            return DelimitedMatch(
                body = body,
                original = text.substring(start, end + close.length),
                nextIndex = end + close.length
            )
        }

        return null
    }

    private fun readInlineMath(text: String, start: Int): DelimitedMatch? {
        if (start >= text.length || text[start] != '$') return null
        if (isEscaped(text, start)) return null
        if (start + 1 < text.length && text[start + 1] == '$') return null

        var i = start + 1
        while (i < text.length) {
            val ch = text[i]
            if (ch == '\n') return null
            if (ch == '$') {
                if (isEscaped(text, i)) {
                    i++
                    continue
                }
                if (i + 1 < text.length && text[i + 1] == '$') {
                    i += 2
                    continue
                }
                return DelimitedMatch(
                    body = text.substring(start + 1, i),
                    original = text.substring(start, i + 1),
                    nextIndex = i + 1
                )
            }
            i++
        }
        return null
    }

    private fun isEscaped(text: String, index: Int): Boolean {
        var slashCount = 0
        var cursor = index - 1
        while (cursor >= 0 && text[cursor] == '\\') {
            slashCount++
            cursor--
        }
        return slashCount % 2 == 1
    }

    private fun normalizeLatexDelimiters(markdown: String): String {
        if (markdown.isEmpty()) return markdown

        var normalized = normalizeLatexCodeFences(markdown)
        normalized = normalizeMathEnvironments(normalized)

        return normalized
    }

    private fun normalizeLatexCodeFences(markdown: String): String {
        return fencedCodePattern.replace(markdown) { match ->
            val lang = match.groupValues.getOrNull(1)?.trim()?.lowercase().orEmpty()
            val body = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (body.isEmpty()) return@replace match.value

            val isLatexLang = lang in latexLangSet
            val isLatexDoc = looksLikeLatexDocument(body)
            val isFormulaBlock = looksLikeFormulaBlock(body)

            if (!isLatexLang && !isLatexDoc && !isFormulaBlock) {
                return@replace match.value
            }

            if (isLatexDoc) {
                return@replace body
            }

            val mathBody = stripMathWrappers(body).trim()
            if (mathBody.isEmpty() || !looksLikeFormula(mathBody)) {
                return@replace body
            }

            "\n$$\n$mathBody\n$$\n"
        }
    }

    private fun normalizeMathEnvironments(markdown: String): String {
        return equationEnvPattern.replace(markdown) { match ->
            val body = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (body.isEmpty()) match.value else "\n$$\n$body\n$$\n"
        }
    }

    private fun stripMathWrappers(content: String): String {
        var text = content.trim()
        if (text.startsWith("$$") && text.endsWith("$$") && text.length > 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("$") && text.endsWith("$") && text.length > 2) {
            text = text.substring(1, text.length - 1).trim()
        } else if (text.startsWith("\\[") && text.endsWith("\\]") && text.length > 4) {
            text = text.substring(2, text.length - 2).trim()
        } else if (text.startsWith("\\(") && text.endsWith("\\)") && text.length > 4) {
            text = text.substring(2, text.length - 2).trim()
        }

        val env = equationEnvPattern.find(text)
        if (env != null) {
            text = env.groupValues.getOrNull(1)?.trim().orEmpty()
        }
        return text
    }

    private fun looksLikeFormulaBlock(content: String): Boolean {
        if (looksLikeLatexDocument(content)) return false
        val text = content.trim()
        if (text.startsWith("\\[") && text.endsWith("\\]")) return true
        if (text.startsWith("\\(") && text.endsWith("\\)")) return true
        if (text.startsWith("$$") && text.endsWith("$$")) return true
        if (equationEnvPattern.containsMatchIn(text)) return true
        return looksLikeFormula(text)
    }

    private fun looksLikeFormula(content: String): Boolean {
        if (content.isBlank()) return false

        if (content.any { it == '\\' || it == '^' || it == '_' || it == '{' || it == '}' }) {
            return true
        }

        var letterCount = 0
        var digitCount = 0
        var operatorCount = 0
        for (ch in content) {
            when {
                ch.isLetter() -> letterCount++
                ch.isDigit() -> digitCount++
                ch in setOf('+', '-', '*', '/', '=', '(', ')', '[', ']', '<', '>') -> operatorCount++
            }
        }

        return operatorCount > 0 && (letterCount + digitCount) >= 2
    }

    private fun looksLikeLatexDocument(content: String): Boolean {
        val text = content.lowercase()
        return text.contains("\\documentclass") ||
            text.contains("\\usepackage") ||
            text.contains("\\begin{document}") ||
            text.contains("\\end{document}") ||
            text.contains("\\maketitle") ||
            text.contains("\\tableofcontents") ||
            text.contains("\\section{") ||
            text.contains("\\subsection{")
    }

    private data class MathToken(
        val placeholder: String,
        val body: String,
        val displayMode: Boolean,
        val original: String
    )

    private data class ExtractedMath(val markdown: String, val tokens: List<MathToken>)

    private data class DelimitedMatch(
        val body: String,
        val original: String,
        val nextIndex: Int
    )

    companion object {
        @Volatile private var instance: MarkdownRenderer? = null

        private val fencedCodePattern =
            Regex("""```([a-zA-Z0-9_-]*)\s*\n(.*?)\n?```""", setOf(RegexOption.DOT_MATCHES_ALL))
        private val equationEnvPattern =
            Regex(
                """\\begin\{(?:equation|align|aligned|gather|multline|eqnarray)\*?\}(.*?)\\end\{(?:equation|align|aligned|gather|multline|eqnarray)\*?\}""",
                setOf(RegexOption.DOT_MATCHES_ALL)
            )
        private val latexLangSet = setOf("latex", "tex", "math")

        fun getInstance(context: Context): MarkdownRenderer {
            return instance ?: synchronized(this) {
                instance ?: MarkdownRenderer(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 纯 Kotlin 的轻量 Markdown 回退渲染器。
 */
object SimpleMarkdownRenderer {

    fun render(text: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val lines = text.split("\n")

        var inCodeBlock = false
        val codeBlockContent = StringBuilder()

        for ((index, line) in lines.withIndex()) {
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    val codeRendered = renderCodeBlock(codeBlockContent.toString())
                    builder.append(codeRendered)
                    codeBlockContent.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                if (index < lines.size - 1) builder.append("\n")
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line)
                if (index < lines.size - 1) codeBlockContent.append("\n")
                continue
            }

            val processedLine = processLine(line)
            builder.append(processedLine)
            if (index < lines.size - 1) builder.append("\n")
        }

        processInlineFormatting(builder)
        return builder
    }

    private fun renderCodeBlock(code: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append("\n")
        val start = builder.length
        builder.append(code)
        val end = builder.length
        builder.append("\n")

        builder.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(0.95f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }

    private fun processLine(line: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        when {
            line.startsWith("### ") -> {
                val content = line.substring(4)
                builder.append(content)
                builder.setSpan(StyleSpan(Typeface.BOLD), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(1.15f), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            line.startsWith("## ") -> {
                val content = line.substring(3)
                builder.append(content)
                builder.setSpan(StyleSpan(Typeface.BOLD), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(1.25f), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            line.startsWith("# ") -> {
                val content = line.substring(2)
                builder.append(content)
                builder.setSpan(StyleSpan(Typeface.BOLD), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(1.4f), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            line.startsWith("- ") || line.startsWith("* ") -> {
                val content = line.substring(2)
                builder.append("  - $content")
            }

            line.matches(Regex("^\\d+\\.\\s.*")) -> {
                val match = Regex("^(\\d+)\\.\\s(.*)").find(line)
                if (match != null) {
                    builder.append("  ${match.groupValues[1]}. ${match.groupValues[2]}")
                } else {
                    builder.append(line)
                }
            }

            line.startsWith("> ") -> {
                val content = "  ${line.substring(2)}"
                builder.append(content)
                builder.setSpan(StyleSpan(Typeface.ITALIC), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            else -> builder.append(line)
        }

        return builder
    }

    private fun processInlineFormatting(builder: SpannableStringBuilder) {
        processCodePattern(builder)
        processBoldPattern(builder)
        processItalicPattern(builder)
    }

    private fun processCodePattern(builder: SpannableStringBuilder) {
        val pattern = Regex("`([^`]+?)`")
        var offset = 0
        val text = builder.toString()

        pattern.findAll(text).toList().forEach { match ->
            val start = match.range.first - offset
            val end = match.range.last + 1 - offset
            val content = match.groupValues[1]

            builder.replace(start, end, content)
            builder.setSpan(TypefaceSpan("monospace"), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(0.94f), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            offset += match.value.length - content.length
        }
    }

    private fun processBoldPattern(builder: SpannableStringBuilder) {
        val pattern = Regex("\\*\\*([^*]+?)\\*\\*")
        var offset = 0
        val text = builder.toString()

        pattern.findAll(text).toList().forEach { match ->
            val start = match.range.first - offset
            val end = match.range.last + 1 - offset
            val content = match.groupValues[1]

            builder.replace(start, end, content)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(1.02f), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            offset += match.value.length - content.length
        }
    }

    private fun processItalicPattern(builder: SpannableStringBuilder) {
        val pattern = Regex("(?<!\\*)\\*(?!\\*)([^*]+?)\\*(?!\\*)")
        var offset = 0
        val text = builder.toString()

        pattern.findAll(text).toList().forEach { match ->
            val start = match.range.first - offset
            val end = match.range.last + 1 - offset
            val content = match.groupValues[1]

            builder.replace(start, end, content)
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            offset += match.value.length - content.length
        }
    }

    fun renderCodeBlock(code: String, language: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        if (language.isNotEmpty()) {
            builder.append("$language\n")
            builder.setSpan(RelativeSizeSpan(0.85f), 0, language.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val codeStart = builder.length
        builder.append(code)

        builder.setSpan(TypefaceSpan("monospace"), codeStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }
}

/** Prism4j 语法定位器。 */
class GrammarLocatorDef : io.noties.prism4j.GrammarLocator {
    override fun grammar(prism4j: Prism4j, language: String): io.noties.prism4j.Prism4j.Grammar? {
        return null
    }

    override fun languages(): MutableSet<String> {
        return mutableSetOf()
    }
}
