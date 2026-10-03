package com.example.ui.screens.desk

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MulberryColors

/**
 * Obsidian-style in-place live Markdown VisualTransformation.
 * Formats headings, bold, italic, checklists, and wikilinks directly inside BasicTextField
 * without altering the underlying raw text length (preserving cursor index 1:1).
 */
class MarkdownVisualTransformation(
    private val colors: MulberryColors
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(raw)
        val mutedMarkerColor = colors.textMuted.copy(alpha = 0.55f)

        // 1. Line-by-line block formatting (Headings, Checklists, Quotes)
        val lines = raw.lines()
        var currentOffset = 0

        for (line in lines) {
            val lineLength = line.length
            val lineEnd = currentOffset + lineLength
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length

            if (trimmed.startsWith("# ")) {
                // H1 Heading
                builder.addStyle(
                    SpanStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    currentOffset,
                    lineEnd
                )
                // Subdue '# ' prefix
                val markerEnd = (currentOffset + indent + 2).coerceAtMost(lineEnd)
                builder.addStyle(
                    SpanStyle(
                        color = mutedMarkerColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    currentOffset + indent,
                    markerEnd
                )
            } else if (trimmed.startsWith("## ")) {
                // H2 Heading
                builder.addStyle(
                    SpanStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    currentOffset,
                    lineEnd
                )
                val markerEnd = (currentOffset + indent + 3).coerceAtMost(lineEnd)
                builder.addStyle(
                    SpanStyle(
                        color = mutedMarkerColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    currentOffset + indent,
                    markerEnd
                )
            } else if (trimmed.startsWith("### ")) {
                // H3 Heading
                builder.addStyle(
                    SpanStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    ),
                    currentOffset,
                    lineEnd
                )
                val markerEnd = (currentOffset + indent + 4).coerceAtMost(lineEnd)
                builder.addStyle(
                    SpanStyle(
                        color = mutedMarkerColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    currentOffset + indent,
                    markerEnd
                )
            } else if (trimmed.startsWith("> ")) {
                // Blockquote
                builder.addStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = colors.textSecondary
                    ),
                    currentOffset,
                    lineEnd
                )
                builder.addStyle(
                    SpanStyle(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    currentOffset + indent,
                    (currentOffset + indent + 2).coerceAtMost(lineEnd)
                )
            } else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [ ]")) {
                // Uncompleted checklist task
                val markerEnd = (currentOffset + indent + 5).coerceAtMost(lineEnd)
                builder.addStyle(
                    SpanStyle(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    currentOffset + indent,
                    markerEnd
                )
            } else if (trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                       trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) {
                // Completed checklist task: dim and strikethrough the line text
                val markerEnd = (currentOffset + indent + 5).coerceAtMost(lineEnd)
                builder.addStyle(
                    SpanStyle(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    currentOffset + indent,
                    markerEnd
                )
                if (markerEnd < lineEnd) {
                    builder.addStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = colors.textMuted
                        ),
                        markerEnd,
                        lineEnd
                    )
                }
            }

            currentOffset += lineLength + 1 // +1 for the newline character
        }

        // 2. Inline Bold: **text**
        val boldRegex = Regex("""\*\*([^*]+)\*\*""")
        for (match in boldRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary),
                start,
                end
            )
            // Dim markers
            builder.addStyle(SpanStyle(color = mutedMarkerColor, fontWeight = FontWeight.Normal), start, start + 2)
            builder.addStyle(SpanStyle(color = mutedMarkerColor, fontWeight = FontWeight.Normal), end - 2, end)
        }

        // 3. Inline Italic: *text* (excluding **)
        val italicRegex = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
        for (match in italicRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                start,
                end
            )
            builder.addStyle(SpanStyle(color = mutedMarkerColor), start, start + 1)
            builder.addStyle(SpanStyle(color = mutedMarkerColor), end - 1, end)
        }

        // 4. Book Wikilinks: [[Book#p.123]]
        val wikilinkRegex = Regex("""\[\[([^\]]+)\]\]""")
        for (match in wikilinkRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                SpanStyle(
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    background = colors.primary.copy(alpha = 0.12f)
                ),
                start,
                end
            )
        }

        // 5. Snip embeds: ![[snip.webp|Caption]]
        val snipRegex = Regex("""!\[\[([^\]]+)\]\]""")
        for (match in snipRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                SpanStyle(
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    background = colors.surfaceTint.copy(alpha = 0.6f)
                ),
                start,
                end
            )
        }

        // 6. Tags: #Tag
        val tagRegex = Regex("""(?<!\w)#([a-zA-Z0-9_\-]+)""")
        for (match in tagRegex.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(
                SpanStyle(
                    color = colors.primary,
                    fontWeight = FontWeight.Medium
                ),
                start,
                end
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
