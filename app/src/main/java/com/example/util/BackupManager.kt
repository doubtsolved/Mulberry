package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.repository.VaultMetadataManager
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

    suspend fun exportVaultBackup(context: Context, vaultRootPath: String): Uri? = withContext(Dispatchers.IO) {
        try {
            if (vaultRootPath.isBlank()) return@withContext null
            val rootDir = File(vaultRootPath)
            if (!rootDir.exists() || !rootDir.isDirectory) return@withContext null

            // 1. Ensure latest Room state is flushed to .mulberry folder
            val metadataDir = File(rootDir, ".mulberry")
            if (!metadataDir.exists()) {
                metadataDir.mkdirs()
            }
            try {
                VaultMetadataManager.persistVaultMetadata(vaultRootPath, "", context)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Prepare Zip target in cacheDir
            val timestamp = System.currentTimeMillis()
            val zipFileName = "Mulberry_Backup_${timestamp}.zip"
            val zipFile = File(context.cacheDir, zipFileName)
            if (zipFile.exists()) {
                zipFile.delete()
            }

            // 3. Compress .mulberry recursively
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                fun addFileToZip(file: File, parentPath: String) {
                    val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                    if (file.isDirectory) {
                        val dirEntry = ZipEntry(if (entryName.endsWith("/")) entryName else "$entryName/")
                        zos.putNextEntry(dirEntry)
                        zos.closeEntry()
                        file.listFiles()?.forEach { child ->
                            addFileToZip(child, entryName)
                        }
                    } else {
                        val entry = ZipEntry(entryName)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos, bufferSize = 8192)
                        }
                        zos.closeEntry()
                    }
                }

                metadataDir.listFiles()?.forEach { file ->
                    addFileToZip(file, "")
                }
            }

            // 4. Return FileProvider content URI
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

    suspend fun restoreVaultBackup(context: Context, zipUri: Uri, vaultRootPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (vaultRootPath.isBlank()) return@withContext false
            val rootDir = File(vaultRootPath)
            if (!rootDir.exists() || !rootDir.isDirectory) return@withContext false

            val metadataDir = File(rootDir, ".mulberry")
            if (!metadataDir.exists()) {
                metadataDir.mkdirs()
            }

            val inputStream = context.contentResolver.openInputStream(zipUri) ?: return@withContext false

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(metadataDir, entry.name)

                    // ZipSlip prevention check
                    val canonicalDest = destFile.canonicalPath
                    val canonicalRoot = metadataDir.canonicalPath
                    if (!canonicalDest.startsWith(canonicalRoot)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos, bufferSize = 8192)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // 5. Hydrate Room database from restored metadata
            VaultMetadataManager.hydrateDatabaseFromMetadata(vaultRootPath, "", context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
