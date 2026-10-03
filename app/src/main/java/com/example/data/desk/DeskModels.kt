package com.example.data.desk

import java.io.File

data class DeskFolder(
    val id: String,
    val name: String,
    val directory: File,
    val noteCount: Int,
    val pendingTasksCount: Int,
    val completedTasksCount: Int,
    val boundBookFileName: String? = null
)

data class DeskNote(
    val file: File,
    val folderName: String,
    val title: String,
    val lastModified: Long,
    val excerpt: String,
    val tasksCount: Int,
    val completedTasksCount: Int
)

data class DeskTask(
    val id: String,
    val sourceFile: File,
    val folderName: String,
    val noteTitle: String,
    val lineIndex: Int,
    val rawText: String,
    val content: String,
    val isCompleted: Boolean,
    val linkedBook: String? = null,
    val linkedPage: Int? = null,
    val dueDate: String? = null,
    val tags: List<String> = emptyList()
)

object DeskMarkdownPatterns {
    // Matches: - [ ] Task description OR - [x] Completed task
    val TASK_PATTERN = Regex("""^(\s*)-\s*\[([ xX])\]\s+(.+)$""", RegexOption.MULTILINE)

    // Matches: [[Book Title#p.123]] or [[Book Title]]
    val BOOK_WIKILINK_PATTERN = Regex("""\[\[([^#\]]+)(?:#p\.?(\d+))?\]\]""")

    // Matches: ![[image_name.webp|Optional Caption]]
    val SNIP_EMBED_PATTERN = Regex("""!\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")

    // Matches: @due(YYYY-MM-DD)
    val DUE_DATE_PATTERN = Regex("""@due\((\d{4}-\d{2}-\d{2})\)""")

    // Matches: #Tag
    val TAG_PATTERN = Regex("""#([a-zA-Z0-9_\-]+)""")
}
