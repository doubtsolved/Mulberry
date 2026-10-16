package com.example.util

import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SafeFileWriter {
    private const val TAG = "SafeFileWriter"

    fun writeAtomically(targetFile: File, content: String): Boolean {
        val parent = targetFile.parentFile ?: return false
        if (!parent.exists()) {
            val created = parent.mkdirs()
            if (!created && !parent.exists()) {
                Log.d(TAG, "Parent directory does not exist and cannot be created: ${parent.absolutePath}")
                return false
            }
        }
        if (!parent.isDirectory) {
            Log.d(TAG, "Parent path is not a directory: ${parent.absolutePath}")
            return false
        }

        // Clean up legacy .new or .bak left behind by Android's AtomicFile
        try {
            val legacyNew = File(parent, "${targetFile.name}.new")
            if (legacyNew.exists()) legacyNew.delete()
            val legacyBak = File(parent, "${targetFile.name}.bak")
            if (legacyBak.exists()) legacyBak.delete()
        } catch (_: Exception) {}

        val tempFile = File(parent, "${targetFile.name}.${System.currentTimeMillis()}.tmp")
        try {
            // Write content and flush to disk
            FileOutputStream(tempFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Exception) {}
            }

            // Attempt atomic move on API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                    return true
                } catch (_: Exception) {
                    try {
                        Files.move(
                            tempFile.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        return true
                    } catch (_: Exception) {}
                }
            }

            // Fallback for older API or filesystem without atomic move
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (tempFile.renameTo(targetFile)) {
                return true
            }

            // Final fallback: copy directly
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed atomic write to ${targetFile.absolutePath}: ${e.message}")
            if (parent.exists() && parent.isDirectory) {
                try {
                    targetFile.writeText(content, Charsets.UTF_8)
                    return true
                } catch (ex: Exception) {
                    Log.w(TAG, "Fallback direct write failed: ${ex.message}")
                    return false
                }
            }
            return false
        } finally {
            if (tempFile.exists()) {
                try { tempFile.delete() } catch (_: Exception) {}
            }
        }
    }

    fun readSafely(file: File): String? {
        if (file.exists() && file.isFile) {
            return try {
                file.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
        val parent = file.parentFile ?: return null
        val legacyNew = File(parent, "${file.name}.new")
        if (legacyNew.exists() && legacyNew.isFile) {
            try { return legacyNew.readText(Charsets.UTF_8) } catch (_: Exception) {}
        }
        val legacyBak = File(parent, "${file.name}.bak")
        if (legacyBak.exists() && legacyBak.isFile) {
            try { return legacyBak.readText(Charsets.UTF_8) } catch (_: Exception) {}
        }
        return null
    }
}
