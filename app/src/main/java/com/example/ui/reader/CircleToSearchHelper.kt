package com.example.ui.reader

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object CircleToSearchHelper {

    /**
     * Activates Google Circle to Search by:
     * 1. Attempting to launch the native Omnient Activity overlay (Circle to Search internal activity).
     * 2. Automatically falling back to a system ACTION_ASSIST intent with a screenshot payload
     *    for full compatibility on unrooted third-party devices.
     * 3. Additional fallbacks to Google Lens and universal visual search chooser.
     */
    suspend fun activateCircleToSearch(
        context: Context,
        pageBitmap: Bitmap?,
        pageIndex: Int
    ) {
        val pm = context.packageManager

        // Save screenshot payload to cache for FileProvider URI sharing
        val screenshotUri: Uri? = withContext(Dispatchers.IO) {
            if (pageBitmap != null) {
                try {
                    val snapshotsDir = File(context.cacheDir, "search_snapshots")
                    if (!snapshotsDir.exists()) snapshotsDir.mkdirs()
                    val snapshotFile = File(snapshotsDir, "search_page_${pageIndex + 1}.png")
                    FileOutputStream(snapshotFile).use { out ->
                        pageBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        snapshotFile
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else null
        }

        var launched = false

        // 1. Check for native Google Omnient Activity overlay (Circle to Search)
        val omnientCandidateIntents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.apps.search.omnient.OmnientActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.apps.search.omnient.entrypoint.OmnientLauncherActivity"
                )
            },
            Intent("com.google.android.googlequicksearchbox.action.OMNIENT").apply {
                setPackage("com.google.android.googlequicksearchbox")
            }
        )

        for (candidate in omnientCandidateIntents) {
            if (candidate.resolveActivity(pm) != null) {
                try {
                    if (screenshotUri != null) {
                        candidate.putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        candidate.setDataAndType(screenshotUri, "image/png")
                        candidate.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(candidate)
                    launched = true
                    break
                } catch (e: Exception) {
                    // Omnient activity might throw SecurityException or require system permissions
                    e.printStackTrace()
                }
            }
        }

        // 2. Fallback: System ACTION_ASSIST intent with screenshot payload
        if (!launched) {
            try {
                val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (screenshotUri != null) {
                        putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val assistBundle = Bundle().apply {
                        if (screenshotUri != null) {
                            putParcelable("screenshot_uri", screenshotUri)
                        }
                        if (pageBitmap != null) {
                            // Downscale bitmap if needed for IPC bundle limit
                            val scaledBmp = if (pageBitmap.byteCount > 1024 * 1024) {
                                Bitmap.createScaledBitmap(
                                    pageBitmap,
                                    (pageBitmap.width / 2).coerceAtLeast(1),
                                    (pageBitmap.height / 2).coerceAtLeast(1),
                                    true
                                )
                            } else {
                                pageBitmap
                            }
                            putParcelable("screenshot", scaledBmp)
                        }
                    }
                    putExtra("assist_context", assistBundle)
                }

                if (assistIntent.resolveActivity(pm) != null) {
                    context.startActivity(assistIntent)
                    launched = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Fallback: Google Lens Visual Search
        if (!launched && screenshotUri != null) {
            try {
                val lensIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, screenshotUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("com.google.ar.lens")
                }
                if (lensIntent.resolveActivity(pm) != null) {
                    context.startActivity(lensIntent)
                    launched = true
                } else {
                    val gAppLensIntent = Intent().apply {
                        component = ComponentName(
                            "com.google.android.googlequicksearchbox",
                            "com.google.android.apps.lens.MainActivity"
                        )
                        putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        setDataAndType(screenshotUri, "image/png")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (gAppLensIntent.resolveActivity(pm) != null) {
                        context.startActivity(gAppLensIntent)
                        launched = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Universal Fallback: System Share Chooser for Image Search / Web Search
        if (!launched) {
            try {
                if (screenshotUri != null) {
                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Circle to Search - Page ${pageIndex + 1}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(chooserIntent, "Search Screen Content"))
                } else {
                    val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Circle to Search activated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
