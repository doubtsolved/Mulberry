package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FluentIcons {
    // Tab 1: Book24Regular
    val Book24Regular: ImageVector = ImageVector.Builder(
        name = "Book24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 19.5f)
            curveTo(4f, 18.12f, 5.12f, 17f, 6.5f, 17f)
            horizontalLineTo(20f)
            moveTo(6.5f, 2f)
            horizontalLineTo(20f)
            verticalLineTo(22f)
            horizontalLineTo(6.5f)
            curveTo(5.12f, 22f, 4f, 20.88f, 4f, 19.5f)
            verticalLineTo(4.5f)
            curveTo(4f, 3.12f, 5.12f, 2f, 6.5f, 2f)
            close()
        }
    }.build()

    // Tab 1: Book24Filled
    val Book24Filled: ImageVector = ImageVector.Builder(
        name = "Book24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.5f, 2f)
            horizontalLineTo(19.25f)
            curveTo(19.66f, 2f, 20f, 2.34f, 20f, 2.75f)
            verticalLineTo(21.25f)
            curveTo(20f, 21.66f, 19.66f, 22f, 19.25f, 22f)
            horizontalLineTo(6.5f)
            curveTo(5.12f, 22f, 4f, 20.88f, 4f, 19.5f)
            verticalLineTo(4.5f)
            curveTo(4f, 3.12f, 5.12f, 2f, 6.5f, 2f)
            close()
            moveTo(6.5f, 18f)
            curveTo(5.67f, 18f, 5f, 18.67f, 5f, 19.5f)
            curveTo(5f, 20.33f, 5.67f, 21f, 6.5f, 21f)
            horizontalLineTo(19f)
            verticalLineTo(18f)
            horizontalLineTo(6.5f)
            close()
        }
    }.build()

    // Tab 2: TaskListSquareLtr24Regular
    val TaskListSquareLtr24Regular: ImageVector = ImageVector.Builder(
        name = "TaskListSquareLtr24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 3f)
            horizontalLineTo(18f)
            curveTo(19.66f, 3f, 21f, 4.34f, 21f, 6f)
            verticalLineTo(18f)
            curveTo(21f, 19.66f, 19.66f, 21f, 18f, 21f)
            horizontalLineTo(6f)
            curveTo(4.34f, 21f, 3f, 19.66f, 3f, 18f)
            verticalLineTo(6f)
            curveTo(3f, 4.34f, 4.34f, 3f, 6f, 3f)
            close()
            moveTo(7.5f, 8.5f)
            lineTo(9f, 10f)
            lineTo(12.5f, 6.5f)
            moveTo(14.5f, 8.5f)
            horizontalLineTo(17.5f)
            moveTo(7.5f, 15f)
            horizontalLineTo(17.5f)
        }
    }.build()

    // Tab 2: TaskListSquareLtr24Filled
    val TaskListSquareLtr24Filled: ImageVector = ImageVector.Builder(
        name = "TaskListSquareLtr24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18f, 3f)
            curveTo(19.66f, 3f, 21f, 4.34f, 21f, 6f)
            verticalLineTo(18f)
            curveTo(21f, 19.66f, 19.66f, 21f, 18f, 21f)
            horizontalLineTo(6f)
            curveTo(4.34f, 21f, 3f, 19.66f, 3f, 18f)
            verticalLineTo(6f)
            curveTo(3f, 4.34f, 4.34f, 3f, 6f, 3f)
            horizontalLineTo(18f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.5f, 8.5f)
            lineTo(9f, 10f)
            lineTo(12.5f, 6.5f)
            moveTo(14.5f, 8.5f)
            horizontalLineTo(17.5f)
            moveTo(7.5f, 15f)
            horizontalLineTo(17.5f)
        }
    }.build()

    // Tab 3: Settings24Regular
    val Settings24Regular: ImageVector = ImageVector.Builder(
        name = "Settings24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            close()
            moveTo(19.4f, 13f)
            lineTo(20.8f, 14.1f)
            lineTo(19.2f, 16.9f)
            lineTo(17.4f, 16.2f)
            curveTo(16.9f, 16.6f, 16.3f, 16.9f, 15.7f, 17.2f)
            lineTo(15.4f, 19f)
            horizontalLineTo(12.2f)
            lineTo(11.9f, 17.2f)
            curveTo(11.3f, 16.9f, 10.7f, 16.6f, 10.2f, 16.2f)
            lineTo(8.4f, 16.9f)
            lineTo(6.8f, 14.1f)
            lineTo(8.2f, 13f)
            curveTo(8.1f, 12.7f, 8.1f, 12.3f, 8.1f, 12f)
            curveTo(8.1f, 11.7f, 8.1f, 11.3f, 8.2f, 11f)
            lineTo(6.8f, 9.9f)
            lineTo(8.4f, 7.1f)
            lineTo(10.2f, 7.8f)
            curveTo(10.7f, 7.4f, 11.3f, 7.1f, 11.9f, 6.8f)
            lineTo(12.2f, 5f)
            horizontalLineTo(15.4f)
            lineTo(15.7f, 6.8f)
            curveTo(16.3f, 7.1f, 16.9f, 7.4f, 17.4f, 7.8f)
            lineTo(19.2f, 7.1f)
            lineTo(20.8f, 9.9f)
            lineTo(19.4f, 11f)
            curveTo(19.5f, 11.3f, 19.5f, 11.7f, 19.5f, 12f)
            curveTo(19.5f, 12.3f, 19.5f, 12.7f, 19.4f, 13f)
            close()
        }
    }.build()

    // Tab 3: Settings24Filled
    val Settings24Filled: ImageVector = ImageVector.Builder(
        name = "Settings24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.4f, 13f)
            lineTo(20.8f, 14.1f)
            lineTo(19.2f, 16.9f)
            lineTo(17.4f, 16.2f)
            curveTo(16.9f, 16.6f, 16.3f, 16.9f, 15.7f, 17.2f)
            lineTo(15.4f, 19f)
            horizontalLineTo(12.2f)
            lineTo(11.9f, 17.2f)
            curveTo(11.3f, 16.9f, 10.7f, 16.6f, 10.2f, 16.2f)
            lineTo(8.4f, 16.9f)
            lineTo(6.8f, 14.1f)
            lineTo(8.2f, 13f)
            curveTo(8.1f, 12.7f, 8.1f, 12.3f, 8.1f, 12f)
            curveTo(8.1f, 11.7f, 8.1f, 11.3f, 8.2f, 11f)
            lineTo(6.8f, 9.9f)
            lineTo(8.4f, 7.1f)
            lineTo(10.2f, 7.8f)
            curveTo(10.7f, 7.4f, 11.3f, 7.1f, 11.9f, 6.8f)
            lineTo(12.2f, 5f)
            horizontalLineTo(15.4f)
            lineTo(15.7f, 6.8f)
            curveTo(16.3f, 7.1f, 16.9f, 7.4f, 17.4f, 7.8f)
            lineTo(19.2f, 7.1f)
            lineTo(20.8f, 9.9f)
            lineTo(19.4f, 11f)
            curveTo(19.5f, 11.3f, 19.5f, 11.7f, 19.5f, 12f)
            curveTo(19.5f, 12.3f, 19.5f, 12.7f, 19.4f, 13f)
            close()
            moveTo(12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            close()
        }
    }.build()

    // Search24Regular
    val Search24Regular: ImageVector = ImageVector.Builder(
        name = "Search24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10.5f, 18f)
            curveTo(14.64f, 18f, 18f, 14.64f, 18f, 10.5f)
            curveTo(18f, 6.36f, 14.64f, 3f, 10.5f, 3f)
            curveTo(6.36f, 3f, 3f, 6.36f, 3f, 10.5f)
            curveTo(3f, 14.64f, 6.36f, 18f, 10.5f, 18f)
            close()
            moveTo(16f, 16f)
            lineTo(21f, 21f)
        }
    }.build()

    // Folder24Filled
    val Folder24Filled: ImageVector = ImageVector.Builder(
        name = "Folder24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 6.25f)
            curveTo(3f, 5.01f, 4.01f, 4f, 5.25f, 4f)
            horizontalLineTo(9.38f)
            curveTo(9.98f, 4f, 10.55f, 4.24f, 10.97f, 4.66f)
            lineTo(12.31f, 6f)
            horizontalLineTo(18.75f)
            curveTo(19.99f, 6f, 21f, 7.01f, 21f, 8.25f)
            verticalLineTo(17.75f)
            curveTo(21f, 18.99f, 19.99f, 20f, 18.75f, 20f)
            horizontalLineTo(5.25f)
            curveTo(4.01f, 20f, 3f, 18.99f, 3f, 17.75f)
            verticalLineTo(6.25f)
            close()
        }
    }.build()

    // Star20Regular
    val Star20Regular: ImageVector = ImageVector.Builder(
        name = "Star20Regular",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10f, 2.5f)
            lineTo(12.3f, 7.2f)
            lineTo(17.5f, 8f)
            lineTo(13.8f, 11.6f)
            lineTo(14.7f, 16.8f)
            lineTo(10f, 14.3f)
            lineTo(5.3f, 16.8f)
            lineTo(6.2f, 11.6f)
            lineTo(2.5f, 8f)
            lineTo(7.7f, 7.2f)
            close()
        }
    }.build()

    // Star20Filled
    val Star20Filled: ImageVector = ImageVector.Builder(
        name = "Star20Filled",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color(0xFFF59E0B))) {
            moveTo(10f, 2.5f)
            lineTo(12.3f, 7.2f)
            lineTo(17.5f, 8f)
            lineTo(13.8f, 11.6f)
            lineTo(14.7f, 16.8f)
            lineTo(10f, 14.3f)
            lineTo(5.3f, 16.8f)
            lineTo(6.2f, 11.6f)
            lineTo(2.5f, 8f)
            lineTo(7.7f, 7.2f)
            close()
        }
    }.build()

    // Clock24Regular
    val Clock24Regular: ImageVector = ImageVector.Builder(
        name = "Clock24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            close()
            moveTo(12f, 7f)
            verticalLineTo(12f)
            lineTo(15.5f, 14f)
        }
    }.build()

    // Edit20Regular
    val Edit20Regular: ImageVector = ImageVector.Builder(
        name = "Edit20Regular",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(14.5f, 2.5f)
            lineTo(17.5f, 5.5f)
            lineTo(6.5f, 16.5f)
            lineTo(2.5f, 17.5f)
            lineTo(3.5f, 13.5f)
            close()
        }
    }.build()

    // Library Spine glyph (3 spines: |||)
    val LibrarySpines24Regular: ImageVector = ImageVector.Builder(
        name = "LibrarySpines24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 4f)
            verticalLineTo(20f)
            moveTo(12f, 4f)
            verticalLineTo(20f)
            moveTo(18f, 4f)
            verticalLineTo(20f)
        }
    }.build()

    // ArrowLeft24Regular
    val ArrowLeft24Regular: ImageVector = ImageVector.Builder(
        name = "ArrowLeft24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f)
            horizontalLineTo(5f)
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
    }.build()

    // Pen24Filled
    val Pen24Filled: ImageVector = ImageVector.Builder(
        name = "Pen24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.07f, 4.93f)
            curveTo(19.78f, 5.64f, 19.78f, 6.79f, 19.07f, 7.5f)
            lineTo(8.5f, 18.07f)
            lineTo(4f, 20f)
            lineTo(5.93f, 15.5f)
            lineTo(16.5f, 4.93f)
            curveTo(17.21f, 4.22f, 18.36f, 4.22f, 19.07f, 4.93f)
            close()
        }
    }.build()

    // Highlighter24Filled
    val Highlighter24Filled: ImageVector = ImageVector.Builder(
        name = "Highlighter24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.5f, 3.5f)
            lineTo(20.5f, 8.5f)
            lineTo(11.5f, 17.5f)
            lineTo(6.5f, 12.5f)
            close()
            moveTo(5.5f, 14.5f)
            lineTo(9.5f, 18.5f)
            lineTo(4.5f, 21.5f)
            lineTo(2.5f, 19.5f)
            close()
        }
    }.build()

    // Note24Filled
    val Note24Filled: ImageVector = ImageVector.Builder(
        name = "Note24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineTo(19f)
            curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
            horizontalLineTo(15f)
            lineTo(21f, 15f)
            verticalLineTo(5f)
            curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
            close()
            moveTo(14f, 19.5f)
            verticalLineTo(16f)
            curveTo(14f, 15.45f, 14.45f, 15f, 15f, 15f)
            horizontalLineTo(18.5f)
            lineTo(14f, 19.5f)
            close()
        }
    }.build()

    // Undo24Filled
    val Undo24Filled: ImageVector = ImageVector.Builder(
        name = "Undo24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 8f)
            lineTo(3f, 12f)
            lineTo(7f, 16f)
            moveTo(3f, 12f)
            horizontalLineTo(15f)
            curveTo(18.31f, 12f, 21f, 14.69f, 21f, 18f)
        }
    }.build()

    // Redo24Filled
    val Redo24Filled: ImageVector = ImageVector.Builder(
        name = "Redo24Filled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(17f, 8f)
            lineTo(21f, 12f)
            lineTo(17f, 16f)
            moveTo(21f, 12f)
            horizontalLineTo(9f)
            curveTo(5.69f, 12f, 3f, 14.69f, 3f, 18f)
        }
    }.build()

    // Checkmark24Regular (Office exit/commit icon)
    val Checkmark24Regular: ImageVector = ImageVector.Builder(
        name = "Checkmark24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 6.5f)
        }
    }.build()

    // Text24Regular (Insert Text Tool T)
    val Text24Regular: ImageVector = ImageVector.Builder(
        name = "Text24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 6f)
            horizontalLineTo(19f)
            moveTo(12f, 6f)
            verticalLineTo(19f)
            moveTo(9.5f, 19f)
            horizontalLineTo(14.5f)
        }
    }.build()

    // Eraser24Regular
    val Eraser24Regular: ImageVector = ImageVector.Builder(
        name = "Eraser24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16.5f, 3.5f)
            lineTo(20.5f, 7.5f)
            lineTo(11.5f, 16.5f)
            lineTo(7.5f, 12.5f)
            close()
            moveTo(7.5f, 12.5f)
            lineTo(4f, 16f)
            curveTo(3.5f, 16.5f, 3.5f, 17.5f, 4f, 18f)
            lineTo(6f, 20f)
            curveTo(6.5f, 20.5f, 7.5f, 20.5f, 8f, 20f)
            lineTo(11.5f, 16.5f)
            moveTo(9f, 20f)
            horizontalLineTo(21f)
        }
    }.build()

    // Bookmark24Regular
    val Bookmark24Regular: ImageVector = ImageVector.Builder(
        name = "Bookmark24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 4f)
            curveTo(6f, 2.9f, 6.9f, 2f, 8f, 2f)
            horizontalLineTo(16f)
            curveTo(17.1f, 2f, 18f, 2.9f, 18f, 4f)
            verticalLineTo(22f)
            lineTo(12f, 18f)
            lineTo(6f, 22f)
            close()
        }
    }.build()

    // Copy24Regular
    val Copy24Regular: ImageVector = ImageVector.Builder(
        name = "Copy24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 7f)
            curveTo(8f, 5.9f, 8.9f, 5f, 10f, 5f)
            horizontalLineTo(19f)
            curveTo(20.1f, 5f, 21f, 5.9f, 21f, 7f)
            verticalLineTo(18f)
            curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
            horizontalLineTo(10f)
            curveTo(8.9f, 20f, 8f, 19.1f, 8f, 18f)
            close()
            moveTo(5f, 8f)
            verticalLineTo(19f)
            curveTo(5f, 20.66f, 6.34f, 22f, 8f, 22f)
            horizontalLineTo(17f)
        }
    }.build()

    // Grid24Regular (Pages View 3-column)
    val Grid24Regular: ImageVector = ImageVector.Builder(
        name = "Grid24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 4f)
            horizontalLineTo(9.5f)
            verticalLineTo(9.5f)
            horizontalLineTo(4f)
            close()
            moveTo(14.5f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(9.5f)
            horizontalLineTo(14.5f)
            close()
            moveTo(4f, 14.5f)
            horizontalLineTo(9.5f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(14.5f, 14.5f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(14.5f)
            close()
        }
    }.build()

    // Shapes24Regular
    val Shapes24Regular: ImageVector = ImageVector.Builder(
        name = "Shapes24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Circle top right
            moveTo(17f, 10f)
            curveTo(19.2f, 10f, 21f, 8.2f, 21f, 6f)
            curveTo(21f, 3.8f, 19.2f, 2f, 17f, 2f)
            curveTo(14.8f, 2f, 13f, 3.8f, 13f, 6f)
            curveTo(13f, 8.2f, 14.8f, 10f, 17f, 10f)
            close()
            // Triangle bottom left
            moveTo(8.5f, 6.5f)
            lineTo(3.5f, 15f)
            lineTo(13.5f, 15f)
            close()
            // Square bottom right
            moveTo(13f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(13f)
            close()
        }
    }.build()

    // Open24Regular (Open with External App)
    val Open24Regular: ImageVector = ImageVector.Builder(
        name = "Open24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 4f)
            horizontalLineTo(6f)
            curveTo(4.9f, 4f, 4f, 4.9f, 4f, 6f)
            verticalLineTo(18f)
            curveTo(4f, 19.1f, 4.9f, 20f, 6f, 20f)
            horizontalLineTo(18f)
            curveTo(19.1f, 20f, 20f, 19.1f, 20f, 18f)
            verticalLineTo(13f)
            moveTo(13f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            moveTo(20f, 4f)
            lineTo(10f, 14f)
        }
    }.build()

    // Share24Regular
    val Share24Regular: ImageVector = ImageVector.Builder(
        name = "Share24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 3f)
            verticalLineTo(15f)
            moveTo(12f, 3f)
            lineTo(8f, 7f)
            moveTo(12f, 3f)
            lineTo(16f, 7f)
            moveTo(5f, 12f)
            verticalLineTo(19f)
            curveTo(5f, 20.1f, 5.9f, 21f, 7f, 21f)
            horizontalLineTo(17f)
            curveTo(18.1f, 21f, 19f, 20.1f, 19f, 19f)
            verticalLineTo(12f)
        }
    }.build()

    // Print24Regular
    val Print24Regular: ImageVector = ImageVector.Builder(
        name = "Print24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 7f)
            verticalLineTo(3f)
            horizontalLineTo(17f)
            verticalLineTo(7f)
            moveTo(5f, 7f)
            horizontalLineTo(19f)
            curveTo(20.1f, 7f, 21f, 7.9f, 21f, 9f)
            verticalLineTo(16f)
            curveTo(21f, 17.1f, 20.1f, 18f, 19f, 18f)
            horizontalLineTo(17f)
            verticalLineTo(21f)
            horizontalLineTo(7f)
            verticalLineTo(18f)
            horizontalLineTo(5f)
            curveTo(3.9f, 18f, 3f, 17.1f, 3f, 16f)
            verticalLineTo(9f)
            curveTo(3f, 7.9f, 3.9f, 7f, 5f, 7f)
            close()
            moveTo(17f, 11f)
            horizontalLineTo(17.01f)
        }
    }.build()

    // Delete24Regular (Trash icon for removing shapes and notes)
    val Delete24Regular: ImageVector = ImageVector.Builder(
        name = "Delete24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            moveTo(10f, 10f)
            verticalLineTo(17f)
            moveTo(14f, 10f)
            verticalLineTo(17f)
            moveTo(5f, 6f)
            lineTo(6.5f, 20f)
            curveTo(6.6f, 20.6f, 7.1f, 21f, 7.7f, 21f)
            horizontalLineTo(16.3f)
            curveTo(16.9f, 21f, 17.4f, 20.6f, 17.5f, 20f)
            lineTo(19f, 6f)
            moveTo(9f, 6f)
            verticalLineTo(4f)
            curveTo(9f, 3.4f, 9.4f, 3f, 10f, 3f)
            horizontalLineTo(14f)
            curveTo(14.6f, 3f, 15f, 3.4f, 15f, 4f)
            verticalLineTo(6f)
        }
    }.build()

    // Pin24Regular (Sticky note pin)
    val Pin24Regular: ImageVector = ImageVector.Builder(
        name = "Pin24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16f, 12f)
            verticalLineTo(4f)
            horizontalLineTo(8f)
            verticalLineTo(12f)
            lineTo(5f, 15f)
            verticalLineTo(17f)
            horizontalLineTo(11f)
            verticalLineTo(22f)
            lineTo(12f, 22f)
            lineTo(13f, 22f)
            verticalLineTo(17f)
            horizontalLineTo(19f)
            verticalLineTo(15f)
            close()
        }
    }.build()

    // Square24Regular
    val Square24Regular: ImageVector = ImageVector.Builder(
        name = "Square24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(5f)
            close()
        }
    }.build()

    // Circle24Regular
    val Circle24Regular: ImageVector = ImageVector.Builder(
        name = "Circle24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 3f)
            curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
            curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
            curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
            close()
        }
    }.build()

    // ArrowRight24Regular
    val ArrowRight24Regular: ImageVector = ImageVector.Builder(
        name = "ArrowRight24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 12f)
            horizontalLineTo(20f)
            moveTo(14f, 6f)
            lineTo(20f, 12f)
            lineTo(14f, 18f)
        }
    }.build()

    // Line24Regular
    val Line24Regular: ImageVector = ImageVector.Builder(
        name = "Line24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 20f)
            lineTo(20f, 4f)
        }
    }.build()

    // Info24Regular (Document Info)
    val Info24Regular: ImageVector = ImageVector.Builder(
        name = "Info24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            close()
            moveTo(12f, 8f)
            horizontalLineTo(12.01f)
            moveTo(11f, 12f)
            horizontalLineTo(12f)
            verticalLineTo(16f)
            horizontalLineTo(13f)
        }
    }.build()

    // Mail24Regular (Feedback mail)
    val Mail24Regular: ImageVector = ImageVector.Builder(
        name = "Mail24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
            verticalLineTo(16f)
            curveTo(22f, 17.1f, 21.1f, 18f, 20f, 18f)
            horizontalLineTo(4f)
            curveTo(2.9f, 18f, 2f, 17.1f, 2f, 16f)
            verticalLineTo(8f)
            curveTo(2f, 6.9f, 2.9f, 6f, 4f, 6f)
            close()
            moveTo(3f, 8f)
            lineTo(12f, 13.5f)
            lineTo(21f, 8f)
        }
    }.build()

    // Toc24Regular (Table of Contents / Outline 📑)
    val Toc24Regular: ImageVector = ImageVector.Builder(
        name = "Toc24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 6f)
            horizontalLineTo(7f)
            moveTo(10f, 6f)
            horizontalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(7f)
            moveTo(10f, 12f)
            horizontalLineTo(19f)
            moveTo(5f, 18f)
            horizontalLineTo(7f)
            moveTo(10f, 18f)
            horizontalLineTo(19f)
        }
    }.build()

    val DocumentBulletList24Regular: ImageVector = Toc24Regular

    // MoreVertical24Regular (More Options ⋮)
    val MoreVertical24Regular: ImageVector = ImageVector.Builder(
        name = "MoreVertical24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // Top dot
            moveTo(12f, 6.5f)
            curveTo(12.83f, 6.5f, 13.5f, 5.83f, 13.5f, 5f)
            curveTo(13.5f, 4.17f, 12.83f, 3.5f, 12f, 3.5f)
            curveTo(11.17f, 3.5f, 10.5f, 4.17f, 10.5f, 5f)
            curveTo(10.5f, 5.83f, 11.17f, 6.5f, 12f, 6.5f)
            close()
            // Middle dot
            moveTo(12f, 13.5f)
            curveTo(12.83f, 13.5f, 13.5f, 12.83f, 13.5f, 12f)
            curveTo(13.5f, 11.17f, 12.83f, 10.5f, 12f, 10.5f)
            curveTo(11.17f, 10.5f, 10.5f, 11.17f, 10.5f, 12f)
            curveTo(10.5f, 12.83f, 11.17f, 13.5f, 12f, 13.5f)
            close()
            // Bottom dot
            moveTo(12f, 20.5f)
            curveTo(12.83f, 20.5f, 13.5f, 19.83f, 13.5f, 19f)
            curveTo(13.5f, 18.17f, 12.83f, 17.5f, 12f, 17.5f)
            curveTo(11.17f, 17.5f, 10.5f, 18.17f, 10.5f, 19f)
            curveTo(10.5f, 19.83f, 11.17f, 20.5f, 12f, 20.5f)
            close()
        }
    }.build()

    // ChevronDown16Regular (and 24)
    val ChevronDown16Regular: ImageVector = ImageVector.Builder(
        name = "ChevronDown16Regular",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 6.5f)
            lineTo(8f, 10.5f)
            lineTo(12f, 6.5f)
        }
    }.build()

    // ChevronRight16Regular
    val ChevronRight16Regular: ImageVector = ImageVector.Builder(
        name = "ChevronRight16Regular",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6.5f, 4f)
            lineTo(10.5f, 8f)
            lineTo(6.5f, 12f)
        }
    }.build()

    // ChevronLeft24Regular
    val ChevronLeft24Regular: ImageVector = ImageVector.Builder(
        name = "ChevronLeft24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15f, 5f)
            lineTo(8f, 12f)
            lineTo(15f, 19f)
        }
    }.build()

    // ChevronRight24Regular
    val ChevronRight24Regular: ImageVector = ImageVector.Builder(
        name = "ChevronRight24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }
    }.build()

    val ChevronDown24Regular: ImageVector = ImageVector.Builder(
        name = "ChevronDown24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 9.5f)
            lineTo(12f, 15.5f)
            lineTo(18f, 9.5f)
        }
    }.build()

    // FolderAdd24Regular
    val FolderAdd24Regular: ImageVector = ImageVector.Builder(
        name = "FolderAdd24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Folder outline
            moveTo(3f, 6.5f)
            curveTo(3f, 5.67f, 3.67f, 5f, 4.5f, 5f)
            horizontalLineTo(9f)
            lineTo(11f, 7f)
            horizontalLineTo(19.5f)
            curveTo(20.33f, 7f, 21f, 7.67f, 21f, 8.5f)
            verticalLineTo(13f)
            moveTo(3f, 6.5f)
            verticalLineTo(18.5f)
            curveTo(3f, 19.33f, 3.67f, 20f, 4.5f, 20f)
            horizontalLineTo(13f)
            moveTo(3f, 6.5f)
            horizontalLineTo(11f)
            // Plus sign at bottom right
            moveTo(18f, 15f)
            verticalLineTo(21f)
            moveTo(15f, 18f)
            horizontalLineTo(21f)
        }
    }.build()

    // BookOpen24Regular (Single-Page Mode)
    val BookOpen24Regular: ImageVector = ImageVector.Builder(
        name = "BookOpen24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 6.5f)
            curveTo(10f, 5.5f, 6.5f, 5f, 3f, 6f)
            verticalLineTo(19f)
            curveTo(6.5f, 18f, 10f, 18.5f, 12f, 19.5f)
            moveTo(12f, 6.5f)
            curveTo(14f, 5.5f, 17.5f, 5f, 21f, 6f)
            verticalLineTo(19f)
            curveTo(17.5f, 18f, 14f, 18.5f, 12f, 19.5f)
            moveTo(12f, 6.5f)
            verticalLineTo(19.5f)
        }
    }.build()

    // ContinuousScroll24Regular (Continuous Scroll Mode)
    val ContinuousScroll24Regular: ImageVector = ImageVector.Builder(
        name = "ContinuousScroll24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 3.5f)
            horizontalLineTo(18f)
            verticalLineTo(20.5f)
            horizontalLineTo(6f)
            close()
            moveTo(9f, 8f)
            horizontalLineTo(15f)
            moveTo(9f, 12f)
            horizontalLineTo(15f)
            moveTo(9f, 16f)
            horizontalLineTo(13f)
            moveTo(12f, 1.5f)
            lineTo(12f, 3.5f)
            moveTo(12f, 20.5f)
            lineTo(12f, 22.5f)
        }
    }.build()

    // ChevronUp24Regular
    val ChevronUp24Regular: ImageVector = ImageVector.Builder(
        name = "ChevronUp24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 14.5f)
            lineTo(12f, 8.5f)
            lineTo(18f, 14.5f)
        }
    }.build()

    // Dismiss24Regular (Close / Clear)
    val Dismiss24Regular: ImageVector = ImageVector.Builder(
        name = "Dismiss24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }.build()

    // ArrowSync24Regular (Check for Updates / Refresh)
    val ArrowSync24Regular: ImageVector = ImageVector.Builder(
        name = "ArrowSync24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 4f)
            lineTo(20f, 9f)
            lineTo(15f, 9f)
            moveTo(4f, 20f)
            lineTo(4f, 15f)
            lineTo(9f, 15f)
            moveTo(20f, 9f)
            lineTo(17f, 6f)
            lineTo(12f, 5f)
            lineTo(7f, 7f)
            lineTo(4f, 12f)
            moveTo(4f, 15f)
            lineTo(7f, 18f)
            lineTo(12f, 19f)
            lineTo(17f, 17f)
            lineTo(20f, 12f)
        }
    }.build()

    // ArrowDownload24Regular (Download / Install Update)
    val ArrowDownload24Regular: ImageVector = ImageVector.Builder(
        name = "ArrowDownload24Regular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            lineTo(12f, 16f)
            moveTo(7f, 11f)
            lineTo(12f, 16f)
            lineTo(17f, 11f)
            moveTo(5f, 20f)
            lineTo(19f, 20f)
        }
    }.build()
}



