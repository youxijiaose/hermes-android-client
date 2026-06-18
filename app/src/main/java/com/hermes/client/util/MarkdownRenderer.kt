package com.hermes.client.util

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
        // Regex patterns for Markdown
        private val CODE_BLOCK_PATTERN = Pattern.compile("\x60\x60\x60(\\w*)\\n([\\s\\S]*?)\\x60\x60\x60")
        private val INLINE_CODE_PATTERN = Pattern.compile("\x60([^\x60]+)\x60")
        private val BOLD_PATTERN = Pattern.compile("\*\*([^*]+)\*\*")
        private val ITALIC_PATTERN = Pattern.compile("\*([^*]+)\*")
        private val LINK_PATTERN = Pattern.compile("\[([^\\]]+)\]\(([^\\)]+)\)")
        private val HEADER_PATTERN = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE)
        private val BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*(.+)$", Pattern.MULTILINE)
        private val UNORDERED_LIST_PATTERN = Pattern.compile("^[-*+]\\s+(.+)$", Pattern.MULTILINE)
        private val ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.+)$", Pattern.MULTILINE)
        private val HORIZONTAL_RULE_PATTERN = Pattern.compile("^---+$", Pattern.MULTILINE)

        fun renderMarkdown(text: String, textView: TextView) {
            val html = markdownToHtml(text)
            textView.text = fromHtml(html)
        }

        fun markdownToHtml(text: String): String {
            var result = text

            // 1. 代码块
            result = result.replace(Regex("\x60\x60\x60(\\w*)\\n([\\s\\S]*?)\\x60\x60\x60")) { match ->
                val language = match.groupValues[1]
                val code = match.groupValues[2].trim()
                "<pre><code class=\"language-$language\">${escapeHtml(code)}</code></pre>"
            }

            // 2. 行内代码
            result = result.replace(Regex("\x60([^\x60]+)\x60")) { match ->
                "<code>${escapeHtml(match.groupValues[1])}</code>"
            }

            // 3. 粗体
            result = result.replace(Regex("\*\*([^*]+)\*\*")) { match ->
                "<strong>${match.groupValues[1]}</strong>"
            }

            // 4. 斜体
            result = result.replace(Regex("\*([^*]+)\*")) { match ->
                "<em>${match.groupValues[1]}</em>"
            }

            // 5. 链接
            result = result.replace(Regex("\[([^\\]]+)\]\(([^\\)]+)\)")) { match ->
                "<a href=\"${match.groupValues[2]}\">${match.groupValues[1]}</a>"
            }

            // 6. 标题
            result = result.replace(Regex("^#{1,6}\\s+(.+)$", RegexOption.MULTILINE)) { match ->
                val level = match.value.takeWhile { it == '#' }.length
                "<h$level>${match.groupValues[1]}</h$level>"
            }

            // 7. 引用
            result = result.replace(Regex("^>\\s*(.+)$", RegexOption.MULTILINE)) { match ->
                "<blockquote>${match.groupValues[1]}</blockquote>"
            }

            // 8. 无序列表
            result = result.replace(Regex("^[-*+]\\s+(.+)$", RegexOption.MULTILINE)) { match ->
                "<li>${match.groupValues[1]}</li>"
            }

            // 9. 有序列表
            result = result.replace(Regex("^\\d+\\.\\s+(.+)$", RegexOption.MULTILINE)) { match ->
                "<li>${match.groupValues[1]}</li>"
            }

            // 10. 水平线
            result = result.replace(Regex("^---+$", RegexOption.MULTILINE)) {
                "<hr>"
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
        private fun fromHtml(html: String): Spanned {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        }
    }
}

/**
 * TextView 扩展函数：设置 Markdown 渲染后的文本
 * 使用方式：textView.setMarkdown("Hello **world**")
 */
fun TextView.setMarkdown(text: String) {
    if (text.isBlank()) {
        this.text = ""
    } else {
        MarkdownRenderer.renderMarkdown(text, this)
    }
}