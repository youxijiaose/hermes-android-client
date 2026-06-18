package com.hermes.client.util

import android.text.Html
import android.text.Spanned
import android.text.style.*
import android.widget.TextView
import java.util.regex.Pattern

/**
 * 轻量级 Markdown 渲染器
 * 支持：标题、粗体、斜体、代码块、行内代码、列表、引用、链接
 */
object MarkdownRenderer {

    private val CODE_BLOCK_PATTERN = Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```")
    private val INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`")
    private val BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*|__([^_]+)__")
    private val ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*|_([^_]+)_")
    private val LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\\\(([^)]+)\\)")
    private val HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE)
    private val BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*(.+)$", Pattern.MULTILINE)
    private val UNORDERED_LIST_PATTERN = Pattern.compile("^[-*+]\\s+(.+)$", Pattern.MULTILINE)
    private val ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.+)$", Pattern.MULTILINE)
    private val HORIZONTAL_RULE_PATTERN = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$")

    fun renderMarkdown(text: String): CharSequence {
        var result = text

        // 1. 代码块 (先处理，避免内部标记被解析)
        val codeBlocks = mutableListOf<String>()
        result = result.replace(CODE_BLOCK_PATTERN) { match ->
            val language = match.groupValues[1]
            val code = match.groupValues[2]
            val placeholder = "%%CODE_BLOCK_${codeBlocks.size}%%"
            codeBlocks.add("<pre><code class=\"language-$language\">${escapeHtml(code)}</code></pre>")
            placeholder
        }

        // 2. 行内代码
        result = result.replace(INLINE_CODE_PATTERN) { match ->
            "<code>${escapeHtml(match.groupValues[1])}</code>"
        }

        // 3. 粗体
        result = result.replace(BOLD_PATTERN) { match ->
            "<strong>${match.groupValues[1]}${match.groupValues[2]}</strong>"
        }

        // 4. 斜体
        result = result.replace(ITALIC_PATTERN) { match ->
            "<em>${match.groupValues[3]}${match.groupValues[4]}</em>"
        }

        // 5. 链接
        result = result.replace(LINK_PATTERN) { match ->
            "<a href=\"${match.groupValues[2]}\">${match.groupValues[1]}</a>"
        }

        // 6. 标题
        result = result.replace(HEADER_PATTERN) { match ->
            val level = match.groupValues[1].length
            val content = match.groupValues[2]
            "<h$level>$content</h$level>"
        }

        // 7. 引用块
        result = result.replace(BLOCKQUOTE_PATTERN) { match ->
            "<blockquote>${match.groupValues[1]}</blockquote>"
        }

        // 8. 无序列表
        result = result.replace(UNORDERED_LIST_PATTERN) { match ->
            "<li>${match.groupValues[1]}</li>"
        }
        result = result.replace(Regex("(<li>.*</li>)+")) { match ->
            "<ul>${match.groupValues[0]}</ul>"
        }

        // 9. 有序列表
        result = result.replace(ORDERED_LIST_PATTERN) { match ->
            "<li>${match.groupValues[1]}</li>"
        }
        result = result.replace(Regex("(<li>.*</li>)+")) { match ->
            "<ol>${match.groupValues[0]}</ol>"
        }

        // 10. 水平线
        result = result.replace(HORIZONTAL_RULE_PATTERN) { match ->
            "<hr/>"
        }

        // 11. 换行
        result = result.replace("\n", "<br/>")

        // 恢复代码块
        codeBlocks.forEachIndexed { index, code ->
            result = result.replace("%%CODE_BLOCK_$index%%", code)
        }

        return Html.fromHtml(result, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    /**
     * 将 Markdown 文本应用到 TextView
     */
    fun applyTo(textView: TextView, markdown: String) {
        textView.text = renderMarkdown(markdown)
        textView.linksClickable = true
    }
}

/**
 * 扩展函数：TextView 设置 Markdown
 */
fun TextView.setMarkdown(markdown: String) {
    MarkdownRenderer.applyTo(this, markdown)
}
