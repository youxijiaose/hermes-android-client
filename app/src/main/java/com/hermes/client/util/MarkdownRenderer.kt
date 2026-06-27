package com.hermes.client.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.text.Html
import android.text.Spanned
import android.text.style.*
import android.widget.TextView
import java.util.regex.Pattern

/**
 * 轻量级 Markdown 渲染器
 * 将 Markdown 文本转换为 HTML，然后渲染为 Android Spanned
 */
class MarkdownRenderer {

    companion object {
        // Regex patterns for Markdown — compiled once, reused
        private val CODE_BLOCK_PATTERN = Regex("""```(\w*)\n([\s\S]*?)```""")
        private val INLINE_CODE_PATTERN = Regex("""`([^`]+)`""")
        private val BOLD_PATTERN = Regex("""\*\*([^*]+)\*\*""")
        private val ITALIC_PATTERN = Regex("""\*([^*]+)\*""")
        private val LINK_PATTERN = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
        private val HEADER_PATTERN = Regex("""^#{1,6}\s+(.+)$""", RegexOption.MULTILINE)
        private val BLOCKQUOTE_PATTERN = Regex("""^>\s*(.+)$""", RegexOption.MULTILINE)
        private val UNORDERED_LIST_PATTERN = Regex("""^[-*+]\s+(.+)$""", RegexOption.MULTILINE)
        private val ORDERED_LIST_PATTERN = Regex("""^\d+\.\s+(.+)$""", RegexOption.MULTILINE)
        private val HORIZONTAL_RULE_PATTERN = Regex("""^---+$""", RegexOption.MULTILINE)

        fun renderMarkdown(text: String, textView: TextView) {
            val html = markdownToHtml(text)
            textView.text = fromHtml(html)
        }

        fun markdownToHtml(text: String): String {
            var result = text

            // 1. 代码块（必须最先处理，防止内部 markdown 被误解析）
            result = CODE_BLOCK_PATTERN.replace(result) { match ->
                val language = match.groups[1]?.value ?: ""
                val code = match.groups[2]?.value?.trim() ?: ""
                "<pre><code class=\"language-$language\">${escapeHtml(code)}</code></pre>"
            }

            // 2. 行内代码
            result = INLINE_CODE_PATTERN.replace(result) { match ->
                "<code>${escapeHtml(match.groups[1]?.value ?: "")}</code>"
            }

            // 3. 粗体
            result = BOLD_PATTERN.replace(result) { match ->
                "<strong>${match.groups[1]?.value ?: ""}</strong>"
            }

            // 4. 斜体（注意：必须在粗体之后，且不能匹配 ** 内部的 *）
            result = ITALIC_PATTERN.replace(result) { match ->
                val content = match.groups[1]?.value ?: ""
                if (content.contains("**")) "" else "<em>$content</em>"
            }

            // 5. 链接
            result = LINK_PATTERN.replace(result) { match ->
                val url = match.groups[2]?.value ?: ""
                val label = match.groups[1]?.value ?: ""
                "<a href=\"$url\">$label</a>"
            }

            // 6. 标题
            result = HEADER_PATTERN.replace(result) { match ->
                val level = match.value.takeWhile { it == '#' }.length
                "<h$level>${match.groups[1]?.value ?: match.value.substringAfter('#')}</h$level>"
            }

            // 7. 引用
            result = BLOCKQUOTE_PATTERN.replace(result) { match ->
                "<blockquote>${match.groups[1]?.value ?: ""}</blockquote>"
            }

            // 8. 无序列表
            result = UNORDERED_LIST_PATTERN.replace(result) { match ->
                "<li>${match.groups[1]?.value ?: ""}</li>"
            }

            // 9. 有序列表
            result = ORDERED_LIST_PATTERN.replace(result) { match ->
                "<li>${match.groups[1]?.value ?: ""}</li>"
            }

            // 10. 水平线
            result = HORIZONTAL_RULE_PATTERN.replace(result) {
                "<hr>"
            }

            // 包裹段落
            if (!result.startsWith("<") && !result.startsWith("\n")) {
                result = "<p>$result</p>"
            }

            return result
        }

        private fun escapeHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }

        @Suppress("DEPRECATION")
        fun fromHtml(html: String): Spanned {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        }
    }
}

/**
 * TextView 扩展函数：设置 Markdown 渲染后的文本
 * 使用协程在 IO 线程渲染，主线程设置，避免阻塞 UI
 */
@OptIn(DelicateCoroutinesApi::class)
fun TextView.setMarkdown(text: String) {
    if (text.isBlank()) {
        this.text = ""
        return
    }
    GlobalScope.launch {
        try {
            val html = withContext(Dispatchers.Default) {
                MarkdownRenderer.markdownToHtml(text)
            }
            val spanned = withContext(Dispatchers.Main) {
                MarkdownRenderer.fromHtml(html)
            }
            // Only set if this TextView is still attached to window
            if (this@setMarkdown.isAttachedToWindow) {
                this@setMarkdown.text = spanned
            }
        } catch (e: Exception) {
            // Fallback: set raw text if rendering fails
            if (this@setMarkdown.isAttachedToWindow) {
                this@setMarkdown.text = text
            }
        }
    }
}