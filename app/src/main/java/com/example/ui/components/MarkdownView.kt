package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

/**
 * Universal Markdown renderer for Compose.
 * Supports headings (#, ##, ###, ####), lists (bullet, checklist, numbered),
 * blockquotes (>), code blocks (```), inline code (`code`), bold (**), italic (*),
 * strikethrough (~~), mentions (@user), issue links (#123), and dividers (---).
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val rawLines = markdown.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        rawLines.forEach { rawLine ->
            val trimmed = rawLine.trim()

            // Code block delimiter toggle (``` or ~~~)
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                if (inCodeBlock) {
                    // Flush accumulated code block
                    val fullCode = codeBlockLines.joinToString("\n")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = fullCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                return@forEach
            }

            if (inCodeBlock) {
                codeBlockLines.add(rawLine)
                return@forEach
            }

            when {
                // Horizontal divider (---, ***, ___)
                trimmed.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle
                    )
                }

                // Custom Alert: lines starting with !important
                trimmed.startsWith("!important", ignoreCase = true) -> {
                    val alertText = trimmed.removePrefix("!important").trim()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.accentWarning.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accentWarning.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FluentIcons.Warning24Regular,
                                contentDescription = "Alert",
                                tint = colors.accentWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(
                                    text = alertText.ifBlank { "Important update note" },
                                    primaryColor = colors.primary,
                                    codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                    codeColor = colors.primary
                                ),
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = colors.textPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Heading 1: #
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(
                            text = trimmed.removePrefix("# "),
                            primaryColor = colors.primary,
                            codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                            codeColor = colors.primary
                        ),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                // Heading 2: ##
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(
                            text = trimmed.removePrefix("## "),
                            primaryColor = colors.primary,
                            codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                            codeColor = colors.primary
                        ),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                // Heading 3: ###
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(
                            text = trimmed.removePrefix("### "),
                            primaryColor = colors.primary,
                            codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                            codeColor = colors.primary
                        ),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                    )
                }

                // Heading 4+: ####
                trimmed.startsWith("#### ") -> {
                    Text(
                        text = parseInlineMarkdown(
                            text = trimmed.removePrefix("#### "),
                            primaryColor = colors.primary,
                            codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                            codeColor = colors.primary
                        ),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // Checklist item checked: - [x] or * [x]
                trimmed.startsWith("- [x] ", ignoreCase = true) || trimmed.startsWith("* [x] ", ignoreCase = true) -> {
                    val itemText = trimmed.drop(6).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = FluentIcons.CheckmarkCircle24Regular,
                            contentDescription = "Completed",
                            tint = colors.accentSuccess,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(
                                text = itemText,
                                primaryColor = colors.primary,
                                codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                codeColor = colors.primary
                            ),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Checklist item unchecked: - [ ] or * [ ]
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val itemText = trimmed.drop(6).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.borderSubtle)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(
                                text = itemText,
                                primaryColor = colors.primary,
                                codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                codeColor = colors.primary
                            ),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Bullet List: - or * or • or +
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || trimmed.startsWith("+ ") -> {
                    val bulletText = trimmed.drop(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                        Text(
                            text = parseInlineMarkdown(
                                text = bulletText,
                                primaryColor = colors.primary,
                                codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                codeColor = colors.primary
                            ),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Numbered List: 1. , 2.
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val prefix = trimmed.substringBefore(". ") + "."
                    val content = trimmed.substringAfter(". ")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = prefix,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = colors.primary,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(
                                text = content,
                                primaryColor = colors.primary,
                                codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                codeColor = colors.primary
                            ),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Blockquote: > text
                trimmed.startsWith("> ") -> {
                    val quoteText = trimmed.removePrefix("> ")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = colors.primary,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                            .background(colors.surfaceTint.copy(alpha = 0.35f))
                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(
                                text = quoteText,
                                primaryColor = colors.primary,
                                codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                                codeColor = colors.primary
                            ),
                            fontFamily = InterFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.5.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Empty line / spacer
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Normal paragraph text
                else -> {
                    Text(
                        text = parseInlineMarkdown(
                            text = trimmed,
                            primaryColor = colors.primary,
                            codeBg = colors.surfaceTint.copy(alpha = 0.5f),
                            codeColor = colors.primary
                        ),
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Clean up unclosed code block if file ends inside one
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            val fullCode = codeBlockLines.joinToString("\n")
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = fullCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

/**
 * Backward-compatible alias for MarkdownView
 */
@Composable
fun MarkdownSyllabusView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    MarkdownView(markdown = markdown, modifier = modifier)
}

/**
 * Parses bold (**text**), italic (*text*), code (`code`), strikethrough (~~text~~),
 * mentions (@user), and issue links (#123) into an AnnotatedString.
 */
fun parseInlineMarkdown(
    text: String,
    primaryColor: Color = Color.Unspecified,
    codeBg: Color = Color.Transparent,
    codeColor: Color = Color.Unspecified
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex(
            "(`([^`]+)`)|" +
            "(\\*\\*([^*]+)\\*\\*)|" +
            "(__([^_]+)__)|" +
            "(\\*([^*]+)\\*)|" +
            "(_([^_]+)_)|" +
            "(~~([^~]+)~~)|" +
            "(\\[([^\\]]+)\\]\\(([^)]+)\\))|" +
            "(@[a-zA-Z0-9_-]+)|" +
            "(#[0-9]+)"
        )
        val matches = pattern.findAll(text)

        for (match in matches) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val v = match.value
            when {
                // Inline code `code`
                v.startsWith("`") && v.endsWith("`") -> {
                    val codeContent = v.substring(1, v.length - 1)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            background = if (codeBg != Color.Transparent) codeBg else Color(0x22808080),
                            color = if (codeColor != Color.Unspecified) codeColor else Color.Unspecified,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" $codeContent ")
                    pop()
                }

                // Bold: **text** or __text__
                (v.startsWith("**") && v.endsWith("**")) || (v.startsWith("__") && v.endsWith("__")) -> {
                    val boldContent = v.substring(2, v.length - 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldContent)
                    pop()
                }

                // Italic: *text* or _text_
                (v.startsWith("*") && v.endsWith("*")) || (v.startsWith("_") && v.endsWith("_")) -> {
                    val italicContent = v.substring(1, v.length - 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                }

                // Strikethrough: ~~text~~
                v.startsWith("~~") && v.endsWith("~~") -> {
                    val strikeContent = v.substring(2, v.length - 2)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(strikeContent)
                    pop()
                }

                // Link: [title](url)
                v.startsWith("[") && v.contains("](") && v.endsWith(")") -> {
                    val title = v.substringAfter("[").substringBefore("](")
                    pushStyle(
                        SpanStyle(
                            color = if (primaryColor != Color.Unspecified) primaryColor else Color(0xFF6366F1),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(title)
                    pop()
                }

                // Mention: @user
                v.startsWith("@") -> {
                    pushStyle(
                        SpanStyle(
                            color = if (primaryColor != Color.Unspecified) primaryColor else Color(0xFF6366F1),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(v)
                    pop()
                }

                // PR / Issue link: #123
                v.startsWith("#") && v.drop(1).all { it.isDigit() } -> {
                    pushStyle(
                        SpanStyle(
                            color = if (primaryColor != Color.Unspecified) primaryColor else Color(0xFF6366F1),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(v)
                    pop()
                }

                else -> append(v)
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
