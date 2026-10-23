package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val BACKUP_FILES = listOf(
        MulberryStorageManager.STUDY_PLANNER_FILE_NAME,
        MulberryStorageManager.READING_HISTORY_FILE_NAME,
        MulberryStorageManager.TRACKING_FILE_NAME
    )

    suspend fun exportVaultBackup(context: Context, vaultRootPath: String = ""): Uri? = withContext(Dispatchers.IO) {
        try {
            val rootDir = if (vaultRootPath.isNotBlank() && File(vaultRootPath).exists()) {
                File(vaultRootPath)
            } else {
                MulberryStorageManager.getMulberryDirectory(context)
            }
            if (!rootDir.exists() || !rootDir.isDirectory) return@withContext null

            // Prepare Zip target in cacheDir
            val timestamp = System.currentTimeMillis()
            val zipFileName = "Mulberry_Backup_${timestamp}.zip"
            val zipFile = File(context.cacheDir, zipFileName)
            if (zipFile.exists()) {
                zipFile.delete()
            }

            // Compress /sdcard/Mulberry/study_planner.json, reading_history.json, and downloaded_books.json
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                BACKUP_FILES.forEach { fileName ->
                    val file = File(rootDir, fileName)
                    if (file.exists() && file.isFile) {
                        val entry = ZipEntry(fileName)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos, bufferSize = 8192)
                        }
                        zos.closeEntry()
                    }
                }
            }

            val authority = try {
                FileProvider.getUriForFile(context, "com.mulberry.fileprovider", zipFile)
                "com.mulberry.fileprovider"
            } catch (_: Exception) {
                "${context.packageName}.provider"
            }

            FileProvider.getUriForFile(context, authority, zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreVaultBackup(context: Context, zipUri: Uri, vaultRootPath: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootDir = if (vaultRootPath.isNotBlank() && File(vaultRootPath).exists()) {
                File(vaultRootPath)
            } else {
                MulberryStorageManager.getMulberryDirectory(context)
            }
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }

            val inputStream = context.contentResolver.openInputStream(zipUri) ?: return@withContext false

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = File(entry.name).name
                    if (entryName in BACKUP_FILES) {
                        val destFile = File(rootDir, entryName)

                        // ZipSlip prevention check
                        val canonicalDest = destFile.canonicalPath
                        val canonicalRoot = rootDir.canonicalPath
                        if (!canonicalDest.startsWith(canonicalRoot)) {
                            entry = zis.nextEntry
                            continue
                        }

                        val tempDest = File(rootDir, "$entryName.restore.tmp")
                        FileOutputStream(tempDest).use { fos ->
                            zis.copyTo(fos, bufferSize = 8192)
                        }
                        tempDest.readText(Charsets.UTF_8).let { content ->
                            SafeFileWriter.writeAtomically(destFile, content)
                        }
                        tempDest.delete()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Reload schedule state
            com.example.data.schedule.ScheduleRepository.getInstance(context).loadSchedule()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
