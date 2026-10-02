package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PdfAnnotationManager {

    /**
     * Bakes vector annotations, text, shapes, and sticky notes permanently into a PDF file
     * using android.graphics.pdf.PdfDocument.
     */
    suspend fun bakeAnnotationsToPdf(
        context: Context,
        sourceUriOrPath: String,
        destinationFile: File,
        annotations: List<AnnotationEntity>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = if (sourceUriOrPath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(sourceUriOrPath), "r")
            } else {
                val f = File(sourceUriOrPath)
                if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY) else null
            }

            if (pfd == null) return@withContext false
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount <= 0) return@withContext false

            for (pageIdx in 0 until pageCount) {
                val page = renderer.openPage(pageIdx)
                val pageWidth = page.width
                val pageHeight = page.height

                // Render page background at 1.5x resolution for sharp, crisp document text
                val targetBmpWidth = (pageWidth * 1.5f).toInt().coerceIn(720, 2400)
                val targetBmpHeight = (pageHeight * 1.5f).toInt().coerceIn(960, 3200)
                val pageBitmap = Bitmap.createBitmap(targetBmpWidth, targetBmpHeight, Bitmap.Config.ARGB_8888)
                val bgCanvas = android.graphics.Canvas(pageBitmap)
                bgCanvas.drawColor(Color.WHITE)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Create PDF Document page matching original PDF dimensions in points
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas

                // Draw rendered original PDF page
                val destRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                canvas.drawBitmap(pageBitmap, null, destRect, null)
                pageBitmap.recycle()

                // Filter annotations for this page (1-indexed)
                val pageAnnotations = annotations.filter { it.pageIndex == (pageIdx + 1) }

                for (ann in pageAnnotations) {
                    try {
                        when (ann.type) {
                            "NOTE" -> {
                                val obj = JSONObject(ann.strokePointsJson)
                                val noteText = obj.optString("text", "Note")
                                val rawX = obj.optDouble("x", 0.1).toFloat()
                                val rawY = obj.optDouble("y", 0.1).toFloat()
                                // Handle normalized or legacy absolute coordinates
                                val nx = if (rawX > 1f) (rawX / 400f).coerceIn(0f, 0.8f) else rawX
                                val ny = if (rawY > 1f) (rawY / 600f).coerceIn(0f, 0.85f) else rawY

                                val px = nx * pageWidth
                                val py = ny * pageHeight
                                val noteWidth = (pageWidth * 0.35f).coerceIn(120f, 220f)

                                // Draw Sticky Note Card
                                val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = Color.parseColor("#FFFBEB") // Light amber
                                    style = Paint.Style.FILL
                                }
                                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = Color.parseColor("#F59E0B") // Amber border
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = Color.parseColor("#78350F") // Dark amber text
                                    textSize = 10f * (pageWidth / 400f).coerceIn(0.8f, 1.3f)
                                }

                                val cardRect = RectF(px, py, px + noteWidth, py + 55f)
                                canvas.drawRoundRect(cardRect, 6f, 6f, notePaint)
                                canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

                                canvas.save()
                                canvas.translate(px + 8f, py + 8f)
                                val sl = StaticLayout.Builder.obtain(noteText, 0, noteText.length, textPaint, (noteWidth - 16f).toInt())
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .setMaxLines(3)
                                    .build()
                                sl.draw(canvas)
                                canvas.restore()
                            }
                            "TEXT" -> {
                                val obj = JSONObject(ann.strokePointsJson)
                                val text = obj.optString("text", "")
                                val rawX = obj.optDouble("x", 0.1).toFloat()
                                val rawY = obj.optDouble("y", 0.1).toFloat()
                                val nx = if (rawX > 1f) (rawX / 400f).coerceIn(0f, 0.95f) else rawX
                                val ny = if (rawY > 1f) (rawY / 600f).coerceIn(0f, 0.95f) else rawY

                                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = try { Color.parseColor(ann.colorHex) } catch (_: Exception) { Color.BLACK }
                                    textSize = (ann.strokeWidth * 6f).coerceIn(12f, 24f)
                                }
                                canvas.drawText(text, nx * pageWidth, ny * pageHeight, textPaint)
                            }
                            "SHAPE" -> {
                                val obj = JSONObject(ann.strokePointsJson)
                                val shape = obj.optString("shape", "RECTANGLE")
                                val rsx = obj.optDouble("startX", 0.1).toFloat()
                                val rsy = obj.optDouble("startY", 0.1).toFloat()
                                val rex = obj.optDouble("endX", 0.4).toFloat()
                                val rey = obj.optDouble("endY", 0.3).toFloat()

                                val sx = (if (rsx > 1f) rsx / 400f else rsx) * pageWidth
                                val sy = (if (rsy > 1f) rsy / 600f else rsy) * pageHeight
                                val ex = (if (rex > 1f) rex / 400f else rex) * pageWidth
                                val ey = (if (rey > 1f) rey / 600f else rey) * pageHeight

                                val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = try { Color.parseColor(ann.colorHex) } catch (_: Exception) { Color.parseColor("#0284C7") }
                                    style = Paint.Style.STROKE
                                    strokeWidth = ann.strokeWidth * (pageWidth / 400f).coerceIn(1f, 2.5f)
                                }

                                when (shape) {
                                    "CIRCLE" -> {
                                        val left = minOf(sx, ex)
                                        val top = minOf(sy, ey)
                                        val right = maxOf(sx, ex)
                                        val bottom = maxOf(sy, ey)
                                        canvas.drawOval(RectF(left, top, right, bottom), shapePaint)
                                    }
                                    "ARROW" -> {
                                        canvas.drawLine(sx, sy, ex, ey, shapePaint)
                                        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                                        val arrowLen = 14f * (pageWidth / 400f).coerceIn(1f, 2f)
                                        val x1 = ex - arrowLen * cos(angle - Math.PI / 6).toFloat()
                                        val y1 = ey - arrowLen * sin(angle - Math.PI / 6).toFloat()
                                        val x2 = ex - arrowLen * cos(angle + Math.PI / 6).toFloat()
                                        val y2 = ey - arrowLen * sin(angle + Math.PI / 6).toFloat()
                                        canvas.drawLine(ex, ey, x1, y1, shapePaint)
                                        canvas.drawLine(ex, ey, x2, y2, shapePaint)
                                    }
                                    "LINE" -> {
                                        canvas.drawLine(sx, sy, ex, ey, shapePaint)
                                    }
                                    else -> {
                                        // RECTANGLE
                                        val left = minOf(sx, ex)
                                        val top = minOf(sy, ey)
                                        val right = maxOf(sx, ex)
                                        val bottom = maxOf(sy, ey)
                                        canvas.drawRect(RectF(left, top, right, bottom), shapePaint)
                                    }
                                }
                            }
                            else -> {
                                // PEN, HIGHLIGHTER, PEN_BLACK, PEN_RED, PEN_BLUE
                                val arr = JSONArray(ann.strokePointsJson)
                                if (arr.length() > 1) {
                                    val isHighlighter = ann.type == "HIGHLIGHTER"
                                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        if (isHighlighter) {
                                            color = Color.parseColor("#FACC15")
                                            alpha = 90
                                            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                                            strokeWidth = ann.strokeWidth * (pageWidth / 400f).coerceIn(1f, 2.5f)
                                        } else {
                                            color = try { Color.parseColor(ann.colorHex) } catch (_: Exception) { Color.parseColor("#0F172A") }
                                            strokeWidth = ann.strokeWidth * (pageWidth / 400f).coerceIn(0.8f, 2f)
                                        }
                                        style = Paint.Style.STROKE
                                        strokeCap = Paint.Cap.ROUND
                                        strokeJoin = Paint.Join.ROUND
                                    }

                                    val path = Path()
                                    val first = arr.getJSONObject(0)
                                    val rfx = first.getDouble("x").toFloat()
                                    val rfy = first.getDouble("y").toFloat()
                                    val fx = (if (rfx > 1f) rfx / 400f else rfx) * pageWidth
                                    val fy = (if (rfy > 1f) rfy / 600f else rfy) * pageHeight
                                    path.moveTo(fx, fy)

                                    for (i in 1 until arr.length()) {
                                        val pt = arr.getJSONObject(i)
                                        val rpx = pt.getDouble("x").toFloat()
                                        val rpy = pt.getDouble("y").toFloat()
                                        val px = (if (rpx > 1f) rpx / 400f else rpx) * pageWidth
                                        val py = (if (rpy > 1f) rpy / 600f else rpy) * pageHeight
                                        path.lineTo(px, py)
                                    }
                                    canvas.drawPath(path, strokePaint)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                pdfDocument.finishPage(docPage)
                onProgress(pageIdx + 1, pageCount)
            }

            // Write document to file
            if (destinationFile.exists()) destinationFile.delete()
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDocument.close()
                renderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * "Save": Bakes annotations into a temporary PDF, then overwrites the original file.
     */
    suspend fun saveDocument(
        context: Context,
        book: BookEntity,
        annotations: List<AnnotationEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "mulberry_save_${System.currentTimeMillis()}.pdf")
        val success = bakeAnnotationsToPdf(
            context = context,
            sourceUriOrPath = book.uriString,
            destinationFile = tempFile,
            annotations = annotations
        )

        if (success && tempFile.exists() && tempFile.length() > 0) {
            try {
                val originalFile = File(book.uriString)
                if (originalFile.exists()) {
                    tempFile.copyTo(originalFile, overwrite = true)
                    tempFile.delete()
                    true
                } else if (book.uriString.startsWith("content://")) {
                    context.contentResolver.openOutputStream(Uri.parse(book.uriString), "wt")?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    tempFile.delete()
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tempFile.delete()
                false
            }
        } else {
            tempFile.delete()
            false
        }
    }

    /**
     * "Save As": Bakes annotations into a new physical file on storage.
     */
    suspend fun saveAsDocument(
        context: Context,
        book: BookEntity,
        newFileName: String,
        annotations: List<AnnotationEntity>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val originalFile = File(book.uriString)
            val parentDir = if (originalFile.exists()) originalFile.parentFile else File(context.filesDir, "MulberryVault")
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs()

            val cleanName = if (newFileName.endsWith(".pdf", ignoreCase = true)) newFileName else "$newFileName.pdf"
            val targetFile = File(parentDir ?: context.filesDir, cleanName)

            val success = bakeAnnotationsToPdf(
                context = context,
                sourceUriOrPath = book.uriString,
                destinationFile = targetFile,
                annotations = annotations
            )

            if (success && targetFile.exists() && targetFile.length() > 0) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
