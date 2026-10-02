package com.example.ui.reader

/**
 * Data model for hierarchical PDF Table of Contents bookmarks.
 *
 * @param title The title/label of the chapter or section.
 * @param pageIdx The 0-based page index targeted by the bookmark.
 * @param children Sub-chapters or nested bookmark sections.
 */
data class TocItem(
    val title: String,
    val pageIdx: Long,
    val children: List<TocItem> = emptyList()
)
