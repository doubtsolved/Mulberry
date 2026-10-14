package com.example.ui.screens.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalMulberryColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun SnipOverlayView(
    pageBitmap: Bitmap?,
    pageNumber: Int,
    onSnipConfirmed: (croppedBitmap: Bitmap, title: String, tags: String) -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalMulberryColors.current

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var viewSize by remember { mutableStateOf(Size.Zero) }

    var showMetadataDialog by remember { mutableStateOf(false) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Dialog for naming the snip
    if (showMetadataDialog && pendingCropBitmap != null) {
        var snipTitle by remember { mutableStateOf("Page $pageNumber Diagram") }
        var snipTags by remember { mutableStateOf("high yield") }

        Dialog(onDismissRequest = { showMetadataDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Save Snip to Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Page $pageNumber Crop",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Diagram Label", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = snipTitle,
                        onValueChange = { snipTitle = it },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tags", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = snipTags,
                        onValueChange = { snipTags = it },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMetadataDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = colors.textSecondary)
                        }

                        Button(
                            onClick = {
                                pendingCropBitmap?.let { bmp ->
                                    onSnipConfirmed(bmp, snipTitle, snipTags)
                                }
                                showMetadataDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Transparent interaction overlay for drag-selection
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                            dragEnd = offset
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragEnd = (dragEnd ?: Offset.Zero) + dragAmount
                        }
                    )
                }
        ) {
            viewSize = size

            val start = dragStart
            val end = dragEnd

            if (start != null && end != null) {
                val left = min(start.x, end.x).coerceIn(0f, size.width)
                val top = min(start.y, end.y).coerceIn(0f, size.height)
                val right = max(start.x, end.x).coerceIn(0f, size.width)
                val bottom = max(start.y, end.y).coerceIn(0f, size.height)

                val rect = Rect(left, top, right, bottom)

                // Dim unselected area
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f)
                )

                // Highlight selected bounding box
                drawRect(
                    color = Color.Transparent,
                    topLeft = rect.topLeft,
                    size = rect.size
                )

                // Border around crop region
                drawRect(
                    color = Color(0xFF14B8A6),
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 3.dp.toPx())
                )
            } else {
                drawRect(color = Color.Black.copy(alpha = 0.4f))
            }
        }

        // Top Banner Hint
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surface.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        ) {
            Text(
                text = "Drag a bounding box over diagram to crop",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // Bottom Controls (Cancel & Confirm Crop)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.surface.copy(alpha = 0.95f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = colors.textSecondary)
            }

            val hasSelection = dragStart != null && dragEnd != null &&
                    kotlin.math.abs((dragStart?.x ?: 0f) - (dragEnd?.x ?: 0f)) > 20f

            Button(
                onClick = {
                    val bmp = pageBitmap ?: return@Button
                    val start = dragStart ?: return@Button
                    val end = dragEnd ?: return@Button
                    if (viewSize.width <= 0 || viewSize.height <= 0) return@Button

                    val scaleX = bmp.width / viewSize.width
                    val scaleY = bmp.height / viewSize.height

                    val left = (min(start.x, end.x) * scaleX).toInt().coerceIn(0, bmp.width - 1)
                    val top = (min(start.y, end.y) * scaleY).toInt().coerceIn(0, bmp.height - 1)
                    val right = (max(start.x, end.x) * scaleX).toInt().coerceIn(left + 1, bmp.width)
                    val bottom = (max(start.y, end.y) * scaleY).toInt().coerceIn(top + 1, bmp.height)

                    val cropWidth = (right - left).coerceAtLeast(1)
                    val cropHeight = (bottom - top).coerceAtLeast(1)

                    try {
                        val cropped = Bitmap.createBitmap(bmp, left, top, cropWidth, cropHeight)
                        pendingCropBitmap = cropped
                        showMetadataDialog = true
                    } catch (_: Exception) {}
                },
                enabled = hasSelection,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Crop Selected Area", color = colors.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
