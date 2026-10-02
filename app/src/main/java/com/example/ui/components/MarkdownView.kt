package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

/**
 * Live Markdown parser component for Compose that renders headers, bold/italic,
 * ordered/bulleted lists, blockquotes, and custom alert callouts (!important).
 */
@Composable
fun MarkdownSyllabusView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val lines = markdown.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                // Custom Alert: lines starting with !important
                line.trimStart().startsWith("!important", ignoreCase = true) -> {
                    val alertText = line.trimStart().removePrefix("!important").trim()
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = colors.accentWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(alertText.ifBlank { "Crucial exam milestone topic!" }),
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
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ")),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                // Heading 2: ##
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ")),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                // Heading 3: ###
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ")),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // Blockquote: > text
                line.startsWith("> ") -> {
                    val quoteText = line.removePrefix("> ")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = colors.primary,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 3.5.dp.toPx()
                                )
                            }
                            .background(colors.surfaceTint.copy(alpha = 0.5f))
                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(quoteText),
                            fontFamily = InterFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Bullet List: - or *
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletText = line.drop(2)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Numbered List: 1. , 2.
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val prefix = line.substringBefore(". ") + "."
                    val content = line.substringAfter(". ")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = prefix,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.primary,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content),
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Empty line / spacer
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Normal paragraph text
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

/**
 * Parses bold (**text**) and italic (*text*) inside strings to AnnotatedString.
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("(\\*\\*(.*?)\\*\\*)|(\\*(.*?)\\*)")
        val matches = pattern.findAll(text)

        for (match in matches) {
            // Append text before match
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val fullMatch = match.value
            if (fullMatch.startsWith("**") && fullMatch.endsWith("**")) {
                val boldContent = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(boldContent)
                pop()
            } else if (fullMatch.startsWith("*") && fullMatch.endsWith("*")) {
                val italicContent = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(italicContent)
                pop()
            } else {
                append(fullMatch)
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
